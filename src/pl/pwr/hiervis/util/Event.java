package pl.pwr.hiervis.util;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * This class is meant to emulate C#-like events in syntax, in order to avoid Java's listener
 * system and its terrible verbosity and code duplication.
 * 
 * <p>
 * One grave shortcoming of this class is that the {@link #broadcast(Object)} method is public,
 * therefore anyone can invoke the handler and broadcast events to listeners.
 * This lack of fine access control makes this class unusable for anything other than private projects.
 * </p>
 * 
 * <h1>Thread Safety</h1>
 * <p>
 * Theoretically this class should be thread-safe, but this has not been tested at all.
 * </p>
 * <p>
 * An important thing to note when using this class with threads: listeners have their handler
 * methods executed on the thread that invoked the {@link #broadcast(Object)} method.
 * Consider using a lock, or executing the handler code inside of a {@code synchronized} block,
 * to avoid potential threading issues.
 * </p>
 * 
 * <h1>Java 8 Lambdas and Method References</h1>
 * <p>
 * Using this class in conjunction with Java 8 method references requires one to be aware
 * that each method reference ({@code this::example}) creates a new closure. Despite referencing the same
 * method, these closures are not only different, but also impossible to compare. Both {@code ==} and
 * {@code equals()} will return false.
 * As such, it's not possible to do something like this:
 * </p>
 * <p>
 * <code>
 * event.removeListener( this::onEvent )
 * </code>
 * </p>
 * <p>
 * ...as much as I'd want to. The listener has to be saved to a variable when registered, and then
 * unregistered by passing that variable into {@link #removeListener(Consumer)}.
 * </p>
 * 
 * @author Tomasz Bachmiński
 *
 * @param <T>
 *            the argument taken by listeners of this event
 */
public class Event<T>
{
	private static final Logger log = LogManager.getLogger( Event.class );

	private Set<Consumer<? super T>> listeners = null;


	public Consumer<? super T> addListener( Consumer<? super T> listener )
	{
		if ( listener == null )
			throw new IllegalArgumentException( "Argument must not be null." );
		if ( listeners == null )
			listeners = new CopyOnWriteArraySet<>();
		listeners.add( listener );
		return listener;
	}

	public void removeListener( Consumer<? super T> listener )
	{
		if ( listener == null )
			throw new IllegalArgumentException( "Argument must not be null." );
		if ( listeners == null || listeners.size() == 0 )
			return;
		if ( !listeners.remove( listener ) ) {
			log.trace(
				"Tried to remove a listener that was not registered.\n",
				new Exception( "Stack trace" )
			);
		}
	}

	/**
	 * Clears the list of listeners of this event, so no dangling references are left over.
	 */
	public void clearListeners()
	{
		if ( listeners != null )
			listeners.clear();
	}

	/**
	 * Notifies all registered listeners of this event.
	 * <p>
	 * <b>DO NOT CALL THIS:</b>
	 * </p>
	 * <p>
	 * This method is made public only so that the owner of the event handler can broadcast the event.
	 * Calling this method anywhere else violates the principle of loosely-coupled, event-driven
	 * communication between components.
	 * </p>
	 */
	public void broadcast( T args )
	{
		if ( listeners == null )
			return;
		// Iterates over a snapshot of the original collection
		listeners.forEach( listener -> safeNotify( listener, args ) );
	}

	/**
	 * Notifies the listener of the event inside of a try-catch block, so that if a single
	 * listener throws an exception, it won't break the whole chain.
	 * 
	 * @param listener
	 *            the listener to notify
	 * @param args
	 *            the event arguments to pass
	 */
	private void safeNotify( Consumer<? super T> listener, T args )
	{
		try {
			listener.accept( args );
		}
		catch ( RuntimeException e ) {
			e.printStackTrace();
		}
	}
}
