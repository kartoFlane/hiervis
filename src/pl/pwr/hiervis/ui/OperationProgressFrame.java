package pl.pwr.hiervis.ui;

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

import pl.pwr.hiervis.util.GridBagConstraintsBuilder;
import pl.pwr.hiervis.util.SwingUIUtils;


/**
 * A simple dialog that can report progress of any operation that can quantify its progress as
 * an integer from range [0, 100].
 * 
 * @author Tomasz Bachmiński
 *
 */
@SuppressWarnings("serial")
public class OperationProgressFrame extends JDialog
{
	private JProgressBar progressBar;
	private JButton button;

	private Supplier<Integer> updateCallback;
	private Timer timer;


	public OperationProgressFrame( Window owner, String title )
	{
		super( owner, title );

		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		setMinimumSize( new Dimension( 200, 100 ) );

		GridBagLayout layout = new GridBagLayout();
		layout.columnWidths = new int[] { 0, 0 };
		layout.rowHeights = new int[] { 0, 0 };
		layout.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		layout.rowWeights = new double[] { 1.0, 0.0 };
		getContentPane().setLayout( layout );

		GridBagConstraintsBuilder builder = new GridBagConstraintsBuilder();

		progressBar = new JProgressBar();
		progressBar.setIndeterminate( true );
		getContentPane().add( progressBar, builder.insets( 5, 5, 5, 5 ).position( 0, 0 ).fillHorizontal().build() );

		button = new JButton( "Abort" );
		button.setEnabled( false );
		getContentPane().add( button, builder.anchorCenter().insets( 0, 5, 5, 5 ).position( 0, 1 ).build() );

		SwingUIUtils.addCloseCallback( this, () -> button.doClick() );
	}

	/**
	 * Sets the operation to execute when the user presses the 'Abort' button.
	 * 
	 * @param abortOperation
	 *            the operation to perform when the 'Abort' button is pressed.
	 *            If null, the button is disabled.
	 */
	public void setAbortOperation( ActionListener abortOperation )
	{
		ActionListener[] listeners = button.getActionListeners();
		for ( ActionListener listener : listeners )
			button.removeActionListener( listener );

		if ( abortOperation != null )
			button.addActionListener( abortOperation );
		button.setEnabled( abortOperation != null );
	}

	/**
	 * Sets the callback that supplies an integer representing the operation's progress.
	 * 
	 * @param progressSupplier
	 *            a callback that provides an integer, which represents the operation's progress.
	 *            If null, the progress bar is made indeterminate.
	 */
	public void setProgressUpdateCallback( Supplier<Integer> progressSupplier )
	{
		if ( progressSupplier == null ) {
			cleanupTimer();
		}

		progressBar.setIndeterminate( progressSupplier == null );
		updateCallback = progressSupplier;
	}

	/**
	 * Sets the interval at which to poll the {@code updateCallback} and update the progress bar.
	 * Calling this method when {@code updateCallback} is not set throws an exception.
	 * 
	 * @param intervalMs
	 *            the interval, in miliseconds. If value is less than or equal to 0, polling is disabled.
	 */
	public void setProgressPollInterval( int intervalMs )
	{
		if ( updateCallback == null )
			throw new IllegalStateException( "No updateCallback has been set!" );

		cleanupTimer();

		if ( intervalMs > 0 ) {
			timer = new Timer( true );
			TimerTask tt = new TimerTask() {

				@Override
				public void run()
				{
					updateProgressLater();
				}
			};

			timer.scheduleAtFixedRate( tt, 0, intervalMs );
		}
	}

	/**
	 * Schedules an update, so that the progress bar displays the most recent progress value.
	 * If {@code updateCallback} is not set, this method does nothing.
	 */
	public void updateProgressLater()
	{
		// The actual update call is deferred and performed on the main thread,
		// so updateCallback's value *might* have changed.
		if ( updateCallback != null ) {
			SwingUtilities.invokeLater(
				() -> {
					if ( updateCallback != null )
						progressBar.setValue( updateCallback.get() );
				}
			);
		}
	}

	private void cleanupTimer()
	{
		if ( timer != null ) {
			timer.cancel();
			timer = null;
		}
	}

	@Override
	public void dispose()
	{
		super.dispose();

		cleanupTimer();
	}
}
