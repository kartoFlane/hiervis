package pl.pwr.hiervis.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Map.Entry;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;

import org.apache.commons.lang3.tuple.Pair;

import basic_hierarchy.interfaces.Hierarchy;
import internal_measures.statistics.AvgWithStdev;
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


	@SuppressWarnings("unchecked")
	public HierarchyStatisticsFrame( HVContext context, Window frame )
	{
		super( "Hierarchy Statistics" );
		owner = frame;

		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		setMinimumSize( new Dimension( 300, 200 ) );
		setSize( 300, 250 );

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
				addMeasurePanel( set.toArray( new Entry[set.size()] ) );
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

		GridBagLayout layout = new GridBagLayout();
		layout.columnWidths = new int[] { 0 };
		layout.rowHeights = new int[] { 0 };
		layout.columnWeights = new double[] { 1.0 };
		layout.rowWeights = new double[] { Double.MIN_VALUE };
		cMeasures.setLayout( layout );

		scrollPane.setViewportView( cMeasures );
	}

	private JPanel createMeasurePanel( Entry<String, Object> entry )
	{
		JPanel cMeasure = new JPanel();
		cMeasure.setBorder( new TitledBorder( null, entry.getKey(), TitledBorder.LEADING, TitledBorder.TOP, null, null ) );
		cMeasure.setLayout( new BorderLayout( 0, 0 ) );

		cMeasure.add( createMeasureContent( entry.getValue() ), BorderLayout.NORTH );

		return cMeasure;
	}

	@SafeVarargs
	private final void addMeasurePanel( Entry<String, Object>... entries )
	{
		int curItems = cMeasures.getComponentCount();
		int newItems = curItems + entries.length;

		GridBagLayout layout = (GridBagLayout)cMeasures.getLayout();
		layout.rowHeights = new int[newItems + 1];
		layout.rowWeights = new double[newItems + 1];
		layout.rowWeights[newItems] = Double.MIN_VALUE;
		cMeasures.setLayout( layout );

		int i = curItems;
		for ( Entry<String, Object> entry : entries ) {
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.fill = GridBagConstraints.BOTH;
			constraints.gridx = 0;
			constraints.gridy = i;

			cMeasures.add( createMeasurePanel( entry ), constraints );

			++i;
		}

		cMeasures.revalidate();
		cMeasures.repaint();
	}

	private JComponent createMeasureContent( Object result )
	{
		if ( result == null ) {
			throw new IllegalArgumentException( "Result must not be null!" );
		}

		if ( result instanceof double[] ) {
			// Histogram data
			throw new UnsupportedOperationException( "Not implemented yet." );
		}
		else if ( result instanceof AvgWithStdev ) {
			return new JLabel( result.toString() );
		}
		else if ( result instanceof Double ) {
			return new JLabel( result.toString() );
		}
		else {
			throw new IllegalArgumentException(
				String.format(
					"No case defined for data type '%s'",
					result.getClass().getSimpleName()
				)
			);
		}
	}

	private void onMeasureComputed( Pair<String, Object> result )
	{
		SwingUtilities.invokeLater(
			() -> {
				addMeasurePanel( result );
			}
		);
	}

	private void onHierarchyChanging( Hierarchy oldHierarchy )
	{
		cMeasures.removeAll();
		cMeasures.revalidate();
		cMeasures.repaint();
	}
}
