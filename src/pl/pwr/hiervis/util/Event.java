package pl.pwr.hiervis.util;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;


/**
 * This class is meant to emulate C#-like events in syntax, in order to avoid Java's listener
 * system and its terrible verbosity and code duplication.
 * 
 * <p>
 * One grave shortcoming of this class is that the {@link #broadcast(Object)} method is public,
 * therefore anyone can invoke the handler and broadcast events to listeners.
 * This makes this class unusable for anything other than private projects.
 * </p>
 * <p>
 * Theoretically this class should be thread-safe, but this has not been tested at all.
 * </p>
 * 
 * @author Tomasz Bachmiñski
 *
 * @param <T>
 *            the argument taken by listeners of this event
 */
public class Event<T>
{
	private Set<Consumer<T>> listeners = null;


	public void addListener( Consumer<T> listener )
	{
		if ( listener == null )
			return;
		if ( listeners == null )
			listeners = new CopyOnWriteArraySet<>();
		listeners.add( listener );
	}

	public void removeListener( Consumer<T> listener )
	{
		if ( listener == null )
			return;
		if ( listeners == null || listeners.size() == 0 )
			return;
		listeners.remove( listener );
	}

	public void clearListeners()
	{
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

	private void safeNotify( Consumer<T> listener, T args )
	{
		try {
			listener.accept( args );
		}
		catch ( RuntimeException e ) {
			e.printStackTrace();
		}
	}
}
