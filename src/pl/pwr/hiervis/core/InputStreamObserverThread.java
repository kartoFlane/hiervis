package pl.pwr.hiervis.core;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import pl.pwr.hiervis.util.Event;


/**
 * A thread that continually observes and consumes bytes sent down an input stream,
 * and converts them into a string message that can be looked up later.
 * 
 * Primarily intended to be used to intercept console output of programs running as subprocesses.
 * 
 * @author Tomasz Bachmiński
 *
 */
public class InputStreamObserverThread extends Thread
{
	/**
	 * Sent when a new message appears in the input stream.
	 * 
	 * @param first
	 *            the new message
	 */
	public final Event<String> messageReceived = new Event<>();

	private final BufferedReader in;
	private final boolean close;

	private volatile String message;


	/**
	 * 
	 * @param is
	 *            the input stream to observe. The stream will not closed when the thread dies.
	 */
	public InputStreamObserverThread( InputStream is )
	{
		this( is, false );
	}

	/**
	 * 
	 * @param is
	 *            the input stream to observe
	 * @param close
	 *            whether to close the stream once the thread dies.
	 */
	public InputStreamObserverThread( InputStream is, boolean close )
	{
		setDaemon( true );

		in = new BufferedReader( new InputStreamReader( is ) );
		this.close = close;
	}

	@Override
	public void run()
	{
		try {
			while ( !interrupted() ) {
				if ( in.ready() ) {
					String line = in.readLine();
					if ( line != null ) {
						message = line;
						messageReceived.broadcast( message );
					}
				}
			}
		}
		catch ( IOException e ) {
			e.printStackTrace();
		}
		finally {
			try {
				if ( close ) {
					in.close();
				}
			}
			catch ( IOException e ) {
				// Ignore.
			}
		}
	}

	/**
	 * @return the latest message that has been supplied by the input stream.
	 */
	public String getLatestMessage()
	{
		return message;
	}
}
