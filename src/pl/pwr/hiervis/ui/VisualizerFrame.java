package pl.pwr.hiervis.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.function.Consumer;

import javax.swing.AbstractButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import basic_hierarchy.interfaces.Node;
import pl.pwr.hiervis.core.HVConfig;
import pl.pwr.hiervis.core.HVConstants;
import pl.pwr.hiervis.core.HVContext;
import pl.pwr.hiervis.hierarchy.HierarchyProcessor;
import pl.pwr.hiervis.hierarchy.LoadedHierarchy;
import pl.pwr.hiervis.prefuse.DisplayEx;
import pl.pwr.hiervis.prefuse.control.CustomToolTipControl;
import pl.pwr.hiervis.prefuse.control.MouseControl;
import pl.pwr.hiervis.prefuse.control.MouseControl.MouseAction;
import pl.pwr.hiervis.prefuse.control.MouseControl.TriggerAreaTypes;
import pl.pwr.hiervis.prefuse.control.NodeSelectionControl;
import pl.pwr.hiervis.prefuse.control.PanControl;
import pl.pwr.hiervis.prefuse.control.SubtreeDragControl;
import pl.pwr.hiervis.prefuse.control.ZoomScrollControl;
import pl.pwr.hiervis.util.Event;
import pl.pwr.hiervis.util.HierarchyUtils;
import pl.pwr.hiervis.util.SwingUIUtils;
import pl.pwr.hiervis.util.Utils;
import pl.pwr.hiervis.util.ui.CloseableTabComponent;
import pl.pwr.hiervis.util.ui.FileDrop;
import pl.pwr.hiervis.util.ui.JFileChooserEx;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.controls.Control;
import prefuse.visual.NodeItem;


@SuppressWarnings("serial")
public class VisualizerFrame extends JFrame implements ActionListener
{
	private static final Logger log = LogManager.getLogger( VisualizerFrame.class );

	/** Sent when a hierarchy tab is closed. */
	public final Event<Integer> hierarchyTabClosed = new Event<>();
	/** Sent when a hierarchy tab is selected. */
	public final Event<Integer> hierarchyTabSelected = new Event<>();

	private static final int defaultFrameWidth = 600;
	private static final int defaultFrameHeight = 600;

	private HVContext context;
	private String subtitle = null;

	private JTabbedPane tabPane;
	private JMenuItem mntmCloseFile;
	private JMenuItem mntmSaveFile;
	private JMenuItem mntmFlatten;


