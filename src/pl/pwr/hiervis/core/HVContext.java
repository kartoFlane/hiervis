package pl.pwr.hiervis.core;

import java.awt.Dimension;
import java.awt.Window;
import java.io.File;

import javax.swing.SwingUtilities;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import basic_hierarchy.interfaces.Hierarchy;
import basic_hierarchy.interfaces.Node;
import pl.pwr.hiervis.measures.MeasureTask;
import pl.pwr.hiervis.ui.FileLoadingOptionsDialog;
import pl.pwr.hiervis.ui.HierarchyStatisticsFrame;
import pl.pwr.hiervis.ui.InstanceVisualizationsFrame;
import pl.pwr.hiervis.ui.OperationProgressFrame;
import pl.pwr.hiervis.ui.VisualizerFrame;
import pl.pwr.hiervis.util.Event;
import pl.pwr.hiervis.util.HierarchyUtils;
import pl.pwr.hiervis.util.SwingUIUtils;
import pl.pwr.hiervis.visualisation.HierarchyProcessor;
import pl.pwr.hiervis.visualisation.TreeLayoutData;
import prefuse.Visualization;
import prefuse.data.Table;
import prefuse.data.Tree;


/**
 * A way to pass various application data around, without having to rely on
 * statically-accessible variables and states, or the singleton pattern.
 * 
 * @author Tomasz Bachmiński
 *
 */
public class HVContext
{
	private static final Logger log = LogManager.getLogger( HVContext.class );

	// Events

	/** Sent when the node (group) selected by the user is about to change. */
	public final Event<Integer> nodeSelectionChanging = new Event<>();
	/** Sent when the node (group) selected by the user has changed. */
	public final Event<Integer> nodeSelectionChanged = new Event<>();
	/** Sent when the loaded hierarchy is about to change. */
	public final Event<Hierarchy> hierarchyChanging = new Event<>();
	/** Sent when the loaded hierarchy has changed. */
	public final Event<Hierarchy> hierarchyChanged = new Event<>();
	/** Send when the app configuration is about to change. */
	public final Event<HVConfig> configChanging = new Event<>();
	/** Send when the app configuration has changed. */
	public final Event<HVConfig> configChanged = new Event<>();


	// Members

	private HVConfig config = null;
	private MeasureManager measureManager = null;

	/** The raw hierarchy data, as it was loaded from the file. */
	private Hierarchy inputHierarchy = null;
	/** Tree structure representing relationships between groups (nodes) in the hierarchy */
	private Tree hierarchyTree = null;
	/** Helper layout data for drawing the tree. */
	private TreeLayoutData hierarchyTreeLayout = null;
	/** Table containing processed instance data */
	private Table instanceTable = null;
	private int selectedRow = 0;

	private VisualizerFrame hierarchyFrame;
	private HierarchyStatisticsFrame statsFrame;
	private InstanceVisualizationsFrame visFrame;


	public HVContext()
	{
		setConfig( new HVConfig() );

		measureManager = new MeasureManager( this );

		hierarchyChanging.addListener( this::onHierarchyChanging );
		hierarchyChanged.addListener( this::onHierarchyChanged );
	}

	public void createGUI( String subtitle )
	{
		if ( hierarchyFrame == null ) {
			hierarchyFrame = new VisualizerFrame( this, subtitle );
			statsFrame = new HierarchyStatisticsFrame( this, hierarchyFrame, subtitle );
			visFrame = new InstanceVisualizationsFrame( this, hierarchyFrame, subtitle );
		}
	}

	/**
	 * @return true if there is hierarchy data available (ie. has been loaded),
	 *         false otherwise.
	 */
	public boolean isHierarchyDataLoaded()
	{
		return inputHierarchy != null;
	}

	// -------------------------------------------------------------------------------------------
	// Getters / setters

	public void setConfig( HVConfig config )
	{
		if ( config == null )
			return;
		if ( this.config == null || !this.config.equals( config ) ) {
			configChanging.broadcast( this.config );
			this.config = config;
			configChanged.broadcast( config );
		}
	}

	public HVConfig getConfig()
	{
		return config;
	}

	public MeasureManager getMeasureManager()
	{
		return measureManager;
	}

	public void setHierarchy( Hierarchy hierarchy )
	{
		if ( this.inputHierarchy != hierarchy ) {
			hierarchyChanging.broadcast( this.inputHierarchy );
			this.inputHierarchy = hierarchy;

			SwingUIUtils.executeAsyncWithWaitWindow(
				null, "Processing hierarchy data...", log, true,
				() -> processHierarchy( hierarchy ),
				() -> hierarchyChanged.broadcast( hierarchy ),
				null
			);
		}
	}

	public Hierarchy getHierarchy()
	{
		return inputHierarchy;
	}

	public Tree getTree()
	{
		return hierarchyTree;
	}

	public TreeLayoutData getTreeLayoutData()
	{
		return hierarchyTreeLayout;
	}

	public Table getInstanceTable()
	{
		return instanceTable;
	}

	/**
	 * @return row of the currently selected node in the hierarchy view.
	 */
	public int getSelectedRow()
	{
		return selectedRow;
	}

	public void setSelectedRow( int row )
	{
		if ( selectedRow != row ) {
			nodeSelectionChanging.broadcast( row );
			selectedRow = row;
			nodeSelectionChanged.broadcast( row );
		}
	}

	public VisualizerFrame getHierarchyFrame()
	{
		return hierarchyFrame;
	}

