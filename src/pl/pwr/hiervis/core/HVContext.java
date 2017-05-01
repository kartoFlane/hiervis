package pl.pwr.hiervis.core;

import java.awt.Dimension;
import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import basic_hierarchy.common.HierarchyBuilder;
import basic_hierarchy.interfaces.Hierarchy;
import pl.pwr.hiervis.measures.MeasureTask;
import pl.pwr.hiervis.ui.FileLoadingOptionsDialog;
import pl.pwr.hiervis.ui.HierarchyStatisticsFrame;
import pl.pwr.hiervis.ui.InstanceVisualizationsFrame;
import pl.pwr.hiervis.ui.OperationProgressFrame;
import pl.pwr.hiervis.ui.VisualizerFrame;
import pl.pwr.hiervis.util.Event;
import pl.pwr.hiervis.util.SwingUIUtils;
import prefuse.Visualization;


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
	public final Event<LoadedHierarchy> hierarchyChanging = new Event<>();
	/** Sent when the loaded hierarchy has changed. */
	public final Event<LoadedHierarchy> hierarchyChanged = new Event<>();

	/** Send when the app configuration is about to change. */
	public final Event<HVConfig> configChanging = new Event<>();
	/** Send when the app configuration has changed. */
	public final Event<HVConfig> configChanged = new Event<>();


	// Members

	private HVConfig config = null;
	private MeasureManager measureManager = null;

	/** The raw hierarchy data, as it was loaded from the file. */
	private LoadedHierarchy currentHierarchy = null;
	private HKPlusPlusWrapper currentHKWrapper = null;
	private int selectedRow = 0;

	private List<LoadedHierarchy> hierarchyList = new ArrayList<>();

	private VisualizerFrame hierarchyFrame;
	private HierarchyStatisticsFrame statsFrame;
	private InstanceVisualizationsFrame visFrame;


	public HVContext()
	{
		setConfig( new HVConfig() );

		measureManager = new MeasureManager();

		hierarchyChanging.addListener( this::onHierarchyChanging );
		hierarchyChanged.addListener( this::onHierarchyChanged );
	}

	public void createGUI( String subtitle )
	{
		if ( hierarchyFrame == null ) {
			hierarchyFrame = new VisualizerFrame( this, subtitle );
			statsFrame = new HierarchyStatisticsFrame( this, hierarchyFrame, subtitle );
			visFrame = new InstanceVisualizationsFrame( this, hierarchyFrame, subtitle );

			hierarchyFrame.hierarchyTabClosed.addListener( this::onHierarchyTabClosed );
			hierarchyFrame.hierarchyTabSelected.addListener( this::onHierarchyTabSelected );
		}
	}

	/**
	 * @return true if there is hierarchy data available (ie. has been loaded),
	 *         false otherwise.
	 */
	public boolean isHierarchyDataLoaded()
	{
		return currentHierarchy != null;
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

	public void setHierarchy( LoadedHierarchy hierarchy )
	{
		if ( this.currentHierarchy != hierarchy ) {
			hierarchyChanging.broadcast( this.currentHierarchy );
			this.currentHierarchy = hierarchy;

			if ( hierarchy != null ) {
				if ( hierarchy.isProcessed() ) {
					hierarchyChanged.broadcast( hierarchy );
				}
				else {
					SwingUIUtils.executeAsyncWithWaitWindow(
						null, "Processing hierarchy data...", log, true,
						() -> hierarchy.processHierarchy( config ),
						() -> hierarchyChanged.broadcast( hierarchy ),
						null
					);
				}
			}
		}
	}

	public LoadedHierarchy getHierarchy()
	{
		return currentHierarchy;
	}

	public int getHierarchyIndex( LoadedHierarchy h )
	{
		return hierarchyList.indexOf( h );
	}

	public LoadedHierarchy.Options getHierarchyOptions()
	{
		return currentHierarchy == null
			? LoadedHierarchy.Options.DEFAULT
			: currentHierarchy.options;
	}

	public void setCurrentHKWrapper( HKPlusPlusWrapper wrapper )
	{
		if ( wrapper == null ) {
			throw new IllegalArgumentException( "Wrapper must not be null." );
		}
		if ( currentHKWrapper != null ) {
			throw new IllegalStateException( "Cannot set current wrapper, because the old one has not been disposed of yet." );
		}

		currentHKWrapper = wrapper;
		currentHKWrapper.subprocessAborted.addListener( this::onHKSubprocessAborted );
		currentHKWrapper.subprocessFinished.addListener( this::onHKSubprocessFinished );
	}

	public HKPlusPlusWrapper getCurrentHKWrapper()
	{
		return currentHKWrapper;
	}

	public boolean isHKSubprocessActive()
	{
		return currentHKWrapper != null;
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

		LoadedHierarchy.Options options = optionsDialog.getOptions();
		if ( options == null ) {
			log.trace( "Loading aborted." );
		}
		else {
			loadFile( window, file, options );
		}
	}

	/**
	 * Same as {@link #loadFile(Window, File)}, except this method allows to specify different
	 * options to use while loading this file.
	 * 
	 * @param window
	 *            a window, used to anchor dialog windows with file loading options / error messages.
	 *            Typically this is the window from which the loading command was issued.
	 * @param file
	 *            the file to load
	 * @param hasInstanceName
	 *            if true, the reader will assume that the file includes a column containing instance names
	 * @param hasTrueClass
	 *            if true, the reader will assume that the file includes a column containing true class
	 * @param hasHeader
	 *            if true, the reader will assume that the first row contains column headers, specifying the name for each column
	 * @param fillBreadth
	 *            if true, the {@link HierarchyBuilder} will attempt to fix the raw hierarchy built from the file.
	 * @param useSubtree
	 *            whether the centroid calculation should also include child groups' instances.
	 */
	public void loadFile(
		Window window, File file,
		boolean hasInstanceName, boolean hasTrueClass, boolean hasHeader, boolean fillBreadth, boolean useSubtree )
	{
		LoadedHierarchy.Options options = new LoadedHierarchy.Options(
			hasInstanceName, hasTrueClass, hasHeader, fillBreadth, useSubtree
		);

		loadFile( window, file, options );
	}

	/**
	 * Same as {@link #loadFile(Window, File)}, except this method allows to specify different
	 * options to use while loading this file.
	 * 
	 * @param window
	 *            a window, used to anchor dialog windows with file loading options / error messages.
	 *            Typically this is the window from which the loading command was issued.
	 * @param file
	 *            the file to load
	 * @param options
	 *            the options to use with the specified file
	 */
	public void loadFile( Window window, File file, LoadedHierarchy.Options options )
	{
		FileLoaderThread thread = new FileLoaderThread( file, options );

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

	/**
	 * Loads the specified hierarchy and creates a new tab for it with the specified name
	 * 
	 * @param tabName
	 *            the name of the tab in the GUI
	 * @param hierarchy
	 *            the hierarchy to load and associate with the tab
	 */
	public void loadHierarchy( String tabName, LoadedHierarchy hierarchy )
	{
		hierarchyList.add( hierarchy );
		hierarchyFrame.createHierarchyTab( tabName );

		setHierarchy( hierarchy );
	}

	// -------------------------------------------------------------------------------------------
	// Listeners

	private void onFileLoaded( Pair<File, LoadedHierarchy> args )
	{
		SwingUtilities.invokeLater(
			() -> {
				File file = args.getLeft();
				LoadedHierarchy loadedHierarchy = args.getRight();
				loadHierarchy( file.getName(), loadedHierarchy );
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

	private void onHierarchyChanging( LoadedHierarchy h )
	{
		selectedRow = 0;
	}

	private void onHierarchyChanged( LoadedHierarchy h )
	{
		// Schedule auto-compute tasks
		for ( MeasureTask task : measureManager.getAllMeasureTasks() ) {
			if ( task.autoCompute && task.applicabilityFunction.apply( h.data )
				&& !h.isMeasureComputed( task.identifier ) ) {
				measureManager.postTask( currentHierarchy, task );
			}
		}
	}

	private void onHierarchyTabSelected( int index )
	{
		setHierarchy( hierarchyList.get( index ) );
	}

	private void onHierarchyTabClosed( int index )
	{
		LoadedHierarchy h = hierarchyList.remove( index );
		h.dispose();

		if ( currentHierarchy == h ) {
			setHierarchy( null );
		}

		System.gc();
	}

	private void onHKSubprocessAborted( HKPlusPlusWrapper wrapper )
	{
		if ( wrapper == currentHKWrapper ) {
			currentHKWrapper = null;
		}
	}

	private void onHKSubprocessFinished( Pair<HKPlusPlusWrapper, Integer> args )
	{
		if ( args.getKey() == currentHKWrapper ) {
			currentHKWrapper = null;
		}
	}
}
