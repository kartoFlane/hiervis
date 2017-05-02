package pl.pwr.hiervis.measures;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import internal_measures.statistics.AvgWithStdev;
import pl.pwr.hiervis.core.LoadedHierarchy;
import pl.pwr.hiervis.core.MeasureComputeThread;
import pl.pwr.hiervis.util.Event;


/**
 * Class for managing measures, {@link MeasureTask}s, and data/processes related to them.
 * 
 * @author Tomasz Bachmiński
 *
 */
public class MeasureManager
{
	/** Sent when a measure task is posted for processing. */
	public final Event<Pair<LoadedHierarchy, MeasureTask>> taskPosted = new Event<>();
	/** Sent when a measure task computation failed due to an exception. */
	public final Event<Pair<LoadedHierarchy, MeasureTask>> taskFailed = new Event<>();
	/** Sent when a measure computation is started. */
	public final Event<Pair<LoadedHierarchy, String>> measureComputing = new Event<>();
	/** Sent when a measure computation is finished. */
	public final Event<Triple<LoadedHierarchy, String, Object>> measureComputed = new Event<>();

	private MeasureComputeThread computeThread = null;
	private Map<String, Collection<MeasureTask>> measureGroupMap = null;


	public MeasureManager()
	{
		measureGroupMap = new HashMap<>();

		computeThread = new MeasureComputeThread();

		computeThread.taskPosted.addListener( this::onTaskPosted );
		computeThread.taskFailed.addListener( this::onTaskFailed );
		computeThread.measureComputing.addListener( this::onMeasureComputing );
		computeThread.measureComputed.addListener( this::onMeasureComputed );

		computeThread.start();
	}

	/**
	 * Checks whether the measure with the specified name is scheduled for processing, or
	 * currently being processed.
	 * 
	 * @param lh
	 *            the hierarchy to check the measure for
	 * @param measureName
	 *            identifier of the measure
	 * @return true if a measure with the specified identifier is pending calculation, or
	 *         is currently being calculated. False otherwise.
	 */
	public boolean isMeasurePending( LoadedHierarchy lh, String measureName )
	{
		return computeThread.isMeasurePending( lh, measureName );
	}

	/**
	 * Posts a new task for the thread to process.
	 * 
	 * @param lh
	 *            the hierarchy to compute the measure for
	 * @param measure
	 *            the measure to post
	 */
	public void postTask( LoadedHierarchy lh, MeasureTask measure )
	{
		computeThread.postTask( lh, measure );
	}

	/**
	 * Posts a new task for the thread to process.
	 * 
	 * @param task
	 *            the task to post
	 */
	public void postTask( Pair<LoadedHierarchy, MeasureTask> task )
	{
		computeThread.postTask( task.getLeft(), task.getRight() );
	}

	/**
	 * Removes the task from processing queue, if it is not already being processed.
	 * 
	 * @param lh
	 *            the hierarchy to remove the measure for
	 * @param measure
	 *            the measure to remove.
	 * @return true if the task was found and removed, false otherwise.
	 */
	public boolean removeTask( LoadedHierarchy lh, MeasureTask measure )
	{
		return computeThread.removeTask( lh, measure );
	}

	/**
	 * Removes the task from processing queue, if it is not already being processed.
	 * 
	 * @param task
	 *            the task to remove.
	 * @return true if the task was found and removed, false otherwise.
	 */
	public boolean removeTask( Pair<LoadedHierarchy, MeasureTask> task )
	{
		return computeThread.removeTask( task.getLeft(), task.getRight() );
	}

	/**
	 * Clears any pending tasks that have been scheduled for computation, but haven't been started yet.
	 */
	public void clearPendingTasks()
	{
		computeThread.clearPendingTasks();
	}

	/**
	 * Loads {@link MeasureTask}s from script files in the specified directory.
	 * 
	 * @param dirPath
	 *            the directory containing all {@link MeasureTask} script files.
	 * @throws IOException
	 *             if an IO error occurs
	 */
	public void loadMeasureFiles( Path dirPath ) throws IOException
	{
		if ( !Files.isDirectory( dirPath ) )
			throw new IllegalArgumentException( "Argument must point to a directory!" );

		MeasureTaskFactory factory = new JavascriptMeasureTaskFactory( false );

		Files.walk( dirPath ).forEach(
			filePath -> {
				if ( Files.isRegularFile( filePath, LinkOption.NOFOLLOW_LINKS ) ) {
					// MeasureTask task = factory.getMeasureTask( filePath );
					// System.out.println( filePath );

					String groupPath = filePath.getParent().toString()
						.replace( dirPath.toString(), "" )
						.substring( 1 )
						.replace( "\\", "/" );

					Collection<MeasureTask> group = measureGroupMap.get( groupPath );
					if ( group == null ) {
						group = new ArrayList<MeasureTask>();
						measureGroupMap.put( groupPath, group );
					}

					group.add( factory.getMeasureTask( filePath ) );
				}
			}
		);
	}

	/**
	 * @param groupId
	 *            id of the measure group we want to receive
	 * @return returns an unmodifiable collection of all measures belonging to
	 *         the group with the specified id.
	 */
	public Collection<MeasureTask> getMeasureTaskGroup( String groupId )
	{
		if ( !measureGroupMap.containsKey( groupId ) )
			throw new IllegalArgumentException( "No such measure task group: " + groupId );
		return Collections.unmodifiableCollection( measureGroupMap.get( groupId ) );
	}

