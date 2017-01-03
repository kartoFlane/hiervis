package pl.pwr.hiervis.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import basic_hierarchy.interfaces.Hierarchy;
import basic_hierarchy.interfaces.Node;
import basic_hierarchy.reader.GeneratedCSVReader;
import pl.pwr.hiervis.HierarchyVisualizer;
import pl.pwr.hiervis.core.HVConfig;
import pl.pwr.hiervis.core.HVConstants;
import pl.pwr.hiervis.core.HVContext;
import pl.pwr.hiervis.core.MeasureComputeThread;
import pl.pwr.hiervis.ui.control.NodeSelectionControl;
import pl.pwr.hiervis.ui.control.PanControl;
import pl.pwr.hiervis.ui.control.SubtreeDragControl;
import pl.pwr.hiervis.ui.control.ZoomScrollControl;
import pl.pwr.hiervis.util.Utils;
import pl.pwr.hiervis.visualisation.HierarchyProcessor;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.controls.Control;
import prefuse.controls.ToolTipControl;
import prefuse.visual.NodeItem;


@SuppressWarnings("serial")
public class VisualizerFrame extends JFrame
{
	private static final Logger log = LogManager.getLogger( VisualizerFrame.class );

	private static final int defaultFrameWidth = 1200;
	private static final int defaultFrameHeight = 900;

	private HVContext context;

	private HierarchyStatisticsFrame statsFrame = null;

	private Display hierarchyDisplay;
	private Display instanceDisplay;

	private ZoomScrollControl hierarchyZoomControl;
	private ZoomScrollControl instanceZoomControl;


	public VisualizerFrame( HVContext context )
	{
		super( HierarchyVisualizer.APP_NAME );

		if ( context == null )
			throw new RuntimeException( "Context must not be null!" );

		this.context = context;

		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		setSize( defaultFrameWidth, defaultFrameHeight );
		setMinimumSize( new Dimension( defaultFrameWidth, defaultFrameHeight / 2 ) );

		addWindowListener(
			new WindowAdapter() {
				@Override
				public void windowClosing( WindowEvent e )
				{
					// Save the current configuration on application exit.
					context.getConfig().to( new File( HVConfig.FILE_PATH ) );

					MeasureComputeThread thread = context.getMeasureComputeThread();
					if ( thread != null ) {
						thread.shutdown();
					}
				}
			}
		);

		createMenu();
		createGUI();

		context.hierarchyChanging.addListener( this::onHierarchyChanging );
		context.hierarchyChanged.addListener( this::onHierarchyChanged );
		context.nodeSelectionChanged.addListener( this::onNodeSelectionChanged );
	}

	private void createGUI()
	{
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWeights = new double[] { 1.0, 1.0 };
		gridBagLayout.rowWeights = new double[] { 1.0 };
		getContentPane().setLayout( gridBagLayout );

		JPanel cTreeViewer = new JPanel();

		GridBagConstraints gbc_cTreeViewer = new GridBagConstraints();
		gbc_cTreeViewer.insets = new Insets( 0, 0, 2, 0 );
		gbc_cTreeViewer.fill = GridBagConstraints.BOTH;
		gbc_cTreeViewer.gridx = 0;
		gbc_cTreeViewer.gridy = 0;
		getContentPane().add( cTreeViewer, gbc_cTreeViewer );

		hierarchyDisplay = new Display( HVConstants.EMPTY_VISUALIZATION );

		hierarchyDisplay.setEnabled( false );
		hierarchyDisplay.setHighQuality( true );
		hierarchyDisplay.setBackground( context.getConfig().getBackgroundColor() );
		hierarchyDisplay.setSize(
			context.getConfig().getTreeWidth(),
			context.getConfig().getTreeHeight()
		);

		hierarchyDisplay.addControlListener( new NodeSelectionControl( context ) );
		hierarchyDisplay.addControlListener( new SubtreeDragControl( Control.RIGHT_MOUSE_BUTTON ) );
		hierarchyDisplay.addControlListener( new PanControl( new Class[] { NodeItem.class } ) );
		hierarchyZoomControl = new ZoomScrollControl();
		hierarchyDisplay.addControlListener( hierarchyZoomControl );
		cTreeViewer.setLayout( new BorderLayout( 0, 0 ) );
		cTreeViewer.add( hierarchyDisplay );

		JPanel cNodeViewer = new JPanel();
		GridBagConstraints gbc_cNodeViewer = new GridBagConstraints();
		gbc_cNodeViewer.insets = new Insets( 0, 2, 0, 0 );
		gbc_cNodeViewer.fill = GridBagConstraints.BOTH;
		gbc_cNodeViewer.gridx = 1;
		gbc_cNodeViewer.gridy = 0;
		getContentPane().add( cNodeViewer, gbc_cNodeViewer );
		cNodeViewer.setLayout( new BorderLayout( 0, 0 ) );

		instanceDisplay = new Display( HVConstants.EMPTY_VISUALIZATION );

		instanceDisplay.setEnabled( false );
		instanceDisplay.setHighQuality( true );
		instanceDisplay.setBackground( context.getConfig().getBackgroundColor() );
		instanceDisplay.setSize(
			context.getConfig().getInstanceWidth(),
			context.getConfig().getInstanceHeight()
		);

		instanceDisplay.addControlListener( new PanControl( true ) );
		instanceZoomControl = new ZoomScrollControl();
		instanceDisplay.addControlListener( instanceZoomControl );
		instanceDisplay.addControlListener( new ToolTipControl( HVConstants.PREFUSE_INSTANCE_LABEL_COLUMN_NAME ) );
		cNodeViewer.add( instanceDisplay );
	}

