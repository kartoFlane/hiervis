package pl.pwr.hiervis.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.Pair;

import basic_hierarchy.common.HierarchyBuilder;
import basic_hierarchy.interfaces.Hierarchy;
import pl.pwr.hiervis.prefuse.visualization.TreeLayoutData;
import prefuse.data.Table;
import prefuse.data.Tree;


/**
 * Container class used to associate a {@link Hierarchy} instance with various other
 * objects pertaining to that hierarchy, like loading options, and processed data used
 * for visualizations.
 * 
 * @author Tomasz Bachmiński
 *
 */
public class LoadedHierarchy
{
	public final Hierarchy data;
	public final LoadedHierarchy.Options options;

	private final Map<String, Object> computedMeasureMap;

	private Tree hierarchyTree;
	private TreeLayoutData hierarchyTreeLayout;
	private Table instanceTable;


	public LoadedHierarchy( Hierarchy h, LoadedHierarchy.Options o )
	{
		if ( h == null )
			throw new IllegalArgumentException( "Hierarchy must not be null!" );
		if ( o == null )
			throw new IllegalArgumentException( "Options must not be null!" );

		this.data = h;
		this.options = o;

		this.computedMeasureMap = new HashMap<>();
	}

	/**
	 * Processes the specified hierarchy, building hierarchy tree and creating instance table
	 * used in visualizations.
	 * 
	 * @param hierarchy
	 *            the hierarchy to process
	 */
	protected void processHierarchy( HVConfig config )
	{
		// TODO:
		// Might want to use some kind of algorithm to figure out optimal tree layout area?
		// 1024x1024 seems to work well enough for now.
		Pair<Tree, TreeLayoutData> treeData = HierarchyProcessor.buildHierarchyTree(
			data.getRoot(), 2048, 2048
		);
		hierarchyTree = treeData.getLeft();
		hierarchyTreeLayout = treeData.getRight();

		instanceTable = HierarchyProcessor.createInstanceTable(
			config, this, hierarchyTree
		);
	}

	/**
	 * @return true if the hierarchy has been processed, and is ready to be visualized.
	 */
	public boolean isProcessed()
	{
		return hierarchyTree != null && hierarchyTreeLayout != null && instanceTable != null;
	}

	/**
	 * @return tree structure representing relationships between groups (nodes) in the hierarchy
	 */
	public Tree getTree()
	{
		return hierarchyTree;
	}

	/**
	 * @return helper layout data for drawing the tree.
	 */
	public TreeLayoutData getTreeLayoutData()
	{
		return hierarchyTreeLayout;
	}

	/**
	 * @return table containing processed instance data
	 */
	public Table getInstanceTable()
	{
		return instanceTable;
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
		synchronized ( computedMeasureMap ) {
			return Collections.unmodifiableMap( computedMeasureMap ).entrySet();
		}
	}

	/**
	 * @param identifier
	 *            identifier of the task to look for
	 * @return true if the task is already computed, false otherwise
	 */
	public boolean isMeasureComputed( String identifier )
	{
		synchronized ( computedMeasureMap ) {
			return computedMeasureMap.containsKey( identifier );
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
		synchronized ( computedMeasureMap ) {
			function.accept( Collections.unmodifiableMap( computedMeasureMap ).entrySet() );
		}
	}

	public Object getMeasureResultOrDefault( String identifier, Object defaultValue )
	{
		synchronized ( computedMeasureMap ) {
			return computedMeasureMap.getOrDefault( identifier, defaultValue );
		}
	}

	public Object getMeasureResult( String identifier )
	{
		synchronized ( computedMeasureMap ) {
			return computedMeasureMap.get( identifier );
		}
	}

	protected void putMeasureResult( String identifier, Object value )
	{
		synchronized ( computedMeasureMap ) {
			computedMeasureMap.put( identifier, value );
		}
	}

	public void dispose()
	{
		computedMeasureMap.clear();

		hierarchyTree = null;
		hierarchyTreeLayout = null;

		if ( instanceTable != null ) {
			instanceTable.removeAllTableListeners();
			instanceTable.clear();
			instanceTable = null;
		}
	}


	/**
	 * Container class for load options for each {@link Hierarchy} object loaded in the program.
	 * 
	 * @author Tomasz Bachmiński
	 *
	 */
	public static class Options
	{
		public static final Options DEFAULT = new Options( false, false, false, false, false );

		public final boolean hasTnstanceNameAttribute;
		public final boolean hasTrueClassAttribute;
		public final boolean hasColumnHeader;
		public final boolean isFillBreadthGaps;
		public final boolean isUseSubtree;


		/**
		 * @param withInstanceNameAttribute
		 *            if true, the reader will assume that the file includes a column containing instance names
		 * @param withTrueClassAttribute
		 *            if true, the reader will assume that the file includes a column containing true class
		 * @param withHeader
		 *            if true, the reader will assume that the first row contains column headers, specifying the name for each column
		 * @param fillBreadthGaps
		 *            if true, the {@link HierarchyBuilder} will attempt to fix the raw hierarchy built from the file.
		 * @param useSubtree
		 *            whether the centroid calculation should also include child groups' instances.
		 */
		public Options(
			boolean withInstanceNameAttribute,
			boolean withTrueClassAttribute,
			boolean withHeader,
			boolean fillBreadthGaps,
			boolean useSubtree )
		{
			this.hasTnstanceNameAttribute = withInstanceNameAttribute;
			this.hasTrueClassAttribute = withTrueClassAttribute;
			this.hasColumnHeader = withHeader;
			this.isFillBreadthGaps = fillBreadthGaps;
			this.isUseSubtree = useSubtree;
		}
	}
}