	public VisualizerFrame( HVContext context, String subtitle )
	{
		super( "Hierarchy View" + ( subtitle == null ? "" : ( " [ " + subtitle + " ]" ) ) );
		this.subtitle = subtitle;

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

	public void createHierarchyTab( String tabName )
	{
		Component tabContent = createHierarchyDisplay();
		tabPane.addTab( tabName, null, tabContent, null );
		int index = tabPane.indexOfComponent( tabContent );

		CloseableTabComponent c = new CloseableTabComponent( tabPane );
		c.addCloseListener( this );
		tabPane.setTabComponentAt( index, c );
	}

	public void selectTab( int index )
	{
		tabPane.setSelectedIndex( index );
	}

	public int getSelectedTabIndex()
	{
		return tabPane.getSelectedIndex();
	}

	public void closeTab( int index )
	{
		log.trace( "Closing tab '" + tabPane.getTitleAt( index ) + "'" );
		hierarchyTabClosed.broadcast( index );

		DisplayEx d = (DisplayEx)tabPane.getComponentAt( index );
		HierarchyProcessor.disposeHierarchyVis( d.getVisualization() );
		d.setVisualization( null );
		d.reset();
		d.dispose();

		tabPane.removeTabAt( index );
	}

	public void closeCurrentTab()
	{
		closeTab( tabPane.getSelectedIndex() );
	}

	// -----------------------------------------------------------------------------------------

	private void createGUI()
	{
		tabPane = new JTabbedPane();

		getContentPane().add( tabPane, BorderLayout.CENTER );

		// Reinsert the original mouse listener, so that ours is first in the
		// notification order.
		MouseListener l = tabPane.getMouseListeners()[0];
		tabPane.removeMouseListener( l );
		tabPane.addMouseListener(
			new MouseAdapter() {
				@Override
				public void mousePressed( MouseEvent e )
				{
					if ( SwingUtilities.isMiddleMouseButton( e ) ) {
						int index = tabPane.getUI().tabForCoordinate( tabPane, e.getX(), e.getY() );
						if ( index >= 0 ) {
							closeTab( index );
						}
					}
				}
			}
		);
		tabPane.addMouseListener( l );

		tabPane.addChangeListener(
			e -> {
				int index = tabPane.getSelectedIndex();
				if ( index >= 0 ) {
					hierarchyTabSelected.broadcast( index );
				}
			}
		);
	}

	private DisplayEx createHierarchyDisplay()
	{
		DisplayEx display = new DisplayEx( HVConstants.EMPTY_VISUALIZATION );

		display.setEnabled( false );
		display.setHighQuality( true );
		display.setBackground( context.getConfig().getBackgroundColor() );

		display.addControlListener(
			new NodeSelectionControl(
				() -> context.getHierarchy().getTree(),
				context::getSelectedRow, context::setSelectedRow
			)
		);
		display.addControlListener( new SubtreeDragControl( Control.RIGHT_MOUSE_BUTTON ) );
		display.addControlListener( new PanControl( new Class[] { NodeItem.class } ) );
		display.addControlListener( new ZoomScrollControl() );
		display.addControlListener(
			new CustomToolTipControl(
				item -> {
					if ( item instanceof NodeItem ) {
						StringBuilder buf = new StringBuilder();

						String nodeId = item.getString( HVConstants.PREFUSE_NODE_ID_COLUMN_NAME );
						Node n = HierarchyUtils.findGroup( context.getHierarchy(), nodeId );
						buf.append( "<html>" );
						buf.append( "<b>" ).append( nodeId ).append( "</b><br/>" )
							.append( "Instances in this node: " ).append( n.getNodeInstances().size() ).append( "<br/>" );
						if ( n.getChildren().size() > 0 ) {
							buf.append( "Instances in subtree: " ).append( n.getSubtreeInstances().size() );
						}
						buf.append( "</html>" );

						return buf.toString();
					}

					return null;
				}
			)
		);

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

		mouseControl.addAction(
			new MouseAction(
				TriggerAreaTypes.VISUAL_ITEM, MouseEvent.BUTTON1, 2, ( item, e ) -> {
					if ( item instanceof NodeItem ) {
						Node n = HierarchyUtils.findGroup( context.getHierarchy(), item.getRow() );

						NodeDetailsFrame detailsFrame = new NodeDetailsFrame( context, this, n, subtitle );

						detailsFrame.setVisible( true );
						detailsFrame.setLocationRelativeTo( this );
					}
				}
			)
		);

		display.addControlListener( mouseControl );

		return display;
	}

	private void createMenu()
	{
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar( menuBar );

		createFileMenu( menuBar );
		createEditMenu( menuBar );
		createViewMenu( menuBar );
	}

	private void createFileMenu( JMenuBar menuBar )
	{
		JMenu mnFile = new JMenu( "File" );
		mnFile.setMnemonic( 'F' );
		menuBar.add( mnFile );

		JMenuItem mntmOpenFile = new JMenuItem( "Open file..." );
		mntmOpenFile.setMnemonic( 'O' );
		mntmOpenFile.addActionListener( e -> openFileSelectionDialog() );
		mnFile.add( mntmOpenFile );

		mntmCloseFile = new JMenuItem( "Close current hierarchy" );
		mntmCloseFile.setMnemonic( 'W' );
		mntmCloseFile.addActionListener( e -> closeCurrentTab() );
		mntmCloseFile.setEnabled( false );
		mnFile.add( mntmCloseFile );

		mntmSaveFile = new JMenuItem( "Save current hierarchy..." );
		mntmSaveFile.setMnemonic( 'S' );
		mntmSaveFile.addActionListener( e -> openSaveDialog() );
		mntmSaveFile.setEnabled( false );
		mnFile.add( mntmSaveFile );

		mnFile.add( new JSeparator() );

		JMenuItem mntmConfig = new JMenuItem( "Config" );
		mntmConfig.setMnemonic( 'C' );
		mntmConfig.addActionListener( e -> openConfigDialog() );
		mnFile.add( mntmConfig );
	}

	private void createEditMenu( JMenuBar menuBar )
	{
		JMenu mnEdit = new JMenu( "Edit" );
		mnEdit.setMnemonic( 'E' );
		menuBar.add( mnEdit );

		mntmFlatten = new JMenuItem( "Flatten Hierarchy" );
		mntmFlatten.setMnemonic( 'F' );
		mntmFlatten.addActionListener(
			e -> {
				String tabTitle = "[F] " + tabPane.getTitleAt( tabPane.getSelectedIndex() );
				context.loadHierarchy( tabTitle, HierarchyUtils.flattenHierarchy( context.getHierarchy() ) );
			}
		);
		mntmFlatten.setEnabled( false );
		mnEdit.add( mntmFlatten );
	}

	private void createViewMenu( JMenuBar menuBar )
	{
		JMenu mnView = new JMenu( "View" );
		mnView.setMnemonic( 'V' );
		menuBar.add( mnView );

		JMenuItem mntmStats = new JMenuItem( "Hierarchy Statistics" );
		mntmStats.setMnemonic( 'S' );
		mnView.add( mntmStats );

		mntmStats.addActionListener(
			e -> {
				// Restore the frame if it was minimized
				context.getStatisticsFrame().setExtendedState( JFrame.NORMAL );
				context.getStatisticsFrame().setVisible( true );
			}
		);

		JMenuItem mntmVis = new JMenuItem( "Instance Visualizations" );
		mntmVis.setMnemonic( 'V' );
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
		fileDialog.setDialogTitle( "Select a file to load" );
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
	 * Opens a file selection dialog, allowing the user to select a destination file to save the hierarchy to.
	 */
	private void openSaveDialog()
	{
		JFileChooserEx fileDialog = new JFileChooserEx();
		fileDialog.setCurrentDirectory( new File( "." ) );
		fileDialog.setDialogTitle( "Select destination file" );
		fileDialog.setFileSelectionMode( JFileChooser.FILES_ONLY );
		fileDialog.setAcceptAllFileFilterUsed( true );
		fileDialog.addChoosableFileFilter( new FileNameExtensionFilter( "*.csv", "csv" ) );

		if ( fileDialog.showSaveDialog( this ) == JFileChooser.APPROVE_OPTION ) {
			try {
				LoadedHierarchy lh = context.getHierarchy();

				HierarchyUtils.save(
					fileDialog.getSelectedFile().getAbsolutePath(),
					lh.data,
					true,
					lh.options.hasTrueClassAttribute,
					lh.options.hasTnstanceNameAttribute,
					true
				);
			}
			catch ( IOException e ) {
				log.error( "Error while saving hierarchy: ", e );
				SwingUIUtils.showErrorDialog( "Error occurred while saving the hierarchy:\n\n" + e.getMessage() );
			}
		}
		else {
			log.trace( "Saving aborted." );
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
	private void recreateHierarchyVisualization( Display display )
	{
		if ( !context.isHierarchyDataLoaded() ) {
			throw new RuntimeException( "No hierarchy data is available." );
		}

		Visualization vis = context.createHierarchyVisualization();
		display.setVisualization( vis );
		HierarchyProcessor.layoutVisualization( vis );

		onNodeSelectionChanged( context.getSelectedRow() );
	}

	private void recreateHierarchyVisualizationAsync( Display display )
	{
		SwingUIUtils.executeAsyncWithWaitWindow(
			this, "Creating hierarchy visualization...", log, false,
			() -> recreateHierarchyVisualization( display ),
			() -> {
				display.setEnabled( true );
				Utils.fitToBounds( display, Visualization.ALL_ITEMS, 0, 0 );
			},
			null
		);
	}

	private void loadFile( File file )
	{
		context.loadFile( this, file );
	}

	private Display getCurrentHierarchyDisplay()
	{
		int index = tabPane.getSelectedIndex();
		if ( index >= 0 ) {
			Component c = tabPane.getComponentAt( index );
			return (Display)c;
		}
		return null;
	}

	// -----------------------------------------------------------------------------------------

	private void onHierarchyChanging( LoadedHierarchy oldHierarchy )
	{
		Display d = getCurrentHierarchyDisplay();
		if ( d != null )
			d.reset();

		mntmCloseFile.setEnabled( false );
		mntmSaveFile.setEnabled( false );
		mntmFlatten.setEnabled( false );
	}

	private void onHierarchyChanged( LoadedHierarchy newHierarchy )
	{
		if ( context.isHierarchyDataLoaded() ) {
			int index = context.getHierarchyIndex( newHierarchy );
			if ( index != getSelectedTabIndex() ) {
				tabPane.setSelectedIndex( index );
			}

			Display currentDisplay = getCurrentHierarchyDisplay();
			// Only recreate the visualization if it's the first time we're visiting that tab.
			if ( currentDisplay.getVisualization() == HVConstants.EMPTY_VISUALIZATION ) {
				recreateHierarchyVisualizationAsync( currentDisplay );
			}

			mntmCloseFile.setEnabled( true );
			mntmSaveFile.setEnabled( true );
			mntmFlatten.setEnabled( true );
		}
	}

	private void onNodeSelectionChanged( int row )
	{
		HierarchyProcessor.updateNodeRoles( context, context.getSelectedRow() );

		// Refresh the hierarchy display so that it reflects node roles correctly
		Display currentDisplay = getCurrentHierarchyDisplay();
		currentDisplay.damageReport();
		currentDisplay.repaint();
	}

	private void onConfigChanged( HVConfig cfg )
	{
		Display currentDisplay = getCurrentHierarchyDisplay();

		if ( currentDisplay != null ) {
			currentDisplay.setBackground( cfg.getBackgroundColor() );
			recreateHierarchyVisualizationAsync( currentDisplay );
		}
	}

	private void onWindowClosing()
	{
		log.trace( "Closing application..." );

		// Save the current configuration on application exit.
		context.getConfig().to( new File( HVConfig.FILE_PATH ) );

		context.getStatisticsFrame().dispose();
		context.getInstanceFrame().dispose();
		context.getMeasureManager().dispose();
	}

	@Override
	public void actionPerformed( ActionEvent e )
	{
		Object source = e.getSource();

		if ( source instanceof AbstractButton ) {
			AbstractButton btn = (AbstractButton)source;

			if ( tabPane.isAncestorOf( btn ) ) {
				int index = tabPane.indexOfTabComponent( btn.getParent() );
				if ( index >= 0 ) {
					btn.removeActionListener( this );
					closeTab( index );
				}
			}
		}
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
}
