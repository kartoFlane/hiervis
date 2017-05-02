package pl.pwr.hiervis.measures;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pl.pwr.hiervis.hierarchy.LoadedHierarchy;
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
	public final Event<Pair<LoadedHierarchy, MeasureTask>> taskPosted = new Event<>();
	/** Sent when a measure task computation failed due to an exception. */
	public final Event<Pair<LoadedHierarchy, MeasureTask>> taskFailed = new Event<>();
	/** Sent when a measure computation is started. */
	public final Event<Pair<LoadedHierarchy, String>> measureComputing = new Event<>();
	/** Sent when a measure computation is finished. */
	public final Event<Triple<LoadedHierarchy, String, Object>> measureComputed = new Event<>();

	private final ReentrantLock lock = new ReentrantLock();
	private Queue<Pair<LoadedHierarchy, MeasureTask>> tasks = new LinkedList<>();
	private Pair<LoadedHierarchy, MeasureTask> currentTask = null;


	public MeasureComputeThread()
	{
		setName( "MeasureComputeThread" );
		setDaemon( true );
	}

	@Override
	public void run()
	{
		log.trace( "Compute thread started." );

		LoadedHierarchy hierarchy = null;
		MeasureTask measure = null;

		while ( !isInterrupted() ) {
			if ( tasks.isEmpty() ) {
				try {
					// Nothing to do -- keep the thread alive, polling the queue for new tasks.
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
					hierarchy = currentTask.getLeft();
					measure = currentTask.getRight();
				}
				finally {
					lock.unlock();
				}

				try {
					log.trace( String.format( "Computing measure '%s'...", measure.identifier ) );
					measureComputing.broadcast( Pair.of( hierarchy, measure.identifier ) );

					Object result = measure.computeFunction.apply( hierarchy.data );

					log.trace( String.format( "Finished computing measure '%s'", measure.identifier ) );
					measureComputed.broadcast( Triple.of( hierarchy, measure.identifier, result ) );
				}
				catch ( Throwable e ) {
					Pair<LoadedHierarchy, MeasureTask> t = currentTask;
					currentTask = null;

					taskFailed.broadcast( t );
					String msg = String.format( "An error occurred while computing measure '%s'", t.getRight().identifier );
					log.error( msg, e );
					SwingUIUtils.showErrorDialog( msg + ":\n\n" + e.getMessage() + "\n\nCheck log for details." );
				}
			}
			catch ( Throwable e ) {
				currentTask = null;
				log.error( "Unexpected error occurred while processing measures.", e );
			}
		}

		log.trace( "Compute thread terminated." );
		cleanup();
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
	public boolean isMeasurePending( LoadedHierarchy hierarchy, String measureName )
	{
		boolean result = false;

		lock.lock();
		try {
			if ( currentTask != null ) {
				result = currentTask.getLeft().equals( hierarchy )
					&& currentTask.getRight().identifier.equals( measureName );
			}
			else {
				for ( Pair<LoadedHierarchy, MeasureTask> task : tasks ) {
					if ( task.getLeft().equals( hierarchy )
						&& task.getRight().identifier.equals( measureName ) ) {
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
	public void postTask( LoadedHierarchy hierarchy, MeasureTask task )
	{
		if ( hierarchy == null ) {
			throw new IllegalStateException( "Hierarchy must not be null!" );
		}
		if ( task == null ) {
			throw new IllegalArgumentException( "Task must not be null!" );
		}

		Pair<LoadedHierarchy, MeasureTask> pair = Pair.of( hierarchy, task );

		lock.lock();
		try {
			tasks.add( pair );
		}
		finally {
			lock.unlock();
		}

		taskPosted.broadcast( pair );
	}

	/**
	 * Removes the task from processing queue, if it is not already being processed.
	 * 
	 * @param task
	 *            the task to remove.
	 * @return true if the task was found and removed, false otherwise.
	 */
	public boolean removeTask( LoadedHierarchy hierarchy, MeasureTask task )
	{
		if ( hierarchy == null ) {
			throw new IllegalStateException( "Hierarchy must not be null!" );
		}
		if ( task == null ) {
			throw new IllegalArgumentException( "Task must not be null!" );
		}

		boolean result = false;
		Pair<LoadedHierarchy, MeasureTask> pair = Pair.of( hierarchy, task );

		lock.lock();
		try {
			result = tasks.remove( pair );
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
	 * Shuts down the thread.
	 */
	public void shutdown()
	{
		log.trace( "Shutting down..." );
		interrupt();
	}

	private void cleanup()
	{
		clearPendingTasks();

		taskPosted.clearListeners();
		taskFailed.clearListeners();
		measureComputing.clearListeners();
		measureComputed.clearListeners();

		currentTask = null;
		tasks = null;
	}
}
