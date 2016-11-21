package pl.pwr.hiervis.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Map.Entry;

import javax.swing.BoxLayout;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.border.TitledBorder;

import org.apache.commons.lang3.tuple.Pair;

import basic_hierarchy.interfaces.Hierarchy;
import pl.pwr.hiervis.core.HVContext;


/*
 * TODO:
 * This class uses a somewhat hacky solution to make the frame always-on-top ONLY
 * within the application. Normally we could use a JDialog for this, however we
 * also want the user to be able to disable this functionality at will.
 * 
 * Another possible solution to this problem would be having shared GUI-creation code,
 * and then calling it inside of a JFrame or a JDialog, depending on the user-selected setting.
 * Changing the setting while the frame was open would close the old frame and create the new one.
 */


@SuppressWarnings("serial")
public class HierarchyStatisticsFrame extends JFrame
{
	private Window owner;
	private JPanel cMeasures;

	private WindowListener ownerListener;


	public HierarchyStatisticsFrame( HVContext context, Window frame )
	{
		super( "Hierarchy Statistics" );
		owner = frame;

		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		setMinimumSize( new Dimension( 300, 200 ) );
		setSize( 300, 200 );

		ownerListener = new WindowAdapter() {
			@Override
			public void windowActivated( WindowEvent e )
			{
				HierarchyStatisticsFrame.this.setAlwaysOnTop( true );
			}

			@Override
			public void windowDeactivated( WindowEvent e )
			{
				if ( e.getOppositeWindow() == null ) {
					// Disable 'always on top' ONLY when the opposite window
					// (the one that stole focus from us) is not part of our
					// own application.
					HierarchyStatisticsFrame.this.setAlwaysOnTop( false );
				}
			}
		};

		owner.addWindowListener(
			new WindowAdapter() {
				@Override
				public void windowClosing( WindowEvent e )
				{
					// Dispatch closing event instead of calling dispose() directly,
					// so that event listeners are notified.
					dispatchEvent(
						new WindowEvent(
							HierarchyStatisticsFrame.this,
							WindowEvent.WINDOW_CLOSING
						)
					);
				}
			}
		);

		addWindowListener(
			new WindowAdapter() {
				@Override
				public void windowClosing( WindowEvent e )
				{
					context.getMeasureComputeThread().measureComputed.removeListener(
						HierarchyStatisticsFrame.this::onMeasureComputed
					);
				}

				@Override
				public void windowDeactivated( WindowEvent e )
				{
					if ( e.getOppositeWindow() == null ) {
						// Disable 'always on top' ONLY when the opposite window
						// (the one that stole focus from us) is not part of our
						// own application.
						HierarchyStatisticsFrame.this.setAlwaysOnTop( false );
					}
				}
			}
		);

		createGUI();
		createMenu();

		context.getMeasureComputeThread().measureComputed.addListener( this::onMeasureComputed );
		context.hierarchyChanging.addListener( this::onHierarchyChanging );

		context.forComputedMeasures(
			set -> {
				for ( Entry<String, Object> entry : set ) {
					cMeasures.add( createMeasurePanel( entry ) );
				}
			}
		);
	}

	public void setKeepOnTop( boolean onTop )
	{
		setAlwaysOnTop( onTop );

		if ( onTop ) {
			owner.addWindowListener( ownerListener );
		}
		else {
			owner.removeWindowListener( ownerListener );
		}
	}

	// ----------------------------------------------------------------------------------------

	private void createMenu()
	{
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar( menuBar );

		JMenu mnOptions = new JMenu( "Options" );
		menuBar.add( mnOptions );

		JCheckBoxMenuItem mntmAlwaysOnTop = new JCheckBoxMenuItem( "Always On Top" );
		mnOptions.add( mntmAlwaysOnTop );

		mntmAlwaysOnTop.addActionListener(
			( e ) -> {
				setKeepOnTop( mntmAlwaysOnTop.isSelected() );
			}
		);
	}

	private void createGUI()
	{
		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
		getContentPane().add( scrollPane, BorderLayout.CENTER );

		cMeasures = new JPanel();
		scrollPane.setViewportView( cMeasures );
		cMeasures.setLayout( new BoxLayout( cMeasures, BoxLayout.Y_AXIS ) );
	}

	private JPanel createMeasurePanel( Entry<String, Object> entry )
	{
		JPanel cMeasure = new JPanel();
		cMeasure.setBorder( new TitledBorder( null, entry.getKey(), TitledBorder.LEADING, TitledBorder.TOP, null, null ) );
		cMeasure.setLayout( new BorderLayout( 0, 0 ) );

		JLabel lblContent = new JLabel( entry.getValue().toString() );
		cMeasure.add( lblContent, BorderLayout.NORTH );

		return cMeasure;
	}

	private void onMeasureComputed( Pair<String, Object> result )
	{
		cMeasures.add( createMeasurePanel( result ) );
		cMeasures.revalidate();
		cMeasures.repaint();
	}

	private void onHierarchyChanging( Hierarchy oldHierarchy )
	{
		cMeasures.removeAll();
		cMeasures.revalidate();
		cMeasures.repaint();
	}
}