	private void createMenu()
	{
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar( menuBar );

		createFileMenu( menuBar );
		createViewMenu( menuBar );
	}

	private void createFileMenu( JMenuBar menuBar )
	{
		JMenu mnFile = new JMenu( "File" );
		menuBar.add( mnFile );

		JMenuItem mntmOpenFile = new JMenuItem( "Open file..." );
		mntmOpenFile.addActionListener( e -> openFileSelectionDialog() );
		mnFile.add( mntmOpenFile );

		JSeparator separator = new JSeparator();
		mnFile.add( separator );

		JMenuItem mntmConfig = new JMenuItem( "Config" );
		mntmConfig.addActionListener( e -> openConfigDialog() );
		mnFile.add( mntmConfig );
	}

	private void createViewMenu( JMenuBar menuBar )
	{
		JMenu mnView = new JMenu( "View" );
		menuBar.add( mnView );

		JMenuItem mntmStats = new JMenuItem( "Hierarchy Statistics" );
		mnView.add( mntmStats );

		mntmStats.addActionListener(
			e -> {
				if ( statsFrame == null ) {
					statsFrame = new HierarchyStatisticsFrame( context, this );
					statsFrame.setLocationRelativeTo( null );
				}

				// Restore the frame if it was minimized
				statsFrame.setExtendedState( JFrame.NORMAL );
				statsFrame.setVisible( true );
			}
		);
	}

	/**
	 * Opens a file selection dialog, allowing the user to select a hierarchy file to load.
	 */
	private void openFileSelectionDialog()
	{
		JFileChooser fileDialog = new JFileChooser();
		fileDialog.setCurrentDirectory( new File( "." ) );
		fileDialog.setDialogTitle( "Choose a file" );
		fileDialog.setFileSelectionMode( JFileChooser.FILES_ONLY );
		fileDialog.setAcceptAllFileFilterUsed( false );
		fileDialog.setFileFilter( new FileNameExtensionFilter( "*.csv", "csv" ) );

		if ( fileDialog.showOpenDialog( this ) == JFileChooser.APPROVE_OPTION ) {
			File file = fileDialog.getSelectedFile();
			try {
				log.trace( String.format( "Selected file: '%s'", file ) );

				FileLoadingOptionsDialog optionsDialog = new FileLoadingOptionsDialog( context, this );
				optionsDialog.setVisible( true );

				if ( optionsDialog.hasConfigChanged() ) {
					context.setConfig( optionsDialog.getConfig() );

					log.trace( "Parsing..." );
					Hierarchy hierarchy = new GeneratedCSVReader().load(
						file.getAbsolutePath(),
						context.getConfig().hasInstanceNameAttribute(),
						context.getConfig().hasTrueClassAttribute(),
						context.getConfig().hasDataNamesRow(),
						context.getConfig().isFillBreadthGaps(),
						context.getConfig().isUseSubtree()
					);

					log.trace( "Switching hierarchy..." );
					context.setHierarchy( hierarchy );

					log.trace( "File selection finished." );
				}
				else {
					log.trace( "Loading aborted." );
				}
			}
			catch ( IOException e ) {
				log.error( "Error while loading hierarchy file: " + file.getName(), e );
			}
		}
		else {
			log.trace( "Loading aborted." );
		}
	}

