package pl.pwr.hiervis.visualisation;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import org.apache.commons.lang3.tuple.Pair;

import basic_hierarchy.interfaces.Hierarchy;
import basic_hierarchy.interfaces.Instance;
import basic_hierarchy.interfaces.Node;
import pl.pwr.hiervis.core.ElementRole;
import pl.pwr.hiervis.core.HVConfig;
import pl.pwr.hiervis.core.HVConstants;
import pl.pwr.hiervis.core.HVContext;
import pl.pwr.hiervis.util.Utils;
import prefuse.Constants;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.layout.AxisLabelLayout;
import prefuse.action.layout.AxisLayout;
import prefuse.action.layout.graph.NodeLinkTreeLayout;
import prefuse.data.Schema;
import prefuse.data.Table;
import prefuse.data.Tree;
import prefuse.data.Tuple;
import prefuse.data.expression.AbstractExpression;
import prefuse.data.expression.ComparisonPredicate;
import prefuse.data.expression.Literal;
import prefuse.data.query.NumberRangeModel;
import prefuse.render.AxisRenderer;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.EdgeRenderer;
import prefuse.render.Renderer;
import prefuse.render.RendererFactory;
import prefuse.util.ColorLib;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.VisiblePredicate;


public class HierarchyProcessor
{
	/**
	 * Processes the currently loaded {@link Hierarchy} and creates a {@link Tree} structure
	 * used to visualize {@link Node}s in that hierarchy.
	 * 
	 * @param config
	 *            the application config
	 * @param sourceRoot
	 *            the root node of the hierarchy
	 * @param availableWidth
	 *            the width the layout has to work with
	 * @param availableHeight
	 *            the height the layout has to work with
	 * @return a tuple of the Tree structure representing the hierarchy, and TreeLayoutData
	 *         associated with it, containing information as to how visualize the tree.
	 */
	public static Pair<Tree, TreeLayoutData> buildHierarchyTree(
		HVConfig config, Node sourceRoot,
		int availableWidth, int availableHeight )
	{
		Tree tree = new Tree();
		tree.addColumn( HVConstants.PREFUSE_NODE_ID_COLUMN_NAME, String.class );
		tree.addColumn( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, int.class );

		prefuse.data.Node treeRoot = tree.addRoot();
		treeRoot.setString( HVConstants.PREFUSE_NODE_ID_COLUMN_NAME, sourceRoot.getId() );
		treeRoot.setInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, ElementRole.OTHER.getNumber() );

		// path from node to root
		int maxTreeDepth = 0;
		int maxTreeWidth = 0;
		// TODO: In order to improve performance, it might be better to change this to a LinkedList, because
		// HashMap is only quick once it is already built, but the building process itself could be slow.
		HashMap<Integer, Integer> treeLevelToWidth = new HashMap<>();
		treeLevelToWidth.put( 0, 1 );

		Queue<Map.Entry<prefuse.data.Node, Node>> treeParentToSourceChild = new LinkedList<>();
		for ( Node sourceChild : sourceRoot.getChildren() ) {
			treeParentToSourceChild.add( new AbstractMap.SimpleEntry<prefuse.data.Node, Node>( treeRoot, sourceChild ) );
		}

		while ( !treeParentToSourceChild.isEmpty() ) {
			Entry<prefuse.data.Node, Node> treeParentAndSourceChild = treeParentToSourceChild.remove();
			Node sourceGroup = treeParentAndSourceChild.getValue();

			// Create a new tree node based on the source group
			prefuse.data.Node newNode = tree.addChild( treeParentAndSourceChild.getKey() );
			newNode.setString( HVConstants.PREFUSE_NODE_ID_COLUMN_NAME, sourceGroup.getId() );
			newNode.setInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, ElementRole.OTHER.getNumber() );

			// Compute new max tree depth
			int currentNodeDepth = newNode.getDepth();
			maxTreeDepth = Math.max( maxTreeDepth, currentNodeDepth );

