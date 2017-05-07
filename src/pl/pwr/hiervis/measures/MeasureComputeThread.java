package pl.pwr.hiervis.measures;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
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
	public final Event<Pair<Hierarchy, MeasureTask>> taskPosted = new Event<>();
	/** Sent when a measure task computation failed due to an exception. */
	public final Event<Pair<Hierarchy, MeasureTask>> taskFailed = new Event<>();
	/** Sent when a measure computation is started. */
	public final Event<Pair<Hierarchy, MeasureTask>> measureComputing = new Event<>();
	/** Sent when a measure computation is finished. */
	public final Event<Triple<Hierarchy, MeasureTask, Object>> measureComputed = new Event<>();

	private final ReentrantLock lock = new ReentrantLock();
	private Queue<Triple<MeasureResultHolder, Hierarchy, MeasureTask>> tasks = new LinkedList<>();
	private Triple<MeasureResultHolder, Hierarchy, MeasureTask> currentTask = null;


	public MeasureComputeThread()
	{
		setName( "MeasureComputeThread" );
		setDaemon( true );
	}

	@Override
	public void run()
	{
		log.trace( "Compute thread started." );

		MeasureResultHolder holder = null;
		Hierarchy hierarchy = null;
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
					holder = currentTask.getLeft();
					hierarchy = currentTask.getMiddle();
					measure = currentTask.getRight();
				}
				finally {
					lock.unlock();
				}

				try {
					log.trace( String.format( "Computing measure '%s'...", measure.identifier ) );
					measureComputing.broadcast( Pair.of( hierarchy, measure ) );

					Object result = measure.computeFunction.apply( hierarchy );
					holder.putMeasureResult( Pair.of( hierarchy, measure ), result );

					log.trace( String.format( "Finished computing measure '%s'", measure.identifier ) );
					measureComputed.broadcast( Triple.of( hierarchy, measure, result ) );
				}
				catch ( Throwable e ) {
					Triple<MeasureResultHolder, Hierarchy, MeasureTask> t = currentTask;
					currentTask = null;

					taskFailed.broadcast( Pair.of( t.getMiddle(), t.getRight() ) );
					String msg = String.format( "An error occurred while computing measure '%s'", t.getRight().identifier );
					log.error( msg, e );
					SwingUIUtils.showErrorDialog( msg + ":\n\n" + e.getMessage() + "\n\nCheck log for details." );
				}
			}
			catch ( Throwable e ) {
				currentTask = null;
				log.error( "Unexpected error occurred while processing measures.", e );
			}
			finally {
				holder = null;
				hierarchy = null;
				measure = null;
			}
		}

		log.trace( "Compute thread terminated." );
		cleanup();
	}

	/**
	 * Checks whether the measure with the specified name is scheduled for processing, or
	 * currently being processed.
	 * 
	 * @param measure
	 *            the task to look for
	 * @return true if a measure with the specified identifier is pending calculation, or
	 *         is currently being calculated. False otherwise.
	 */
	public boolean isMeasurePending( Hierarchy hierarchy, MeasureTask measure )
	{
		boolean result = false;

		lock.lock();
		try {
			if ( currentTask != null ) {
				result = currentTask.getMiddle().equals( hierarchy )
					&& currentTask.getRight().equals( measure );
			}
			else {
				for ( Triple<MeasureResultHolder, Hierarchy, MeasureTask> task : tasks ) {
					if ( task.getMiddle().equals( hierarchy )
						&& task.getRight().equals( measure ) ) {
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
	 * @param holder
	 *            the result holder in which the measure result will be saved
	 * @param hierarchy
	 *            the hierarchy for which the measure is to be computed
	 * @param task
	 *            the task to post
	 */
	public void postTask( MeasureResultHolder holder, Hierarchy hierarchy, MeasureTask task )
	{
		if ( holder == null ) {
			throw new IllegalArgumentException( "Holder must not be null!" );
		}
		if ( hierarchy == null ) {
			throw new IllegalArgumentException( "Hierarchy must not be null!" );
		}
		if ( task == null ) {
			throw new IllegalArgumentException( "Task must not be null!" );
		}

		Triple<MeasureResultHolder, Hierarchy, MeasureTask> arg = Triple.of( holder, hierarchy, task );

		lock.lock();
		try {
			tasks.add( arg );
		}
		finally {
			lock.unlock();
		}

		taskPosted.broadcast( Pair.of( hierarchy, task ) );
	}

	/**
	 * Removes the task from processing queue, if it is not already being processed.
	 * 
	 * @param hierarchy
	 *            the hierarchy for which the measure is to be computed
	 * @param task
	 *            the task to remove.
	 * @return true if the task was found and removed, false otherwise.
	 */
	public boolean removeTask( Hierarchy hierarchy, MeasureTask task )
	{
		if ( hierarchy == null ) {
			throw new IllegalArgumentException( "Hierarchy must not be null!" );
		}
		if ( task == null ) {
			throw new IllegalArgumentException( "Task must not be null!" );
		}

		boolean result = false;

		lock.lock();
		try {
			result = tasks.removeIf( t -> t.getMiddle().equals( hierarchy ) && t.getRight().equals( task ) );
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
