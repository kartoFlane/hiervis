package pl.pwr.hiervis.core;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;

import basic_hierarchy.interfaces.Hierarchy;
import internal_measures.statistics.AvgWithStdev;
import pl.pwr.hiervis.measures.MeasureTask;
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
	private Map<String, Object> measureMap = null;


	public MeasureManager( HVContext context )
	{
		measureMap = new HashMap<>();

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
		synchronized ( measureMap ) {
			return Collections.unmodifiableMap( measureMap ).entrySet();
		}
	}

	/**
	 * @param identifier
	 *            identifier of the task to look for
	 * @return true if the task is already computed, false otherwise
	 */
	public boolean isMeasureComputed( String identifier )
	{
		synchronized ( measureMap ) {
			return measureMap.containsKey( identifier );
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
		synchronized ( measureMap ) {
			function.accept( Collections.unmodifiableMap( measureMap ).entrySet() );
		}
	}

	public void dispose()
	{
		computeThread.shutdown();
	}

	public void dumpMeasures( Path destinationFile, HVConfig config )
	{
		StringBuilder buf = new StringBuilder();

		buf.append( "Use subtree for internal measures?;" )
			.append( MeasureTask.numberOfNodes.identifier ).append( ";stdev;" )
			.append( MeasureTask.numberOfLeaves.identifier ).append( ";stdev;" )
			.append( MeasureTask.height.identifier ).append( ";stdev;" )
			.append( MeasureTask.averagePathLength.identifier ).append( ";stdev;" )
			.append( MeasureTask.varianceDeviation.identifier ).append( ";stdev;" )
			.append( MeasureTask.varianceDeviation2.identifier ).append( ";stdev;" )
			.append( MeasureTask.flatWithinBetween.identifier ).append( ";stdev;" )
			.append( MeasureTask.flatDunn1.identifier ).append( ";stdev;" )
			.append( MeasureTask.flatDunn2.identifier ).append( ";stdev;" )
			.append( MeasureTask.flatDunn3.identifier ).append( ";stdev;" )
			.append( MeasureTask.flatDunn4.identifier ).append( ";stdev;" )
			.append( MeasureTask.flatDaviesBouldin.identifier ).append( ";stdev;" )
			.append( MeasureTask.flatCalinskiHarabasz.identifier ).append( ";stdev;" )
			.append( '\n' );

		final Function<Object, String> dumpData = data -> {
			if ( data instanceof Double || data instanceof Integer )
				return Objects.toString( data ) + ";0.0;";
			else if ( data instanceof AvgWithStdev ) {
				AvgWithStdev avg = (AvgWithStdev)data;
				return avg.getAvg() + ";" + avg.getStdev() + ";";
			}
			throw new IllegalArgumentException( data.getClass().getName() );
		};

		buf.append( config.isUseSubtree() ).append( ';' )
			.append( dumpData.apply( measureMap.getOrDefault( MeasureTask.numberOfNodes.identifier, 0 ) ) )
			.append( dumpData.apply( measureMap.getOrDefault( MeasureTask.numberOfLeaves.identifier, 0 ) ) )
			.append( dumpData.apply( measureMap.getOrDefault( MeasureTask.height.identifier, 0 ) ) )
			.append( dumpData.apply( measureMap.getOrDefault( MeasureTask.averagePathLength.identifier, 0 ) ) )
			.append( dumpData.apply( measureMap.getOrDefault( MeasureTask.varianceDeviation.identifier, 0 ) ) )
			.append( dumpData.apply( measureMap.getOrDefault( MeasureTask.varianceDeviation2.identifier, 0 ) ) )
			.append( dumpData.apply( measureMap.getOrDefault( MeasureTask.flatWithinBetween.identifier, 0 ) ) )
			.append( dumpData.apply( measureMap.getOrDefault( MeasureTask.flatDunn1.identifier, 0 ) ) )
			.append( dumpData.apply( measureMap.getOrDefault( MeasureTask.flatDunn2.identifier, 0 ) ) )
			.append( dumpData.apply( measureMap.getOrDefault( MeasureTask.flatDunn3.identifier, 0 ) ) )
			.append( dumpData.apply( measureMap.getOrDefault( MeasureTask.flatDunn4.identifier, 0 ) ) )
			.append( dumpData.apply( measureMap.getOrDefault( MeasureTask.flatDaviesBouldin.identifier, 0 ) ) )
			.append( dumpData.apply( measureMap.getOrDefault( MeasureTask.flatCalinskiHarabasz.identifier, 0 ) ) )
			.append( '\n' );

		buf.append( '\n' );

		final Function<MeasureTask, String> dumpHistogram = task -> {
			StringBuilder buf2 = new StringBuilder();
			double[] data = (double[])measureMap.getOrDefault( task.identifier, new double[0] );

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

		buf.append( dumpHistogram.apply( MeasureTask.nodesPerLevel ) );
		buf.append( dumpHistogram.apply( MeasureTask.leavesPerLevel ) );
		buf.append( dumpHistogram.apply( MeasureTask.instancesPerLevel ) );
		buf.append( dumpHistogram.apply( MeasureTask.childrenPerNodePerLevel ) );
		buf.append( dumpHistogram.apply( MeasureTask.numberOfChildren ) );

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

		measureMap.clear();
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
		synchronized ( measureMap ) {
			measureMap.put( result.getKey(), result.getValue() );
		}

		measureComputed.broadcast( result );
	}
}
