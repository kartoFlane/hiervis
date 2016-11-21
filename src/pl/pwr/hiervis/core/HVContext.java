package pl.pwr.hiervis.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import basic_hierarchy.interfaces.Group;
import basic_hierarchy.interfaces.Hierarchy;
import internal_measures.statistics.AvgPathLength;
import internal_measures.statistics.Height;
import internal_measures.statistics.NumberOfLeaves;
import internal_measures.statistics.NumberOfNodes;
import pl.pwr.hiervis.util.Event;
import pl.pwr.hiervis.visualisation.HierarchyProcessor;
import pl.pwr.hiervis.visualisation.TreeLayoutData;
import prefuse.Visualization;
import prefuse.data.Tree;


/**
 * A way to pass various application data around, without having to rely on
 * statically-accessible variables and states, or the singleton pattern.
 * 
 * @author Tomasz Bachmiñski
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
	private Hierarchy inputHierarchy = null;
	private Tree hierarchyTree = null;
	private TreeLayoutData hierarchyTreeLayout = null;
	private int selectedRow = 0;
	private Map<String, Object> measureMap = new HashMap<>();

	private MeasureComputeThread computeThread = null;


	public HVContext()
	{
		setConfig( new HVConfig() );

		computeThread = new MeasureComputeThread();
		computeThread.measureComputed.addListener( this::onMeasureComputed );

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

	public void setTree( Tree tree )
	{
		hierarchyTree = tree;
	}

	public Tree getTree()
	{
		return hierarchyTree;
	}

	public void setTreeLayoutData( TreeLayoutData layoutData )
	{
		hierarchyTreeLayout = layoutData;
	}

	public TreeLayoutData getTreeLayoutData()
	{
		return hierarchyTreeLayout;
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
	 * while you are processing the set. resulting in missed entries.
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

	public Visualization createInstanceVisualization( Group group )
	{
		return HierarchyProcessor.createInstanceVisualization( this, group );
	}

	/**
	 * Finds the hierarchy group at the specified row.
	 * 
	 * @param row
	 *            the row in the data table at which the group is located.
	 * @return the group at the specified row, or null if not found.
	 */
	public Group findGroup( int row )
	{
		Hierarchy h = getHierarchy();
		Group group = h.getRoot();

		if ( row == 0 ) {
			return group;
		}

		Queue<Group> stack = new LinkedList<>();
		for ( Group child : group.getChildren() ) {
			stack.add( child );
		}

		int currentRow = 0;
		while ( !stack.isEmpty() ) {
			group = stack.remove();

			++currentRow;
			if ( currentRow == row ) {
				return group;
			}

			for ( Group child : group.getChildren() ) {
				stack.add( child );
			}
		}

		return null;
	}

	private void onHierarchyChanged( Hierarchy h )
	{
		computeThread.clearPendingTasks();
		computeThread.setHierarchy( h );

		measureMap.clear();

		computeThread.postTask( "Average Path Length", new AvgPathLength()::calculate );
		computeThread.postTask( "Height", new Height()::calculate );
		computeThread.postTask( "Number of Leaves", new NumberOfLeaves()::calculate );
		computeThread.postTask( "Number of Nodes", new NumberOfNodes()::calculate );
	}

	private void onMeasureComputed( Pair<String, Object> result )
	{
		synchronized ( measureMap ) {
			measureMap.put( result.getKey(), result.getValue() );
		}
	}
}
