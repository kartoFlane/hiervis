package pl.pwr.hiervis.core;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import basic_hierarchy.interfaces.Hierarchy;
import pl.pwr.hiervis.util.Event;
import pl.pwr.hiervis.util.SwingUIUtils;


/**
 * Thread used to perform calculations of hierarchy measures.
 * 
 * @author Tomasz Bachmiński
 *
 */
public class MeasureComputeThread extends Thread
{
	private static final Logger log = LogManager.getLogger( MeasureComputeThread.class );

	/** Sent when a measure task is posted for processing. */
	public final Event<MeasureTask> taskPosted = new Event<>();
	/** Sent when a measure computation is started. */
	public final Event<String> measureComputing = new Event<>();
	/** Sent when a measure computation is finished. */
	public final Event<Pair<String, Object>> measureComputed = new Event<>();

	private final ReentrantLock lock = new ReentrantLock();
	private Queue<MeasureTask> tasks = new LinkedList<>();
	private MeasureTask currentTask = null;
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
					Thread.sleep( 100 );
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

				log.trace( String.format( "Computing measure '%s'...", currentTask.identifier ) );
				measureComputing.broadcast( currentTask.identifier );
				try {
					Object result = currentTask.function.apply( hierarchy );
					measureComputed.broadcast( Pair.of( currentTask.identifier, result ) );
				}
				catch ( Throwable e ) {
					String msg = String.format( "An error occurred while computing measure '%s'", currentTask.identifier );
					log.error( msg, e );
					SwingUIUtils.showErrorDialog( msg + ":\n\n" + e.getMessage() + "\n\nCheck log for details." );
				}
			}
			catch ( Throwable e ) {
				log.error( "Unexpected error occurred while processing measures.", e );
			}
		}

		log.trace( "Compute thread terminated." );
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
		boolean result = false;

		lock.lock();
		try {
			if ( currentTask != null && currentTask.identifier.equals( measureName ) ) {
				result = true;
			}
			else {
				for ( MeasureTask task : tasks ) {
					if ( task.identifier.equals( measureName ) ) {
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
	 * Posts a new task for the thread to process.
	 * 
	 * @param task
	 *            the task to post
	 */
	public void postTask( MeasureTask task )
	{
		if ( task == null ) {
			throw new IllegalArgumentException( "Task must not be null!" );
		}
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

		taskPosted.broadcast( task );
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
		if ( task == null ) {
			throw new IllegalArgumentException( "Task must not be null!" );
		}

		boolean result = false;

		lock.lock();
		try {
			result = tasks.remove( task );
		}
		finally {
			lock.unlock();
		}

		return result;
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
	 * Shuts down the thread and cleans up.
	 */
	public void shutdown()
	{
		log.trace( "Shutting down..." );
		interrupt();
		clearPendingTasks();

		taskPosted.clearListeners();
		measureComputing.clearListeners();
		measureComputed.clearListeners();
	}
}