	public HierarchyStatisticsFrame getStatisticsFrame()
	{
		return statsFrame;
	}

	public InstanceVisualizationsFrame getInstanceFrame()
	{
		return visFrame;
	}

	// -------------------------------------------------------------------------------------------
	// Convenience methods

	public Visualization createHierarchyVisualization()
	{
		return HierarchyProcessor.createTreeVisualization( this );
	}

	/**
	 * Finds the hierarchy group at the specified row.
	 * 
	 * @param row
	 *            the row in the data table at which the group is located.
	 * @return the group at the specified row, or null if not found.
	 */
	public Node findGroup( int row )
	{
		return HierarchyUtils.findGroup( getHierarchy(), row );
	}

	/**
	 * Searches the hierarchy for a group with the specified identifier / name.
	 * 
	 * @param name
	 *            the name to look for.
	 * @return the node / group with the specified name, or null if not found.
	 */
	public prefuse.data.Node findGroup( String name )
	{
		return HierarchyProcessor.findGroup( hierarchyTree, name );
	}

	/**
	 * Loads the specified file as a CSV file describing a {@link Hierarchy} object.
	 * 
	 * @param window
	 *            a window, used to anchor dialog windows with file loading options / error messages.
	 *            Typically this is the window from which the loading command was issued.
	 * @param file
	 *            the file to load
	 */
	public void loadFile( Window window, File file )
	{
		log.trace( String.format( "Selected file: '%s'", file ) );

		FileLoadingOptionsDialog optionsDialog = new FileLoadingOptionsDialog( this, window );
		optionsDialog.setLocationRelativeTo( window );
		optionsDialog.setVisible( true );

		HVConfig cfg = optionsDialog.getConfig();
		if ( cfg == null ) {
			log.trace( "Loading aborted." );
		}
		else {
			loadFile( window, file, cfg );
		}
	}

	public void loadFile(
		Window window, File file,
		boolean hasInstanceName, boolean hasTrueClass, boolean hasHeader, boolean fillBreadth, boolean useSubtree )
	{
		HVConfig cfg = config.copy();

		cfg.setInstanceNameAttribute( hasInstanceName );
		cfg.setTrueClassAttribute( hasTrueClass );
		cfg.setDataNamesRow( hasHeader );
		cfg.setFillBreadthGaps( fillBreadth );
		cfg.setUseSubtree( useSubtree );

		loadFile( window, file, cfg );
	}

	public void loadFile( Window window, File file, HVConfig cfg )
	{
		setConfig( cfg );

		FileLoaderThread thread = new FileLoaderThread( cfg, file );

		OperationProgressFrame progressFrame = new OperationProgressFrame( window, "Loading..." );
		progressFrame.setProgressUpdateCallback( thread::getProgress );
		progressFrame.setStatusUpdateCallback( thread::getStatusMessage );
		progressFrame.setProgressPollInterval( 100 );
		progressFrame.setModal( true );
		progressFrame.setAbortOperation(
			e -> {
				thread.interrupt();
				progressFrame.dispose();
			}
		);

		thread.fileLoaded.addListener( h -> SwingUtilities.invokeLater( () -> progressFrame.dispose() ) );
		thread.errorOcurred.addListener( e -> SwingUtilities.invokeLater( () -> progressFrame.dispose() ) );
		thread.fileLoaded.addListener( this::onFileLoaded );
		thread.errorOcurred.addListener( this::onFileError );

		thread.start();

		progressFrame.setSize( new Dimension( 300, 150 ) );
		progressFrame.setLocationRelativeTo( window );
		progressFrame.setVisible( true );
	}

	// -------------------------------------------------------------------------------------------
	// Listeners

	private void onFileLoaded( Hierarchy loadedHierarchy )
	{
		SwingUtilities.invokeLater(
			() -> {
				log.trace( "Switching hierarchy..." );
				setHierarchy( loadedHierarchy );
			}
		);
	}

	private void onFileError( Exception ex )
	{
		SwingUtilities.invokeLater(
			() -> {
				SwingUIUtils.showInfoDialog(
					"An error ocurred while loading the specified file. Most often this happens when " +
						"incorrect settings were selected for the file in question." +
						"\n\nError message:\n" + ex.getMessage()
				);
			}
		);
	}

	private void onHierarchyChanging( Hierarchy h )
	{
		selectedRow = 0;
	}

	private void onHierarchyChanged( Hierarchy h )
	{
		// Schedule auto-compute tasks
		for ( MeasureTask task : measureManager.getAllMeasureTasks() ) {
			if ( task.autoCompute && task.applicabilityFunction.apply( h ) ) {
				measureManager.postTask( task );
			}
		}
	}

	/**
	 * Processes the specified hierarchy, building hierarchy tree and creating instance table
	 * used in visualizations.
	 * 
	 * @param hierarchy
	 *            the hierarchy to process
	 */
	private void processHierarchy( Hierarchy hierarchy )
	{
		// TODO:
		// Might want to use some kind of algorithm to figure out optimal tree layout area?
		// 1024x1024 seems to work well enough for now.
		Pair<Tree, TreeLayoutData> treeData = HierarchyProcessor.buildHierarchyTree(
			config, hierarchy.getRoot(),
			2048, 2048
		);
		hierarchyTree = treeData.getLeft();
		hierarchyTreeLayout = treeData.getRight();

		instanceTable = HierarchyProcessor.createInstanceTable(
			config, hierarchy, hierarchyTree
		);
	}
}
