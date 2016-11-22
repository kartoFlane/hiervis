package pl.pwr.hiervis.core;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import basic_hierarchy.interfaces.Hierarchy;
import pl.pwr.hiervis.util.Event;


/**
 * Thread used to perform calculations of hierarchy measures.
 * 
 * @author Tomasz Bachmiñski
 *
 */
public class MeasureComputeThread extends Thread
{
	private static final Logger log = LogManager.getLogger( MeasureComputeThread.class );

	/** Sent when a measure computation is started. */
	public final Event<String> measureComputing = new Event<>();
	/** Sent when a measure computation is finished. */
	public final Event<Pair<String, Object>> measureComputed = new Event<>();

	private final ReentrantLock lock = new ReentrantLock();
	private Queue<Pair<String, Function<Hierarchy, Object>>> tasks = new LinkedList<>();
	private Pair<String, Function<Hierarchy, Object>> currentTask = null;
	private Hierarchy hierarchy;


	public MeasureComputeThread()
	{
		setDaemon( true );
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
		if ( hierarchy == null )
			throw new IllegalArgumentException( "Hierarchy must not be null!" );
		if ( !tasks.isEmpty() )
			throw new IllegalStateException( "Hierarchy cannot be changed while there are still tasks pending!" );

		this.hierarchy = hierarchy;
	}

	@Override
	public void run()
	{
		log.trace( "Compute thread started." );

		while ( !isInterrupted() ) {
			if ( tasks.isEmpty() ) {
				try {
					// Nothing to do -- keep the thread alive, polling the queue for new tasks once every second.
					Thread.sleep( 1000 );
				}
				catch ( InterruptedException e ) {
				}

				continue;
			}

			try {
				currentTask = null;

				lock.lock();
				try {
					currentTask = tasks.poll();
				}
				finally {
					lock.unlock();
				}

				String id = currentTask.getKey();
				Function<Hierarchy, Object> computation = currentTask.getValue();

				log.trace( String.format( "Computing measure '%s'...", id ) );
				measureComputing.broadcast( id );
				try {
					Object result = computation.apply( hierarchy );
					measureComputed.broadcast( Pair.of( id, result ) );
				}
				catch ( Exception e ) {
					log.error( String.format( "An error ocurred while computing measure '%s'.", id ), e );
				}
			}
			catch ( Exception e ) {
				log.error( "Unexpected error ocurred while processing measures.", e );
			}
		}

		measureComputing.clearListeners();
		measureComputed.clearListeners();

		log.trace( "Compute thread shut down." );
	}

	/**
	 * 
	 * @param measureName
	 *            identifier of the measure
	 * @return true if a measure with the specified identifier is pending calculation, or
	 *         is currently being calculated. False otherwise.
	 */
	public boolean isMeasurePending( String measureName )
	{
		boolean result = false;

		lock.lock();
		try {
			if ( currentTask != null && currentTask.getKey().equals( measureName ) ) {
				result = true;
			}
			else {
				for ( Pair<String, Function<Hierarchy, Object>> task : tasks ) {
					if ( task.getKey().equals( measureName ) ) {
						result = true;
						break;
					}
				}
			}
		}
		finally {
			lock.unlock();
		}

		return result;
	}

	/**
	 * {@link #postTask(String, Function)}
	 */
	public void postTask( Pair<String, Function<Hierarchy, Object>> task )
	{
		if ( hierarchy == null ) {
			throw new IllegalStateException( "No hierarchy has been set!" );
		}

		lock.lock();
		try {
			tasks.add( task );
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Posts a new task for the thread to process.
	 * 
	 * @param measureName
	 *            name of the computed measure. This will be displayed in the interface for the user to see.
	 * @param measureFunction
	 *            function that will compute the measure
	 */
	public void postTask( String measureName, Function<Hierarchy, Object> measureFunction )
	{
		postTask( Pair.of( measureName, measureFunction ) );
	}

	/**
	 * Clears any pending tasks that have been scheduled for computation, but haven't been started yet.
	 */
	public void clearPendingTasks()
	{
		lock.lock();
		try {
			tasks.clear();
		}
		finally {
			lock.unlock();
		}
	}

	/**
	 * Shuts down the thread.
	 */
	public void shutdown()
	{
		log.trace( "Shutting down..." );
		interrupt();
		clearPendingTasks();
	}
}