	/**
	 * Opens a configuration dialog, allowing the user to change application settings.
	 * This method blocks until the user exits the dialog.
	 * If the user made changes to the application's settings, the hierarchy visualization
	 * is recreated.
	 */
	private void openConfigDialog()
	{
		ConfigDialog dialog = new ConfigDialog( context, VisualizerFrame.this );

		// Make the dialog appear at the center of the screen
		dialog.setLocationRelativeTo( null );
		dialog.setVisible( true ); // Blocks until the dialog is dismissed.

		if ( dialog.hasConfigChanged() ) {
			log.trace( "Updating current config..." );
			context.setConfig( dialog.getConfig() );

			// If no hierarchy data is currently loaded, then we don't need to reprocess anything.
			if ( context.isHierarchyDataLoaded() ) {
				log.trace( "Reprocessing..." );
				reprocess();
			}
		}

		log.trace( "Config customization finished." );
	}

	/**
	 * Processes the currently loaded hierarchy data, creating and laying out the visualization
	 * using the currently selected settings.
	 * This needs to be called after changes are made to the application's settings so that they
	 * take effect on the interactive visualization.
	 */
	private void reprocess()
	{
		if ( !context.isHierarchyDataLoaded() ) {
			throw new RuntimeException( "No hierarchy data is available." );
		}

		Visualization vis = context.createHierarchyVisualization();
		hierarchyDisplay.setBackground( context.getConfig().getBackgroundColor() );
		hierarchyDisplay.setVisualization( vis );
		HierarchyProcessor.layoutVisualization( vis );

		// Instance visualization will need to be updated to reflect config changes.
		instanceDisplay.setVisualization( HVConstants.EMPTY_VISUALIZATION );

		onNodeSelectionChanged( context.getSelectedRow() );
	}

	private void onHierarchyChanging( Hierarchy h )
	{
		Utils.unzoom( hierarchyDisplay, 0 );
		Utils.unzoom( instanceDisplay, 0 );
		hierarchyDisplay.setVisualization( HVConstants.EMPTY_VISUALIZATION );
		instanceDisplay.setVisualization( HVConstants.EMPTY_VISUALIZATION );
		hierarchyDisplay.setEnabled( false );
		instanceDisplay.setEnabled( false );
	}

	private void onHierarchyChanged( Hierarchy h )
	{
		// If no hierarchy data is currently loaded, then we don't need to reprocess anything.
		if ( context.isHierarchyDataLoaded() ) {
			hierarchyDisplay.setEnabled( true );
			instanceDisplay.setEnabled( true );

			log.trace( "Reprocessing..." );
			reprocess();

			Utils.fitToBounds( hierarchyDisplay, Visualization.ALL_ITEMS, 0, 0 );
			Utils.fitToBounds( instanceDisplay, Visualization.ALL_ITEMS, 0, 0 );
		}

		// Try to coax the VM into reclaiming some of that freed memory.
		System.gc();
	}

	private void onNodeSelectionChanged( int row )
	{
		HierarchyProcessor.updateNodeRoles( context, context.getSelectedRow() );

		// Refresh the hierarchy display so that it reflects node roles correctly
		hierarchyDisplay.damageReport();
		hierarchyDisplay.repaint();

		// However, instance display needs complete redrawing.
		// Save the current display transform so that we can restore it.
		AffineTransform transform = (AffineTransform)instanceDisplay.getTransform().clone();

		Visualization vis = instanceDisplay.getVisualization();
		if ( vis == HVConstants.EMPTY_VISUALIZATION ) {
			Node group = context.findGroup( context.getSelectedRow() );
			vis = context.createInstanceVisualization( group, 0, 1 );
			instanceDisplay.setVisualization( vis );
		}

		// Unzoom the display so that drawing is not botched.
		Utils.unzoom( instanceDisplay, 0 );
		vis.run( "draw" );
		Utils.waitUntilActivitiesAreFinished();

		// Restore the transform for user's convenience
		Utils.setTransform( instanceDisplay, transform );
	}
}
