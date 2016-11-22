package pl.pwr.hiervis.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.lang3.tuple.Pair;

import basic_hierarchy.interfaces.Hierarchy;
import internal_measures.statistics.AvgWithStdev;
import pl.pwr.hiervis.core.HVContext;
import pl.pwr.hiervis.core.MeasureTask;


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
	private HVContext context;

	private JPanel cMeasures;

	private WindowListener ownerListener;
	private HashMap<String, JPanel> measurePanelMap = new HashMap<>();


	public HierarchyStatisticsFrame( HVContext context, Window frame )
	{
		super( "Hierarchy Statistics" );
		owner = frame;
		this.context = context;

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
		createMesurePanels();

		context.getMeasureComputeThread().measureComputing.addListener( this::onMeasureComputing );
		context.getMeasureComputeThread().measureComputed.addListener( this::onMeasureComputed );
		context.hierarchyChanging.addListener( this::onHierarchyChanging );
		context.hierarchyChanged.addListener( this::onHierarchyChanged );

		context.forComputedMeasures(
			set -> {
				set.stream().forEach( this::updateMeasurePanel );
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

		JMenuItem mntmDump = new JMenuItem( "Dump Measures" );
		mnOptions.add( mntmDump );

		mntmDump.addActionListener(
			( e ) -> {
				JFileChooser fileDialog = new JFileChooser();
				fileDialog.setCurrentDirectory( new File( "." ) );
				fileDialog.setDialogTitle( "Choose a file" );
				fileDialog.setFileSelectionMode( JFileChooser.FILES_ONLY );
				fileDialog.setAcceptAllFileFilterUsed( false );
				fileDialog.setFileFilter( new FileNameExtensionFilter( "*.csv", "csv" ) );
				fileDialog.setSelectedFile( new File( "dump.csv" ) );

				if ( fileDialog.showSaveDialog( this ) == JFileChooser.APPROVE_OPTION ) {
					context.dumpMeasures( fileDialog.getSelectedFile().getAbsolutePath() );
				}
			}
		);

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
		scrollPane.getVerticalScrollBar().setUnitIncrement( 8 );
		scrollPane.setBorder( BorderFactory.createEmptyBorder() );
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

	private void createMesurePanels()
	{
		addMeasurePanels(
			createSeparatorPanel( "Statistics" ),
			createPendingMeasurePanel( MeasureTask.numberOfNodes ),
			createPendingMeasurePanel( MeasureTask.numberOfLeaves ),
			createPendingMeasurePanel( MeasureTask.height ),
			createPendingMeasurePanel( MeasureTask.averagePathLength ),

			createSeparatorPanel( "Internal" ),
			createPendingMeasurePanel( MeasureTask.varianceDeviation ),
			createPendingMeasurePanel( MeasureTask.varianceDeviation2 ),
			createPendingMeasurePanel( MeasureTask.flatWithinBetween ),
			createPendingMeasurePanel( MeasureTask.flatDunn1 ),
			createPendingMeasurePanel( MeasureTask.flatDunn2 ),
			createPendingMeasurePanel( MeasureTask.flatDunn3 ),
			createPendingMeasurePanel( MeasureTask.flatDunn4 ),
			createPendingMeasurePanel( MeasureTask.flatDaviesBouldin ),
			createPendingMeasurePanel( MeasureTask.flatCalinskiHarabasz ),

			createSeparatorPanel( "Histograms" ),
			createPendingMeasurePanel( MeasureTask.nodesPerLevel ),
			createPendingMeasurePanel( MeasureTask.leavesPerLevel ),
			createPendingMeasurePanel( MeasureTask.instancesPerLevel ),
			createPendingMeasurePanel( MeasureTask.childrenPerNodePerLevel ),
			createPendingMeasurePanel( MeasureTask.numberOfChildren )
		);
	}

	private void addMeasurePanels( JPanel... panels )
	{
		int curItems = cMeasures.getComponentCount();
		int newItems = curItems + panels.length;

		GridBagLayout layout = (GridBagLayout)cMeasures.getLayout();
		layout.rowHeights = new int[newItems + 1];
		layout.rowWeights = new double[newItems + 1];
		layout.rowWeights[newItems] = Double.MIN_VALUE;
		cMeasures.setLayout( layout );

		int i = curItems;
		for ( JPanel panel : panels ) {
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.fill = GridBagConstraints.BOTH;
			constraints.gridx = 0;
			constraints.gridy = i;
			constraints.insets = new Insets( 5, 5, 0, 5 );

			cMeasures.add( panel, constraints );

			++i;
		}

		cMeasures.revalidate();
		cMeasures.repaint();
	}

	private JPanel createSeparatorPanel( String title )
	{
		JPanel cSeparator = new JPanel();

		GridBagLayout layout = new GridBagLayout();
		layout.columnWidths = new int[] { 0, 0, 0, 0 };
		layout.rowHeights = new int[] { 0, 0 };
		layout.columnWeights = new double[] { 1.0, 0.0, 1.0, Double.MIN_VALUE };
		layout.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
		cSeparator.setLayout( layout );

		JSeparator sepLeft = new JSeparator();
		GridBagConstraints constraintsSepLeft = new GridBagConstraints();
		constraintsSepLeft.insets = new Insets( 0, 5, 5, 5 );
		constraintsSepLeft.fill = GridBagConstraints.HORIZONTAL;
		constraintsSepLeft.gridx = 0;
		constraintsSepLeft.gridy = 0;
		cSeparator.add( sepLeft, constraintsSepLeft );

		JLabel lblNewLabel = new JLabel( title );
		GridBagConstraints constraintsLabel = new GridBagConstraints();
		constraintsLabel.insets = new Insets( 0, 0, 5, 0 );
		constraintsLabel.fill = GridBagConstraints.VERTICAL;
		constraintsLabel.gridx = 1;
		constraintsLabel.gridy = 0;
		cSeparator.add( lblNewLabel, constraintsLabel );

		JSeparator sepRight = new JSeparator();
		GridBagConstraints constraintsSepRight = new GridBagConstraints();
		constraintsSepRight.insets = new Insets( 0, 5, 5, 5 );
		constraintsSepRight.fill = GridBagConstraints.HORIZONTAL;
		constraintsSepRight.gridx = 2;
		constraintsSepRight.gridy = 0;
		cSeparator.add( sepRight, constraintsSepRight );

		return cSeparator;
	}

	private JPanel createPendingMeasurePanel( MeasureTask task )
	{
		JPanel cMeasure = new JPanel();
		cMeasure.setBorder( new TitledBorder( null, task.identifier, TitledBorder.LEADING, TitledBorder.TOP, null, null ) );
		cMeasure.setLayout( new BorderLayout( 0, 0 ) );

		JButton button = new JButton();
		button.addActionListener(
			( e ) -> {
				boolean pending = context.getMeasureComputeThread().isMeasurePending( task.identifier );
				updateTaskButton( button, !pending );

				if ( pending ) {
					context.getMeasureComputeThread().removeTask( task );
				}
				else {
					context.getMeasureComputeThread().postTask( task );
				}
			}
		);
		updateTaskButton( button, false );

		cMeasure.add( button, BorderLayout.NORTH );
		measurePanelMap.put( task.identifier, cMeasure );

		return cMeasure;
	}

	private void updateTaskButton( JButton button, boolean pending )
	{
		button.setEnabled( context.isHierarchyDataLoaded() );
		button.setText( pending ? "Abort" : "Calculate" );
	}

	/**
	 * Creates a GUI component used to represent the specified measure computation result.
	 * 
	 * @param result
	 *            the measure computation result to create the component for
	 * @return the GUI component
	 */
	private JComponent createMeasureContent( Object result )
	{
		if ( result == null ) {
			throw new IllegalArgumentException( "Result must not be null!" );
		}

		if ( result instanceof double[] ) {
			// Histogram data // TODO
			double[] data = (double[])result;

			StringBuilder buf = new StringBuilder();
			buf.append( "<html><body>" );
			for ( int i = 0; i < data.length; ++i ) {
				buf.append( Integer.toString( i ) )
					.append( ": " )
					.append( Double.toString( data[i] ) );

				if ( i + 1 < data.length )
					buf.append( "<br/>" );
			}
			buf.append( "</html></body>" );
			return new JLabel( buf.toString() );
		}
		else if ( result instanceof AvgWithStdev ) {
			return new JLabel( result.toString() );
		}
		else if ( result instanceof Double ) {
			return new JLabel( result.toString() );
		}
		else if ( result instanceof String ) {
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

	private void updateMeasurePanel( Entry<String, Object> result )
	{
		JPanel panel = measurePanelMap.get( result.getKey() );
		panel.removeAll();

		panel.add( createMeasureContent( result.getValue() ), BorderLayout.NORTH );
		panel.revalidate();
		panel.repaint();
	}

	private void onMeasureComputing( String measureName )
	{
		SwingUtilities.invokeLater(
			() -> {
				if ( measurePanelMap.containsKey( measureName ) ) {
					JPanel panel = measurePanelMap.get( measureName );
					JButton button = (JButton)panel.getComponent( 0 );
					button.setEnabled( false );
					button.setText( "Calculating..." );
				}
				else {
					throw new IllegalArgumentException(
						String.format(
							"Implementation error: %s does not have UI component for measure '%s'.",
							this.getClass().getSimpleName(), measureName
						)
					);
				}
			}
		);
	}

	private void onMeasureComputed( Pair<String, Object> result )
	{
		SwingUtilities.invokeLater(
			() -> {
				updateMeasurePanel( result );
			}
		);
	}

	private void onHierarchyChanging( Hierarchy oldHierarchy )
	{
		measurePanelMap.clear();

		cMeasures.removeAll();
		cMeasures.revalidate();
		cMeasures.repaint();
	}

	private void onHierarchyChanged( Hierarchy newHierarchy )
	{
		createMesurePanels();
	}
}
