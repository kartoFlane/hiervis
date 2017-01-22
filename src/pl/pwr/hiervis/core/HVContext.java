package pl.pwr.hiervis.core;

import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.math3.util.CombinatoricsUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import basic_hierarchy.interfaces.Hierarchy;
import basic_hierarchy.interfaces.Node;
import internal_measures.statistics.AvgWithStdev;
import pl.pwr.hiervis.util.Event;
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
	/** The raw hierarchy data, as it was loaded from the file. */
	private Hierarchy inputHierarchy = null;
	/** Tree structure representing relationships between groups (nodes) in the hierarchy */
	private Tree hierarchyTree = null;
	/** Helper layout data for drawing the tree. */
	private TreeLayoutData hierarchyTreeLayout = null;
	/** Table containing processed instance data */
	private Table instanceTable = null;
	private int selectedRow = 0;
	private Map<String, Object> measureMap = new HashMap<>();

	private MeasureComputeThread computeThread = null;


	public HVContext()
	{
		setConfig( new HVConfig() );

		computeThread = new MeasureComputeThread();
		computeThread.measureComputed.addListener( this::onMeasureComputed );

		hierarchyChanging.addListener( this::onHierarchyChanging );
		hierarchyChanged.addListener( this::onHierarchyChanged );

		computeThread.start();
	}

	/**
	 * @return true if there is hierarchy data available (ie. has been loaded),
	 *         false otherwise.
	 */
	public boolean isHierarchyDataLoaded()
	{
		return inputHierarchy != null;
	}

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

	public void setHierarchy( Hierarchy hierarchy )
	{
		if ( this.inputHierarchy != hierarchy ) {
			hierarchyChanging.broadcast( this.inputHierarchy );
			this.inputHierarchy = hierarchy;
			hierarchyChanged.broadcast( hierarchy );
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

	/**
	 * Returns a set of measures that have been computed thus far for the currently loaded hierarchy.
	 * <p>
	 * This method is not particularly thread-safe, as the map of measures might be updated with new entries
	 * while you are processing the set, resulting in missed entries.
	 * </p>
	 * <p>
	 * For a thread-safe alternative, see {@link #forComputedMeasures(Consumer)}
	 * </p>
	 * 
	 * @see #forComputedMeasures(Consumer)
	 */
	public Set<Map.Entry<String, Object>> getComputedMeasures()
	{
		synchronized ( measureMap ) {
			return Collections.unmodifiableMap( measureMap ).entrySet();
		}
	}

	/**
	 * @param identifier
	 *            identifier of the task to look for
	 * @return true if the task is already computed, false otherwise
	 */
	public boolean isMeasureComputed( String identifier )
	{
		synchronized ( measureMap ) {
			return measureMap.containsKey( identifier );
		}
	}

	/**
	 * Performs the specified function on the set of measures that have been computed thus far for
	 * the currently loaded hierarchy.
	 * <p>
	 * This method executes the function inside of a synchronized block, preventing the set from
	 * being updated while this method is executing.
	 * </p>
	 */
	public void forComputedMeasures( Consumer<Set<Map.Entry<String, Object>>> function )
	{
		synchronized ( measureMap ) {
			function.accept( Collections.unmodifiableMap( measureMap ).entrySet() );
		}
	}

	public MeasureComputeThread getMeasureComputeThread()
	{
		return computeThread;
	}

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
		Hierarchy h = getHierarchy();
		Node group = h.getRoot();

		if ( row == 0 ) {
			return group;
		}

		Queue<Node> stack = new LinkedList<>();
		for ( Node child : group.getChildren() ) {
			stack.add( child );
		}

		int currentRow = 0;
		while ( !stack.isEmpty() ) {
			group = stack.remove();

			++currentRow;
			if ( currentRow == row ) {
				return group;
			}

			for ( Node child : group.getChildren() ) {
				stack.add( child );
			}
		}

		return null;
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
		// TODO:
		// Can potentially speed this up by using a lookup cache in the form of a hash map.
		// Not sure if worth it, though.
		int nodeCount = hierarchyTree.getNodeCount();
		for ( int i = 0; i < nodeCount; ++i ) {
			prefuse.data.Node n = hierarchyTree.getNode( i );
			if ( n.getString( HVConstants.PREFUSE_NODE_ID_COLUMN_NAME ).equals( name ) ) {
				return n;
			}
		}

		return null;
	}

	private void onHierarchyChanging( Hierarchy h )
	{
		selectedRow = 0;
	}

	private void onHierarchyChanged( Hierarchy h )
	{
		int dataDimCount = h.getRoot().getSubtreeInstances().get( 0 ).getData().length;
		log.trace(
			String.format(
				"Loaded hierarchy with %s data dimensions. Visualizations needed: %s",
				dataDimCount, CombinatoricsUtils.binomialCoefficient( dataDimCount, 2 )
			)
		);

		// TODO:
		// Might want to use some kind of algorithm to figure out optimal tree layout area?
		// 1024x1024 seems to work well enough for now.
		Pair<Tree, TreeLayoutData> treeData = HierarchyProcessor.buildHierarchyTree(
			config, h.getRoot(),
			1024, 1024
		);
		hierarchyTree = treeData.getLeft();
		hierarchyTreeLayout = treeData.getRight();

		instanceTable = HierarchyProcessor.createInstanceTable( this );

		computeThread.clearPendingTasks();
		computeThread.setHierarchy( h );

		measureMap.clear();

		computeThread.postTask( MeasureTask.averagePathLength );
		computeThread.postTask( MeasureTask.height );
		computeThread.postTask( MeasureTask.numberOfLeaves );
		computeThread.postTask( MeasureTask.numberOfNodes );
	}

	private void onMeasureComputed( Pair<String, Object> result )
	{
		synchronized ( measureMap ) {
			measureMap.put( result.getKey(), result.getValue() );
		}
	}

	public void dumpMeasures( String destinationFile )
	{
		StringBuilder buf = new StringBuilder();

		buf.append( "Use subtree for internal measures?;" )
			.append( MeasureTask.numberOfNodes.identifier ).append( ";stdev;" )
			.append( MeasureTask.numberOfLeaves.identifier ).append( ";stdev;" )
			.append( MeasureTask.height.identifier ).append( ";stdev;" )
			.append( MeasureTask.averagePathLength.identifier ).append( ";stdev;" )
			.append( MeasureTask.varianceDeviation.identifier ).append( ";stdev;" )
			.append( MeasureTask.varianceDeviation2.identifier ).append( ";stdev;" )
			.append( MeasureTask.flatWithinBetween.identifier ).append( ";stdev;" )
			.append( MeasureTask.flatDunn1.identifier ).append( ";stdev;" )
			.append( MeasureTask.flatDunn2.identifier ).append( ";stdev;" )
			.append( MeasureTask.flatDunn3.identifier ).append( ";stdev;" )
			.append( MeasureTask.flatDunn4.identifier ).append( ";stdev;" )
			.append( MeasureTask.flatDaviesBouldin.identifier ).append( ";stdev;" )
			.append( MeasureTask.flatCalinskiHarabasz.identifier ).append( ";stdev;" )
			.append( '\n' );

		final Function<Object, String> dumpData = data -> {
			if ( data instanceof Double || data instanceof Integer )
				return Objects.toString( data ) + ";0.0;";
			else if ( data instanceof AvgWithStdev ) {
				AvgWithStdev avg = (AvgWithStdev)data;
				return avg.getAvg() + ";" + avg.getStdev() + ";";
			}
			throw new IllegalArgumentException( data.getClass().getName() );
		};

		buf.append( config.isUseSubtree() ).append( ';' )
			.append( dumpData.apply( measureMap.getOrDefault( MeasureTask.numberOfNodes.identifier, 0 ) ) )
			.append( dumpData.apply( measureMap.getOrDefault( MeasureTask.numberOfLeaves.identifier, 0 ) ) )
			.append( dumpData.apply( measureMap.getOrDefault( MeasureTask.height.identifier, 0 ) ) )
			.append( dumpData.apply( measureMap.getOrDefault( MeasureTask.averagePathLength.identifier, 0 ) ) )
			.append( dumpData.apply( measureMap.getOrDefault( MeasureTask.varianceDeviation.identifier, 0 ) ) )
			.append( dumpData.apply( measureMap.getOrDefault( MeasureTask.varianceDeviation2.identifier, 0 ) ) )
			.append( dumpData.apply( measureMap.getOrDefault( MeasureTask.flatWithinBetween.identifier, 0 ) ) )
			.append( dumpData.apply( measureMap.getOrDefault( MeasureTask.flatDunn1.identifier, 0 ) ) )
			.append( dumpData.apply( measureMap.getOrDefault( MeasureTask.flatDunn2.identifier, 0 ) ) )
			.append( dumpData.apply( measureMap.getOrDefault( MeasureTask.flatDunn3.identifier, 0 ) ) )
			.append( dumpData.apply( measureMap.getOrDefault( MeasureTask.flatDunn4.identifier, 0 ) ) )
			.append( dumpData.apply( measureMap.getOrDefault( MeasureTask.flatDaviesBouldin.identifier, 0 ) ) )
			.append( dumpData.apply( measureMap.getOrDefault( MeasureTask.flatCalinskiHarabasz.identifier, 0 ) ) )
			.append( '\n' );

		buf.append( '\n' );

		final Function<MeasureTask, String> dumpHistogram = task -> {
			StringBuilder buf2 = new StringBuilder();
			double[] data = (double[])measureMap.getOrDefault( task.identifier, new double[0] );

			if ( data.length > 0 ) {
				buf2.append( task.identifier ).append( '\n' );
				for ( int i = 0; i < data.length; ++i )
					buf2.append( i ).append( ';' );
				buf2.append( '\n' );
				for ( int i = 0; i < data.length; ++i )
					buf2.append( data[i] ).append( ';' );
				buf2.append( '\n' );
				for ( int i = 0; i < data.length; ++i )
					buf2.append( "0.0;" );
				buf2.append( "\n\n" );
			}

			return buf2.toString();
		};

		buf.append( dumpHistogram.apply( MeasureTask.nodesPerLevel ) );
		buf.append( dumpHistogram.apply( MeasureTask.leavesPerLevel ) );
		buf.append( dumpHistogram.apply( MeasureTask.instancesPerLevel ) );
		buf.append( dumpHistogram.apply( MeasureTask.childrenPerNodePerLevel ) );
		buf.append( dumpHistogram.apply( MeasureTask.numberOfChildren ) );

		try ( FileWriter writer = new FileWriter( destinationFile ) ) {
			writer.write( buf.toString() );
		}
		catch ( IOException ex ) {
			ex.printStackTrace();
		}
	}
}
