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
import javax.swing.Box;
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
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.JTextComponent;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
	private static final Logger log = LogManager.getLogger( HierarchyStatisticsFrame.class );

	private HVContext context;
	private Window owner;

	private JPanel cMeasures;
	private JMenuItem mntmDump;

	private WindowListener ownerListener;

	private HashMap<String, JPanel> measurePanelMap = new HashMap<>();


	public HierarchyStatisticsFrame( HVContext context, Window frame, String subtitle )
	{
		super( "Statistics Frame" + ( subtitle == null ? "" : ( " [ " + subtitle + " ]" ) ) );
		this.context = context;
		this.owner = frame;

		setDefaultCloseOperation( HIDE_ON_CLOSE );
		setMinimumSize( new Dimension( 400, 200 ) );
		setSize( 400, 350 );

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

		addWindowListener(
			new WindowAdapter() {
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
		createMeasurePanels();

		context.getMeasureComputeThread().measureComputing.addListener( this::onMeasureComputing );
		context.getMeasureComputeThread().measureComputed.addListener( this::onMeasureComputed );
		context.hierarchyChanging.addListener( this::onHierarchyChanging );
		context.hierarchyChanged.addListener( this::onHierarchyChanged );

		VisualizerFrame.createFileDrop( this, log, "csv", file -> context.loadFile( this, file ) );

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

		mntmDump = new JMenuItem( "Dump Measures" );
		mntmDump.setEnabled( context.isHierarchyDataLoaded() );
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

	private void createMeasurePanels()
	{
		addMeasurePanels(
			createBulkTaskPanel(
				"Calculate All",
				MeasureTask.numberOfNodes, MeasureTask.numberOfLeaves,
				MeasureTask.height, MeasureTask.averagePathLength,
				MeasureTask.nodesPerLevel, MeasureTask.leavesPerLevel,
				MeasureTask.instancesPerLevel, MeasureTask.childrenPerNodePerLevel,
				MeasureTask.numberOfChildren,
				MeasureTask.varianceDeviation, MeasureTask.varianceDeviation2,
				MeasureTask.flatWithinBetween, MeasureTask.flatDunn1,
				MeasureTask.flatDunn2, MeasureTask.flatDunn3, MeasureTask.flatDunn4,
				MeasureTask.flatDaviesBouldin, MeasureTask.flatCalinskiHarabasz
			),

			createFillerPanel( 10 ),
			createSeparatorPanel( "Statistics" ),
			createBulkTaskPanel(
				"Calculate All Statistics",
				MeasureTask.numberOfNodes, MeasureTask.numberOfLeaves,
				MeasureTask.height, MeasureTask.averagePathLength
			),
			createPendingMeasurePanel( MeasureTask.numberOfNodes ),
			createPendingMeasurePanel( MeasureTask.numberOfLeaves ),
			createPendingMeasurePanel( MeasureTask.height ),
			createPendingMeasurePanel( MeasureTask.averagePathLength ),

			createFillerPanel( 10 ),
			createSeparatorPanel( "Histograms" ),
			createBulkTaskPanel(
				"Calculate All Histograms",
				MeasureTask.nodesPerLevel, MeasureTask.leavesPerLevel,
				MeasureTask.instancesPerLevel, MeasureTask.childrenPerNodePerLevel,
				MeasureTask.numberOfChildren
			),
			createPendingMeasurePanel( MeasureTask.nodesPerLevel ),
			createPendingMeasurePanel( MeasureTask.leavesPerLevel ),
			createPendingMeasurePanel( MeasureTask.instancesPerLevel ),
			createPendingMeasurePanel( MeasureTask.childrenPerNodePerLevel ),
			createPendingMeasurePanel( MeasureTask.numberOfChildren ),

			createFillerPanel( 10 ),
			createSeparatorPanel( "Internal Measures" ),
			createBulkTaskPanel(
				"Calculate All Internal Measures",
				MeasureTask.varianceDeviation, MeasureTask.varianceDeviation2,
				MeasureTask.flatWithinBetween, MeasureTask.flatDunn1,
				MeasureTask.flatDunn2, MeasureTask.flatDunn3, MeasureTask.flatDunn4,
				MeasureTask.flatDaviesBouldin, MeasureTask.flatCalinskiHarabasz
			),
			createPendingMeasurePanel( MeasureTask.varianceDeviation ),
			createPendingMeasurePanel( MeasureTask.varianceDeviation2 ),
			createPendingMeasurePanel( MeasureTask.flatWithinBetween ),
			createPendingMeasurePanel( MeasureTask.flatDunn1 ),
			createPendingMeasurePanel( MeasureTask.flatDunn2 ),
			createPendingMeasurePanel( MeasureTask.flatDunn3 ),
			createPendingMeasurePanel( MeasureTask.flatDunn4 ),
			createPendingMeasurePanel( MeasureTask.flatDaviesBouldin ),
			createPendingMeasurePanel( MeasureTask.flatCalinskiHarabasz )
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

	private JPanel createFillerPanel( int height )
	{
		JPanel cFiller = new JPanel();

		cFiller.add( Box.createVerticalStrut( height ) );

		return cFiller;
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

		JLabel lblTitle = new JLabel( title );
		GridBagConstraints constraintsLabel = new GridBagConstraints();
		constraintsLabel.insets = new Insets( 0, 0, 5, 0 );
		constraintsLabel.fill = GridBagConstraints.VERTICAL;
		constraintsLabel.gridx = 1;
		constraintsLabel.gridy = 0;
		cSeparator.add( lblTitle, constraintsLabel );

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

		cMeasure.add( createTaskButton( task ), BorderLayout.NORTH );
		measurePanelMap.put( task.identifier, cMeasure );

		return cMeasure;
	}

	private JPanel createBulkTaskPanel( String title, MeasureTask... tasks )
	{
		JPanel cMeasure = new JPanel();
		cMeasure.setLayout( new BorderLayout( 0, 0 ) );
		cMeasure.add( createTaskButton( title, tasks ), BorderLayout.NORTH );

		return cMeasure;
	}

	private JButton createTaskButton( String title, MeasureTask... tasks )
	{
		JButton button = new JButton();
		button.addActionListener(
			( e ) -> {
				button.setEnabled( false );

				for ( MeasureTask task : tasks ) {
					if ( !context.isMeasureComputed( task.identifier ) &&
						!context.getMeasureComputeThread().isMeasurePending( task.identifier ) ) {

						context.getMeasureComputeThread().postTask( task );
					}
				}
			}
		);

		button.setEnabled( context.isHierarchyDataLoaded() );
		button.setText( title );

		return button;
	}

	private JButton createTaskButton( MeasureTask task )
	{
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
		return button;
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
			for ( int i = 0; i < data.length; ++i ) {
				buf.append( Integer.toString( i ) )
					.append( ": " )
					.append( Double.toString( data[i] ) );

				if ( i + 1 < data.length )
					buf.append( "\n" );
			}

			return createFixedTextComponent( buf.toString() );
		}
		else if ( result instanceof AvgWithStdev ) {
			AvgWithStdev avg = (AvgWithStdev)result;
			return createFixedTextComponent( String.format( "%s ± %s", avg.getAvg(), avg.getStdev() ) );
		}
		else if ( result instanceof Double ) {
			return createFixedTextComponent( result.toString() );
		}
		else if ( result instanceof String ) {
			return createFixedTextComponent( result.toString() );
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

	private JTextComponent createFixedTextComponent( String msg )
	{
		JTextArea result = new JTextArea( msg );
		result.setEditable( false );
		result.setBorder( UIManager.getBorder( "TextField.border" ) );

		return result;
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
		SwingUtilities.invokeLater( () -> updateMeasurePanel( result ) );
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
		mntmDump.setEnabled( newHierarchy != null );
		createMeasurePanels();
	}
}