	/**
	 * @param predicate
	 *            the predicate that {@link MeasureTask}s have to match in order
	 *            to be included in the result.
	 * @return a list of {@link MeasureTask}s that match the specified predicate
	 */
	public Collection<MeasureTask> getMeasureTasks( Predicate<MeasureTask> predicate )
	{
		return measureGroupMap.values().stream()
			.flatMap( Collection::stream )
			.filter( predicate )
			.collect( Collectors.toList() );
	}

	/**
	 * @return a list of all {@link MeasureTask}s the manager is aware of
	 */
	public Collection<MeasureTask> getAllMeasureTasks()
	{
		return getMeasureTasks( t -> true );
	}

	/**
	 * @return a sorted collection of ids of all measure groups that have been loaded.
	 */
	public Collection<String> listMeasureTaskGroups()
	{
		return measureGroupMap.keySet().stream()
			.sorted()
			.collect( Collectors.toList() );
	}

	public void dispose()
	{
		computeThread.shutdown();
	}

	public void dumpMeasures( Path destinationFile, LoadedHierarchy hierarchy )
	{
		final Function<Object, String> resultToCSV = data -> {
			if ( data instanceof Number ) {
				return Objects.toString( data ) + ";0.0;";
			}
			else if ( data instanceof AvgWithStdev ) {
				AvgWithStdev avg = (AvgWithStdev)data;
				return avg.getAvg() + ";" + avg.getStdev() + ";";
			}
			else {
				throw new IllegalArgumentException( "Unexpected data type in measure result: " + data.getClass().getName() );
			}
		};

		final Function<MeasureTask, String> dumpHistogram = task -> {
			StringBuilder buf2 = new StringBuilder();
			Object measureResult = hierarchy.measureHolder.getMeasureResultOrDefault( task.identifier, new double[0] );

			if ( measureResult instanceof double[] == false )
				throw new IllegalArgumentException( "Not a histogram measure: " + task.identifier );

			double[] data = (double[])measureResult;

			if ( data.length > 0 ) {
				buf2.append( task.identifier ).append( '\n' );
				for ( int i = 0; i < data.length; ++i )
					buf2.append( i ).append( ';' );
				buf2.append( '\n' );
				for ( int i = 0; i < data.length; ++i )
					buf2.append( data[i] ).append( ';' );
				buf2.append( '\n' );
				for ( int i = 0; i < data.length; ++i )
					buf2.append( "0.0;" );
				buf2.append( "\n\n" );
			}

			return buf2.toString();
		};

		StringBuilder buf = new StringBuilder();

		// -------------------------------------------------------------------------------------------------------
		// Measures
		buf.append( "Use subtree for internal measures?;" );

		Collection<MeasureTask> measures = getAllMeasureTasks();

		measures.forEach(
			task -> {
				Object measureResult = hierarchy.measureHolder.getMeasureResult( task.identifier );

				if ( measureResult instanceof Number || measureResult instanceof AvgWithStdev )
					buf.append( task.identifier ).append( ";stdev;" );
			}
		);

		buf.append( "\n" );
		// Append data values
		buf.append( hierarchy.options.isUseSubtree ).append( ';' );

		measures.forEach(
			task -> {
				Object measureResult = hierarchy.measureHolder.getMeasureResult( task.identifier );

				if ( measureResult instanceof Number || measureResult instanceof AvgWithStdev )
					buf.append( resultToCSV.apply( measureResult ) );
			}
		);

		buf.append( "\n\n" );

		// -------------------------------------------------------------------------------------------------------
		// Histograms
		measures.forEach(
			task -> {
				Object measureResult = hierarchy.measureHolder.getMeasureResult( task.identifier );

				if ( measureResult instanceof double[] )
					buf.append( dumpHistogram.apply( task ) );
			}
		);

		// -------------------------------------------------------------------------------------------------------
		// String measures
		measures.forEach(
			task -> {
				Object measureResult = hierarchy.measureHolder.getMeasureResult( task.identifier );

				if ( measureResult instanceof String ) {
					buf.append( task.identifier ).append( '\n' )
						.append( measureResult ).append( "\n\n" );
				}
			}
		);

		try ( FileWriter writer = new FileWriter( destinationFile.toFile() ) ) {
			writer.write( buf.toString() );
		}
		catch ( IOException ex ) {
			ex.printStackTrace();
		}
	}

	// -------------------------------------------------------------------------------------

	private void onTaskPosted( Pair<LoadedHierarchy, MeasureTask> task )
	{
		taskPosted.broadcast( task );
	}

	private void onTaskFailed( Pair<LoadedHierarchy, MeasureTask> task )
	{
		taskFailed.broadcast( task );
	}

	private void onMeasureComputing( Pair<LoadedHierarchy, String> task )
	{
		measureComputing.broadcast( task );
	}

	private void onMeasureComputed( Triple<LoadedHierarchy, String, Object> result )
	{
		result.getLeft().measureHolder.putMeasureResult( result.getMiddle(), result.getRight() );

		measureComputed.broadcast( result );
	}
}
