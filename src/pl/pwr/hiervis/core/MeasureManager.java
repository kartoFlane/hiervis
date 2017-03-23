package pl.pwr.hiervis.core;

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
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import basic_hierarchy.interfaces.Hierarchy;
import internal_measures.statistics.AvgWithStdev;
import pl.pwr.hiervis.measures.JavascriptMeasureTaskFactory;
import pl.pwr.hiervis.measures.MeasureTask;
import pl.pwr.hiervis.measures.MeasureTaskFactory;
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
	public final Event<MeasureTask> taskPosted = new Event<>();
	/** Sent when a measure task computation failed due to an exception. */
	public final Event<MeasureTask> taskFailed = new Event<>();
	/** Sent when a measure computation is started. */
	public final Event<String> measureComputing = new Event<>();
	/** Sent when a measure computation is finished. */
	public final Event<Pair<String, Object>> measureComputed = new Event<>();

	private MeasureComputeThread computeThread = null;
	private Map<String, Object> computedMeasureMap = null;
	private Map<String, Collection<MeasureTask>> measureGroupMap = null;


	public MeasureManager( HVContext context )
	{
		computedMeasureMap = new HashMap<>();
		measureGroupMap = new HashMap<>();

		computeThread = new MeasureComputeThread();

		computeThread.taskPosted.addListener( this::onTaskPosted );
		computeThread.taskFailed.addListener( this::onTaskFailed );
		computeThread.measureComputing.addListener( this::onMeasureComputing );
		computeThread.measureComputed.addListener( this::onMeasureComputed );

		computeThread.start();

		context.hierarchyChanged.addListener( this::onHierarchyChanged );
	}

	/**
	 * Sets the hierarchy this thread will compute measures for.
	 * This method may only be called while there are no tasks scheduled for computation.
	 * 
	 * @param hierarchy
	 *            the hierarchy for which measures will be computed.
	 */
	public void setHierarchy( Hierarchy hierarchy )
	{
		computeThread.setHierarchy( hierarchy );
	}

	/**
	 * Checks whether the measure with the specified name is scheduled for processing, or
	 * currently being processed.
	 * 
	 * @param measureName
	 *            identifier of the measure
	 * @return true if a measure with the specified identifier is pending calculation, or
	 *         is currently being calculated. False otherwise.
	 */
	public boolean isMeasurePending( String measureName )
	{
		return computeThread.isMeasurePending( measureName );
	}

	/**
	 * Posts a new task for the thread to process.
	 * 
	 * @param task
	 *            the task to post
	 */
	public void postTask( MeasureTask task )
	{
		computeThread.postTask( task );
	}

	/**
	 * Removes the task from processing queue, if it is not already being processed.
	 * 
	 * @param task
	 *            the task to remove.
	 * @return true if the task was found and removed, false otherwise.
	 */
	public boolean removeTask( MeasureTask task )
	{
		return computeThread.removeTask( task );
	}

	/**
	 * Clears any pending tasks that have been scheduled for computation, but haven't been started yet.
	 */
	public void clearPendingTasks()
	{
		computeThread.clearPendingTasks();
	}

	/**
	 * Returns a set of measures that have been computed thus far for the currently loaded hierarchy.
	 * <p>
	 * This method is not particularly thread-safe, as the map of measures might be updated with new entries
	 * while you are processing the set, resulting in missed entries.
	 * </p>
	 * <p>
	 * For a thread-safe alternative, see {@link #forComputedMeasures(Consumer)}
	 * </p>
	 * 
	 * @see #forComputedMeasures(Consumer)
	 */
	public Set<Map.Entry<String, Object>> getComputedMeasures()
	{
		synchronized ( computedMeasureMap ) {
			return Collections.unmodifiableMap( computedMeasureMap ).entrySet();
		}
	}

	/**
	 * @param identifier
	 *            identifier of the task to look for
	 * @return true if the task is already computed, false otherwise
	 */
	public boolean isMeasureComputed( String identifier )
	{
		synchronized ( computedMeasureMap ) {
			return computedMeasureMap.containsKey( identifier );
		}
	}

	/**
	 * Performs the specified function on the set of measures that have been computed thus far for
	 * the currently loaded hierarchy.
	 * <p>
	 * This method executes the function inside of a synchronized block, preventing the set from
	 * being updated while this method is executing.
	 * </p>
	 */
	public void forComputedMeasures( Consumer<Set<Map.Entry<String, Object>>> function )
	{
		synchronized ( computedMeasureMap ) {
			function.accept( Collections.unmodifiableMap( computedMeasureMap ).entrySet() );
		}
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

	public Collection<MeasureTask> getAllMeasureTasks()
	{
		return measureGroupMap.values().stream()
			.flatMap( Collection::stream )
			.collect( Collectors.toList() );
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

	public void dumpMeasures( Path destinationFile, HVConfig config )
	{
		final Function<Object, String> resultToCSV = data -> {
			if ( data instanceof Double || data instanceof Integer )
				return Objects.toString( data ) + ";0.0;";
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
			Object measureResult = computedMeasureMap.getOrDefault( task.identifier, new double[0] );

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
				Object measureResult = computedMeasureMap.get( task.identifier );

				// Ignore uncomputed measures or histograms for now
				if ( !( measureResult == null || measureResult instanceof double[] ) )
					buf.append( task.identifier ).append( ";stdev;" );
			}
		);

		buf.append( "\n" );
		// Append data values
		buf.append( config.isUseSubtree() ).append( ';' );

		measures.forEach(
			task -> {
				Object measureResult = computedMeasureMap.get( task.identifier );

				// Ignore uncomputed measures or histograms for now
				if ( !( measureResult == null || measureResult instanceof double[] ) )
					buf.append( resultToCSV.apply( measureResult ) );
			}
		);

		buf.append( "\n\n" );

		// -------------------------------------------------------------------------------------------------------
		// Histograms
		measures.forEach(
			task -> {
				Object measureResult = computedMeasureMap.get( task.identifier );

				// Ignore uncomputed histograms
				if ( measureResult == null )
					buf.append( dumpHistogram.apply( task ) );
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

	private void onHierarchyChanged( Hierarchy newHierarchy )
	{
		computeThread.clearPendingTasks();
		computeThread.setHierarchy( newHierarchy );

		computedMeasureMap.clear();
	}

	private void onTaskPosted( MeasureTask task )
	{
		taskPosted.broadcast( task );
	}

	private void onTaskFailed( MeasureTask task )
	{
		taskFailed.broadcast( task );
	}

	private void onMeasureComputing( String measureName )
	{
		measureComputing.broadcast( measureName );
	}

	private void onMeasureComputed( Pair<String, Object> result )
	{
		synchronized ( computedMeasureMap ) {
			computedMeasureMap.put( result.getKey(), result.getValue() );
		}

		measureComputed.broadcast( result );
	}
}
