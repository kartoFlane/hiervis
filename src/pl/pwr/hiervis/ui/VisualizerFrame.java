package pl.pwr.hiervis.ui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Locale;
import java.util.function.Consumer;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import basic_hierarchy.interfaces.Hierarchy;
import pl.pwr.hiervis.HierarchyVisualizer;
import pl.pwr.hiervis.core.HVConfig;
import pl.pwr.hiervis.core.HVConstants;
import pl.pwr.hiervis.core.HVContext;
import pl.pwr.hiervis.core.MeasureComputeThread;
import pl.pwr.hiervis.ui.components.FileDrop;
import pl.pwr.hiervis.ui.control.MouseControl;
import pl.pwr.hiervis.ui.control.MouseControl.MouseAction;
import pl.pwr.hiervis.ui.control.MouseControl.TriggerAreaTypes;
import pl.pwr.hiervis.ui.control.NodeSelectionControl;
import pl.pwr.hiervis.ui.control.PanControl;
import pl.pwr.hiervis.ui.control.SubtreeDragControl;
import pl.pwr.hiervis.ui.control.ZoomScrollControl;
import pl.pwr.hiervis.util.SwingUIUtils;
import pl.pwr.hiervis.util.Utils;
import pl.pwr.hiervis.visualisation.HierarchyProcessor;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.controls.Control;
import prefuse.visual.NodeItem;


@SuppressWarnings("serial")
public class VisualizerFrame extends JFrame
{
	private static final Logger log = LogManager.getLogger( VisualizerFrame.class );

	private static final int defaultFrameWidth = 600;
	private static final int defaultFrameHeight = 600;

	private HVContext context;

	private Display hierarchyDisplay;


	public VisualizerFrame( HVContext context )
	{
		super( HierarchyVisualizer.APP_NAME );

		if ( context == null )
			throw new RuntimeException( "Context must not be null!" );

		this.context = context;

		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		setSize( defaultFrameWidth, defaultFrameHeight );

		createMenu();
		createGUI();

		createFileDrop( this, log, "csv", this::loadFile );

		context.hierarchyChanging.addListener( this::onHierarchyChanging );
		context.hierarchyChanged.addListener( this::onHierarchyChanged );
		context.nodeSelectionChanged.addListener( this::onNodeSelectionChanged );
		context.configChanged.addListener( this::onConfigChanged );

		SwingUIUtils.addCloseCallback( this, this::onWindowClosing );
	}

	public void layoutFrames()
	{
		Rectangle r = SwingUIUtils.getEffectiveDisplayArea( null );

		int frameWidth = r.width / 4;

		Dimension dimMainFrame = new Dimension( frameWidth, frameWidth );
		Dimension dimStatsFrame = new Dimension( frameWidth, r.height - frameWidth );
		Dimension dimVisFrame = new Dimension( r.width - frameWidth, r.height );

		this.setSize( dimMainFrame );
		this.setLocation( r.x, r.y );

		context.getStatisticsFrame().setSize( dimStatsFrame );
		context.getStatisticsFrame().setLocation( r.x, r.y + frameWidth );

		context.getInstanceFrame().setSize( dimVisFrame );
		context.getInstanceFrame().setLocation( r.x + frameWidth, r.y );
	}

	public void showFrames()
	{
		// Restore the frames if they were minimized
		context.getStatisticsFrame().setExtendedState( JFrame.NORMAL );
		context.getInstanceFrame().setExtendedState( JFrame.NORMAL );

		context.getStatisticsFrame().setVisible( true );
		context.getInstanceFrame().setVisible( true );
	}

	private void createGUI()
	{
		hierarchyDisplay = new Display( HVConstants.EMPTY_VISUALIZATION );

		hierarchyDisplay.setEnabled( false );
		hierarchyDisplay.setHighQuality( true );
		hierarchyDisplay.setBackground( context.getConfig().getBackgroundColor() );

		hierarchyDisplay.addControlListener(
			new NodeSelectionControl(
				context::getTree, context::getSelectedRow, context::setSelectedRow
			)
		);
		hierarchyDisplay.addControlListener( new SubtreeDragControl( Control.RIGHT_MOUSE_BUTTON ) );
		hierarchyDisplay.addControlListener( new PanControl( new Class[] { NodeItem.class } ) );
		hierarchyDisplay.addControlListener( new ZoomScrollControl() );

		MouseControl mouseControl = new MouseControl();

		mouseControl.addAction(
			new MouseAction(
				TriggerAreaTypes.VISUAL_ITEM, MouseEvent.BUTTON1, true, ( item, e ) -> {
					if ( item instanceof NodeItem ) {
						context.setSelectedRow( item.getRow() );
					}
				}
			)
		);
		hierarchyDisplay.addControlListener( mouseControl );

		getContentPane().add( hierarchyDisplay );
	}

