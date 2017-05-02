package pl.pwr.hiervis.hierarchy;

import org.apache.commons.lang3.tuple.Pair;

import basic_hierarchy.common.HierarchyBuilder;
import basic_hierarchy.interfaces.Hierarchy;
import pl.pwr.hiervis.core.HVConfig;
import pl.pwr.hiervis.measures.MeasureResultHolder;
import pl.pwr.hiervis.prefuse.TableEx;
import pl.pwr.hiervis.prefuse.visualization.TreeLayoutData;
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
	public final MeasureResultHolder measureHolder;

	private Tree hierarchyTree;
	private TreeLayoutData hierarchyTreeLayout;
	private TableEx instanceTable;

	private transient int selectedRow = 0;


	public LoadedHierarchy( Hierarchy h, LoadedHierarchy.Options o )
	{
		if ( h == null )
			throw new IllegalArgumentException( "Hierarchy must not be null!" );
		if ( o == null )
			throw new IllegalArgumentException( "Options must not be null!" );

		this.data = h;
		this.options = o;
		this.measureHolder = new MeasureResultHolder();
	}

	/**
	 * Processes the specified hierarchy, building hierarchy tree and creating instance table
	 * used in visualizations.
	 * 
	 * @param hierarchy
	 *            the hierarchy to process
	 */
	public void processHierarchy( HVConfig config )
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
	 * @return tree structure representing relationships between groups (nodes) in the hierarchy.
	 *         Used to create the main hierarchy visualization.
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
	public TableEx getInstanceTable()
	{
		return instanceTable;
	}

	/**
	 * Sets the row of the currently selected node in the {@link #getTree() hierarchy tree}
	 * 
	 * @param row
	 *            the selected row
	 */
	public void setSelectedRow( int row )
	{
		selectedRow = row;
	}

	/**
	 * @return row of the currently selected node in the {@link #getTree() hierarchy tree}
	 */
	public int getSelectedRow()
	{
		return selectedRow;
	}

	public void dispose()
	{
		measureHolder.clear();

		hierarchyTree.dispose();
		hierarchyTree.removeAllSets();
		hierarchyTree.clear();
		hierarchyTree = null;

		hierarchyTreeLayout = null;

		if ( instanceTable != null ) {
			instanceTable.removeAllTableListeners();
			instanceTable.clear();
			instanceTable.dispose();
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
