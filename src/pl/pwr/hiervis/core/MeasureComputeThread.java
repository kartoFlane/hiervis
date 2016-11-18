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


public class MeasureComputeThread extends Thread
{
	private static final Logger log = LogManager.getLogger( MeasureComputeThread.class );

	public final Event<Pair<String, Object>> measureComputed = new Event<>();

	private final ReentrantLock lock = new ReentrantLock();
	private Queue<Pair<String, Function<Hierarchy, Object>>> tasks = new LinkedList<>();
	private HVContext context;


	public MeasureComputeThread( HVContext context )
	{
		this.context = context;
		setDaemon( true );
	}

	@Override
	public void run()
	{
		log.trace( "Compute thread started." );
		while ( !tasks.isEmpty() && !isInterrupted() ) {
			Pair<String, Function<Hierarchy, Object>> task = null;

			lock.lock();
			try {
				task = tasks.poll();
			}
			finally {
				lock.unlock();
			}

			Object result = task.getValue().apply( context.getHierarchy() );
			measureComputed.broadcast( Pair.of( task.getKey(), result ) );
		}
	}

	public void postTask( Pair<String, Function<Hierarchy, Object>> task )
	{
		lock.lock();
		try {
			tasks.add( task );
		}
		finally {
			lock.unlock();
		}
	}

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
}