	/**
	 * Creates a handler for file drag'n'drop.
	 * 
	 * @param c
	 *            the component files can be dragged onto
	 * @param log
	 *            logger for logging of trace messages
	 * @param fileExtension
	 *            file extension that will be accepted for dragging (just the extension, without dot)
	 * @param fileConsumer
	 *            the method to invoke when a correct file is dragged
	 * @return the {@link FileDrop} object handling the drag'n'drop
	 */
	public static FileDrop createFileDrop( Component c, Logger log, String fileExtension, Consumer<File> fileConsumer )
	{
		String fileSuffix = "." + fileExtension.toUpperCase( Locale.ENGLISH );

		return new FileDrop(
			c, new FileDrop.Listener() {
				public void filesDropped( File[] files )
				{
					if ( files.length == 0 ) {
						log.trace( "Drag and drop: recevied no files." );
					}
					else if ( files.length == 1 ) {
						File file = files[0];
						if ( file.getName().toUpperCase( Locale.ENGLISH ).endsWith( fileSuffix ) ) {
							fileConsumer.accept( file );
						}
						else {
							log.trace( "Drag and drop: recevied file is not a " + fileSuffix + " file, ignoring." );
						}
					}
					else {
						log.trace( "Drag and drop: received multiple files, ignoring." );
					}
				}
			}
		);
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
				// Restore the frame if it was minimized
				context.getStatisticsFrame().setExtendedState( JFrame.NORMAL );
				context.getStatisticsFrame().setVisible( true );
			}
		);

		JMenuItem mntmVis = new JMenuItem( "Instance Visualizations" );
		mnView.add( mntmVis );

		mntmVis.addActionListener(
			e -> {
				// Restore the frame if it was minimized
				context.getInstanceFrame().setExtendedState( JFrame.NORMAL );
				context.getInstanceFrame().setVisible( true );
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
			loadFile( fileDialog.getSelectedFile() );
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
		ConfigDialog dialog = new ConfigDialog( context, this );

		// Make the dialog appear at the center of the screen
		dialog.setLocationRelativeTo( null );
		dialog.setVisible( true ); // Blocks until the dialog is dismissed.

		if ( dialog.hasConfigChanged() ) {
			log.trace( "Updating current config..." );
			context.setConfig( dialog.getConfig() );
		}

		log.trace( "Config customization finished." );
	}

	/**
	 * Creates a hierarchy visualization for the currently loaded hierarchy, and lay it out,
	 * so that it is rendered correctly.
	 */
	private void recreateHierarchyVisualization()
	{
		if ( !context.isHierarchyDataLoaded() ) {
			throw new RuntimeException( "No hierarchy data is available." );
		}

		Visualization vis = context.createHierarchyVisualization();
		hierarchyDisplay.setVisualization( vis );
		HierarchyProcessor.layoutVisualization( vis );

		onNodeSelectionChanged( context.getSelectedRow() );
	}

	private void recreateHierarchyVisualizationAsync()
	{
		SwingUIUtils.executeAsyncWithWaitWindow(
			this, "Creating hierarchy visualization...", log, false,
			() -> recreateHierarchyVisualization(),
			() -> {
				hierarchyDisplay.setEnabled( true );
				Utils.fitToBounds( hierarchyDisplay, Visualization.ALL_ITEMS, 0, 0 );
			},
			null
		);
	}

	private void loadFile( File file )
	{
		context.loadFile( this, file );
	}

	// -----------------------------------------------------------------------------------------

	private void onHierarchyChanging( Hierarchy oldHierarchy )
	{
		hierarchyDisplay.setVisualization( HVConstants.EMPTY_VISUALIZATION );
		hierarchyDisplay.setEnabled( false );
		Utils.unzoom( hierarchyDisplay, 0 );
	}

	private void onHierarchyChanged( Hierarchy newHierarchy )
	{
		if ( context.isHierarchyDataLoaded() ) {
			recreateHierarchyVisualizationAsync();
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
	}

	private void onConfigChanged( HVConfig cfg )
	{
		hierarchyDisplay.setBackground( cfg.getBackgroundColor() );

		if ( context.isHierarchyDataLoaded() ) {
			recreateHierarchyVisualizationAsync();
		}
		else {
			// Refresh the hierarchy display so that it reflects node roles correctly
			hierarchyDisplay.damageReport();
			hierarchyDisplay.repaint();
		}
	}

	private void onWindowClosing()
	{
		log.trace( "Closing application..." );

		// Save the current configuration on application exit.
		context.getConfig().to( new File( HVConfig.FILE_PATH ) );

		context.getStatisticsFrame().dispose();
		context.getInstanceFrame().dispose();

		MeasureComputeThread thread = context.getMeasureComputeThread();
		if ( thread != null ) {
			thread.shutdown();
		}
	}
}