			// Update the number of nodes on this tree level, for later processing
			Integer treeLevelWidth = treeLevelToWidth.get( currentNodeDepth );
			if ( treeLevelWidth == null ) {
				treeLevelToWidth.put( currentNodeDepth, 1 );
			}
			else {
				treeLevelToWidth.put( currentNodeDepth, treeLevelWidth + 1 );
			}

			// Enqueue this group's children for processing
			for ( Node child : sourceGroup.getChildren() ) {
				treeParentToSourceChild.add( new AbstractMap.SimpleEntry<prefuse.data.Node, Node>( newNode, child ) );
			}
		}

		// Tree is complete, now find the max tree width
		maxTreeWidth = Collections.max( treeLevelToWidth.values() );

		TreeLayoutData layoutData = new TreeLayoutData(
			config, tree,
			maxTreeDepth, maxTreeWidth,
			availableWidth, availableHeight
		);

		return Pair.of( tree, layoutData );
	}

	@SuppressWarnings("unchecked")
	public static void updateNodeRoles( HVContext context, int row )
	{
		Tree hierarchyTree = context.getTree();
		HVConfig config = context.getConfig();

		// Reset all nodes back to 'other'
		for ( int i = 0; i < hierarchyTree.getNodeCount(); ++i ) {
			prefuse.data.Node n = hierarchyTree.getNode( i );
			n.setInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, ElementRole.OTHER.getNumber() );
		}

		if ( row < 0 )
			return;

		prefuse.data.Node n = hierarchyTree.getNode( row );

		LinkedList<prefuse.data.Node> stack = new LinkedList<>();
		stack.add( n );
		while ( !stack.isEmpty() ) {
			prefuse.data.Node current = stack.removeFirst();
			current.setInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, ElementRole.CHILD.getNumber() );
			for ( Iterator<prefuse.data.Node> children = current.children(); children.hasNext(); ) {
				prefuse.data.Node child = children.next();
				stack.add( child );
			}
		}

		if ( config.isDisplayAllPoints() && n.getParent() != null ) {
			stack = new LinkedList<>();
			// when the parent is empty, then we need to search up in the hierarchy because empty
			// parents are skipped, but displayed on output images
			prefuse.data.Node directParent = n.getParent();
			stack.add( directParent );
			while ( !stack.isEmpty() ) {
				prefuse.data.Node current = stack.removeFirst();
				current.setInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, ElementRole.INDIRECT_PARENT.getNumber() );
				if ( current.getParent() != null ) {
					stack.add( current.getParent() );
				}
			}

			directParent.setInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, ElementRole.DIRECT_PARENT.getNumber() );
		}

		n.setInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, ElementRole.CURRENT.getNumber() );
	}

	@SuppressWarnings("unchecked")
	public static void updateTreeNodeRoles( HVContext context, String currentGroupId )
	{
		Tree hierarchyTree = context.getTree();
		HVConfig config = context.getConfig();

		if ( context.isHierarchyDataLoaded() ) {
			boolean found = false;

			for ( int i = 0; i < hierarchyTree.getNodeCount(); ++i ) {
				prefuse.data.Node n = hierarchyTree.getNode( i );

				// Reset node role to 'other'
				n.setInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, ElementRole.OTHER.getNumber() );

				if ( !found && n.getString( HVConstants.PREFUSE_NODE_ID_COLUMN_NAME ).equals( currentGroupId ) ) {
					found = true;

					n.setInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, ElementRole.CURRENT.getNumber() );

					// Color child groups
					LinkedList<prefuse.data.Node> stack = new LinkedList<>();
					stack.add( n );

					while ( !stack.isEmpty() ) {
						prefuse.data.Node current = stack.removeFirst();
						current.setInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, ElementRole.CHILD.getNumber() );

						for ( Iterator<prefuse.data.Node> children = current.children(); children.hasNext(); ) {
							prefuse.data.Node child = children.next();
							stack.add( child );
						}
					}

					if ( config.isDisplayAllPoints() && n.getParent() != null ) {
						stack.clear();

						// IF the parent is empty, then we need to search up in the hierarchy because empty
						// parents are skipped, but displayed on output images
						prefuse.data.Node directParent = n.getParent();
						stack.add( directParent );

						while ( !stack.isEmpty() ) {
							prefuse.data.Node current = stack.removeFirst();
							current.setInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, ElementRole.INDIRECT_PARENT.getNumber() );

							if ( current.getParent() != null ) {
								stack.add( current.getParent() );
							}
						}

						directParent.setInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, ElementRole.DIRECT_PARENT.getNumber() );
					}
				}
			}
		}
	}

	public static Visualization createTreeVisualization( HVContext context )
	{
		return createTreeVisualization( context, null );
	}

	public static Visualization createTreeVisualization( HVContext context, String currentGroupId )
	{
		updateTreeNodeRoles( context, currentGroupId );

		Tree hierarchyTree = context.getTree();
		TreeLayoutData layoutData = context.getTreeLayoutData();
		HVConfig config = context.getConfig();

		Visualization vis = new Visualization();

		if ( context.isHierarchyDataLoaded() ) {
			vis.add( HVConstants.HIERARCHY_DATA_NAME, hierarchyTree );

			NodeRenderer r = new NodeRenderer( layoutData.getNodeSize(), config );
			DefaultRendererFactory drf = new DefaultRendererFactory( r );
			EdgeRenderer edgeRenderer = new EdgeRenderer( prefuse.Constants.EDGE_TYPE_LINE );
			drf.setDefaultEdgeRenderer( edgeRenderer );
			vis.setRendererFactory( drf );

			ColorAction edgesColor = new ColorAction(
				HVConstants.HIERARCHY_DATA_NAME + ".edges",
				VisualItem.STROKECOLOR,
				ColorLib.color( Color.lightGray )
			);

			NodeLinkTreeLayout treeLayout = new NodeLinkTreeLayout(
				HVConstants.HIERARCHY_DATA_NAME,
				layoutData.getTreeOrientation(),
				layoutData.getDepthSpace(),
				layoutData.getSiblingSpace(),
				layoutData.getSubtreeSpace()
			);
			treeLayout.setRootNodeOffset( 0 );// 0.5*finalSizeOfNodes);//offset is set in order to show all nodes on images
			treeLayout.setLayoutBounds(
				new Rectangle2D.Double(
					0, 0,
					layoutData.getLayoutWidth(), layoutData.getLayoutHeight()
				)
			);
			ActionList layout = new ActionList();
			layout.add( treeLayout );
			layout.add( new RepaintAction() );

			vis.putAction( HVConstants.HIERARCHY_DATA_NAME + ".edges", edgesColor );
			vis.putAction( HVConstants.HIERARCHY_DATA_NAME + ".layout", layout );
			// TODO we can here implement a heuristic that will check if after enlarging
			// the border lines (rows and columns) of pixels do not contain other values
			// than background colour. If so, then we are expanding one again, otherwise
			// we have appropriate size of image
		}

		return vis;
	}

	public static void layoutVisualization( Visualization vis )
	{
		vis.run( HVConstants.HIERARCHY_DATA_NAME + ".edges" );
		vis.run( HVConstants.HIERARCHY_DATA_NAME + ".layout" );

		Utils.waitUntilActivitiesAreFinished();
	}

	public static Table createInstanceTable( HVContext context )
	{
		String[] dataNames = getFeatureNames( context.getHierarchy() );

		Table table = createEmptyInstanceTable( context, dataNames );
		processInstanceData( context, dataNames.length, table );

		return table;
	}

	/**
	 * If the input file had a first row with column names, then this method returns those names.
	 * If the first row did not contain column names, it creates artificial names ("dimension #")
	 * 
	 * @param hierarchy
	 *            the hierarchy to get the names for
	 * @return array of names for instance features
	 */
	public static String[] getFeatureNames( Hierarchy hierarchy )
	{
		String[] dataNames = hierarchy.getDataNames();

		if ( dataNames == null ) {
			// Input file had no column names -- got to make them up ourselves.
			try {
				Instance instance = hierarchy.getRoot().getSubtreeInstances().get( 0 );
				int dimCount = instance.getData().length;
				dataNames = new String[dimCount];
				for ( int i = 0; i < dimCount; ++i ) {
					dataNames[i] = "dimension " + ( i + 1 );
				}
			}
			catch ( IndexOutOfBoundsException e ) {
				throw new RuntimeException( "Could not get an instance from the hierarchy. Is the hierarchy empty?" );
			}
		}

		return dataNames;
	}

	/**
	 * Creates a new, empty table used to hold processed instance data.
	 * 
	 * @param context
	 *            the application context
	 * @param dataNames
	 *            array of names for instance features
	 * @return the created table
	 */
	private static Table createEmptyInstanceTable( HVContext context, String[] dataNames )
	{
		Table table = new Table();

		for ( int i = 0; i < dataNames.length; ++i ) {
			table.addColumn( dataNames[i], double.class );
		}

		table.addColumn( HVConstants.PREFUSE_INSTANCE_NODE_COLUMN_NAME, prefuse.data.Node.class );
		// table.addColumn( HVConstants.PREFUSE_INSTANCE_VISIBLE_COLUMN_NAME, boolean.class );
		// table.addColumn( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, int.class );
		if ( context.getConfig().hasInstanceNameAttribute() ) {
			table.addColumn( HVConstants.PREFUSE_INSTANCE_LABEL_COLUMN_NAME, String.class );
		}

		return table;
	}

	/**
	 * Processes raw hierarchy data and saves it in the specified table.
	 * 
	 * @param context
	 *            the application context
	 * @param featureCount
	 *            number of instance features in the input file
	 * @param table
	 *            the table the processed data will be saved in.
	 */
	private static void processInstanceData( HVContext context, int featureCount, Table table )
	{
		// TODO: Implement some sort of culling so that we remove overlapping instances?
		// Could use k-d trees maybe?

		HVConfig config = context.getConfig();

		for ( Instance instance : context.getHierarchy().getRoot().getSubtreeInstances() ) {
			int row = table.addRow();

			double[] data = instance.getData();
			for ( int i = 0; i < featureCount; ++i ) {
				table.set( row, i, data[i] );
			}

			prefuse.data.Node node = context.findGroup(
				config.isUseTrueClass()
					? instance.getTrueClass()
					: instance.getNodeId()
			);

			table.set( row, HVConstants.PREFUSE_INSTANCE_NODE_COLUMN_NAME, node );
			// table.set( row, HVConstants.PREFUSE_INSTANCE_VISIBLE_COLUMN_NAME, true );
			// table.set( row, HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, 0 );
			if ( context.getConfig().hasInstanceNameAttribute() ) {
				table.set( row, HVConstants.PREFUSE_INSTANCE_LABEL_COLUMN_NAME, instance.getInstanceName() );
			}
		}
	}

	public static Visualization createInstanceVisualization(
		HVContext context, Node group, int pointSize,
		int dimX, int dimY,
		boolean withLabels )
	{
		HVConfig config = context.getConfig();
		Visualization vis = new Visualization();

		String nameLabelsX = "labelsX";
		String nameLabelsY = "labelsY";

		if ( withLabels ) {
			vis.setRendererFactory(
				new RendererFactory() {
					Renderer rendererAxisX = new AxisRenderer( Constants.CENTER, Constants.FAR_BOTTOM );
					Renderer rendererAxisY = new AxisRenderer( Constants.FAR_LEFT, Constants.CENTER );
					Renderer rendererPoint = new PointRenderer( new Rectangle2D.Double( 0, 0, pointSize, pointSize ) );


					public Renderer getRenderer( VisualItem item )
					{
						if ( item.isInGroup( nameLabelsX ) )
							return rendererAxisX;
						if ( item.isInGroup( nameLabelsY ) )
							return rendererAxisY;
						return rendererPoint;
					}
				}
			);
		}
		else {
			vis.setRendererFactory(
				new DefaultRendererFactory(
					new PointRenderer( new Rectangle2D.Double( 0, 0, pointSize, pointSize ) )
				)
			);
		}

		Table table = context.getInstanceTable();
		vis.addTable( HVConstants.INSTANCE_DATA_NAME, table );

		Node root = context.getHierarchy().getRoot();
		Rectangle2D bounds = Utils.calculateBoundingRectForCluster( root, dimX, dimY );

		AxisLayout axisX = new AxisLayout(
			HVConstants.INSTANCE_DATA_NAME,
			table.getColumnName( dimX ),
			Constants.X_AXIS, VisiblePredicate.TRUE
		);
		axisX.setRangeModel( new NumberRangeModel( bounds.getMinX(), bounds.getMaxX(), bounds.getMinX(), bounds.getMaxX() ) );

		AxisLayout axisY = new AxisLayout(
			HVConstants.INSTANCE_DATA_NAME,
			table.getColumnName( dimY ),
			Constants.Y_AXIS, VisiblePredicate.TRUE
		);
		axisY.setRangeModel( new NumberRangeModel( bounds.getMinY(), bounds.getMaxY(), bounds.getMinY(), bounds.getMaxY() ) );

		ColorAction colorize = new ColorAction( HVConstants.INSTANCE_DATA_NAME, VisualItem.FILLCOLOR );
		colorize.setDefaultColor( Utils.rgba( Color.MAGENTA ) );
		colorize.add( getPredicateFor( ElementRole.CURRENT ), Utils.rgba( config.getCurrentGroupColor() ) );
		colorize.add( getPredicateFor( ElementRole.DIRECT_PARENT ), Utils.rgba( config.getParentGroupColor() ) );
		colorize.add( getPredicateFor( ElementRole.INDIRECT_PARENT ), Utils.rgba( config.getAncestorGroupColor() ) );
		colorize.add( getPredicateFor( ElementRole.CHILD ), Utils.rgba( config.getChildGroupColor() ) );
		colorize.add( getPredicateFor( ElementRole.OTHER ), Utils.rgba( config.getOtherGroupColor() ) );

		ActionList drawActions = new ActionList();
		drawActions.add( axisX );
		drawActions.add( axisY );

		if ( withLabels ) {
			AxisLabelLayout labelX = new AxisLabelLayout( nameLabelsX, axisX );
			labelX.setNumberFormat( NumberFormat.getNumberInstance() );
			labelX.setScale( Constants.LINEAR_SCALE );

			AxisLabelLayout labelY = new AxisLabelLayout( nameLabelsY, axisY );
			labelY.setNumberFormat( NumberFormat.getNumberInstance() );
			labelY.setScale( Constants.LINEAR_SCALE );

			drawActions.add( labelX );
			drawActions.add( labelY );
		}

		drawActions.add( colorize );
		drawActions.add( new RepaintAction() );

		vis.putAction( "draw", drawActions );
		vis.putAction( "repaint", new RepaintAction() );

		return vis;
	}

	/**
	 * @param elementRole
	 *            the {@link ElementRole} to test for
	 * @return creates and returns a predicate which returns true for instances whose node's
	 *         {@link ElementRole} is the same as the one passed in argument.
	 */
	private static ComparisonPredicate getPredicateFor( ElementRole elementRole )
	{
		return new ComparisonPredicate(
			ComparisonPredicate.EQ,
			new InstanceNodeExpression(),
			Literal.getLiteral( elementRole.getNumber() )
		);
	}


	/**
	 * Given a row from the instance data table, extracts the node to which that instance belongs and returns
	 * its {@link ElementRole}.
	 */
	@SuppressWarnings("rawtypes")
	private static class InstanceNodeExpression extends AbstractExpression
	{
		public Class getType( Schema s )
		{
			return int.class;
		}

		public Object get( Tuple t )
		{
			return getInt( t );
		}

		public int getInt( Tuple t )
		{
			prefuse.data.Node node = (prefuse.data.Node)t.get( HVConstants.PREFUSE_INSTANCE_NODE_COLUMN_NAME );
			return node.getInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME );
		}
	}
}
