package pl.pwr.hiervis.hierarchy;

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
import pl.pwr.hiervis.core.HVConfig;
import pl.pwr.hiervis.core.HVConstants;
import pl.pwr.hiervis.core.HVContext;
import pl.pwr.hiervis.prefuse.TableEx;
import pl.pwr.hiervis.prefuse.visualization.NodeRenderer;
import pl.pwr.hiervis.prefuse.visualization.PointRenderer;
import pl.pwr.hiervis.prefuse.visualization.TreeLayoutData;
import pl.pwr.hiervis.util.Utils;
import prefuse.Constants;
import prefuse.Visualization;
import prefuse.action.Action;
import prefuse.action.ActionList;
import prefuse.action.CompositeAction;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.assignment.StrokeAction;
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
import prefuse.util.StrokeLib;
import prefuse.util.ui.ValuedRangeModel;
import prefuse.visual.VisualItem;
import prefuse.visual.VisualTable;


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
		Node sourceRoot, int availableWidth, int availableHeight )
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
			tree,
			maxTreeDepth, maxTreeWidth,
			availableWidth, availableHeight
		);

		return Pair.of( tree, layoutData );
	}

	@SuppressWarnings("unchecked")
	public static void updateNodeRoles( HVContext context, int row )
	{
		Tree hierarchyTree = context.getHierarchy().getTree();

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

		if ( n.getParent() != null ) {
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
		Tree hierarchyTree = context.getHierarchy().getTree();

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

					if ( n.getParent() != null ) {
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

		Tree hierarchyTree = context.getHierarchy().getTree();
		TreeLayoutData layoutData = context.getHierarchy().getTreeLayoutData();
		HVConfig config = context.getConfig();

		Visualization vis = new Visualization();

		if ( context.isHierarchyDataLoaded() ) {
			final float strokeWidth = 3;
			vis.add( HVConstants.HIERARCHY_DATA_NAME, hierarchyTree );

			NodeRenderer r = new NodeRenderer( layoutData.getNodeSize(), config );
			DefaultRendererFactory drf = new DefaultRendererFactory( r );
			EdgeRenderer edgeRenderer = new EdgeRenderer( prefuse.Constants.EDGE_TYPE_LINE );
			edgeRenderer.setDefaultLineWidth( strokeWidth );
			drf.setDefaultEdgeRenderer( edgeRenderer );
			vis.setRendererFactory( drf );

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

			ColorAction edgesColor = new ColorAction(
				HVConstants.HIERARCHY_DATA_NAME + ".edges",
				VisualItem.STROKECOLOR,
				ColorLib.color( Color.lightGray )
			);

			ColorAction nodeBorderColor = new ColorAction(
				HVConstants.HIERARCHY_DATA_NAME + ".nodes",
				VisualItem.STROKECOLOR,
				ColorLib.color( Color.lightGray )
			);

			StrokeAction nodeBorderStroke = new StrokeAction(
				HVConstants.HIERARCHY_DATA_NAME + ".nodes",
				StrokeLib.getStroke( strokeWidth )
			);

			ActionList designList = new ActionList();
			designList.add( edgesColor );
			designList.add( nodeBorderColor );
			designList.add( nodeBorderStroke );

			ActionList layout = new ActionList();
			layout.add( treeLayout );
			layout.add( new RepaintAction() );

			vis.putAction( "design", designList );
			vis.putAction( "layout", layout );
			// TODO we can here implement a heuristic that will check if after enlarging
			// the border lines (rows and columns) of pixels do not contain other values
			// than background colour. If so, then we are expanding one again, otherwise
			// we have appropriate size of image
		}

		return vis;
	}

	/**
	 * Lays out the specified hierarchy visualization.
	 * 
	 * @param vis
	 *            the hierarchy visualization to lay out.
	 */
	public static void layoutVisualization( Visualization vis )
	{
		Utils.waitUntilActivitiesAreFinished();

		vis.run( "design" );
		vis.run( "layout" );

		Utils.waitUntilActivitiesAreFinished();
	}

	public static TableEx createInstanceTable( HVConfig config, LoadedHierarchy hierarchy, Tree hierarchyTree )
	{
		String[] dataNames = getFeatureNames( hierarchy );

		TableEx table = createEmptyInstanceTable( hierarchy.options, dataNames );
		processInstanceData( config, hierarchy, hierarchyTree, table );

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
	public static String[] getFeatureNames( LoadedHierarchy hierarchy )
	{
		String[] dataNames = hierarchy.data.getDataNames();

		if ( dataNames == null ) {
			// Input file had no column names -- got to make them up ourselves.
			try {
				Instance instance = hierarchy.data.getRoot().getSubtreeInstances().get( 0 );
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
	 * @param config
	 *            the application config
	 * @param dataNames
	 *            array of names for instance features
	 * @return the created table
	 */
	private static TableEx createEmptyInstanceTable( LoadedHierarchy.Options options, String[] dataNames )
	{
		TableEx table = new TableEx();

		for ( int i = 0; i < dataNames.length; ++i ) {
			table.addColumn( dataNames[i], double.class );
		}

		table.addColumn( HVConstants.PREFUSE_INSTANCE_NODE_COLUMN_NAME, prefuse.data.Node.class );
		if ( options.hasTrueClassAttribute ) {
			// Can't put a reference to the Node, because ground truth nodes might not exist.
			// (particularly for flattened hierarchies with true class attribute)
			table.addColumn( HVConstants.PREFUSE_INSTANCE_TRUENODE_ID_COLUMN_NAME, String.class );
		}

		if ( options.hasTnstanceNameAttribute ) {
			table.addColumn( HVConstants.PREFUSE_INSTANCE_LABEL_COLUMN_NAME, String.class );
		}

		return table;
	}

	/**
	 * Processes raw hierarchy data and saves it in the specified table.
	 * 
	 * @param config
	 *            the application config
	 * @param hierarchy
	 *            the hierarchy to process
	 * @param hierarchyTree
	 *            the processed hierarchy tree
	 * @param table
	 *            the table the processed data will be saved in.
	 */
	private static void processInstanceData(
		HVConfig config,
		LoadedHierarchy hierarchy,
		Tree hierarchyTree, Table table )
	{
		// TODO: Implement some sort of culling so that we remove overlapping instances?
		// Could use k-d trees maybe?

		for ( Instance instance : hierarchy.data.getRoot().getSubtreeInstances() ) {
			int row = table.addRow();

			double[] data = instance.getData();
			for ( int i = 0; i < data.length; ++i ) {
				table.set( row, i, data[i] );
			}

			prefuse.data.Node node = findGroup( hierarchyTree, instance.getNodeId() );
			table.set( row, HVConstants.PREFUSE_INSTANCE_NODE_COLUMN_NAME, node );

			if ( hierarchy.options.hasTrueClassAttribute ) {
				table.set( row, HVConstants.PREFUSE_INSTANCE_TRUENODE_ID_COLUMN_NAME, instance.getTrueClass() );
			}

			if ( hierarchy.options.hasTnstanceNameAttribute ) {
				table.set( row, HVConstants.PREFUSE_INSTANCE_LABEL_COLUMN_NAME, instance.getInstanceName() );
			}
		}
	}

	public static prefuse.data.Node findGroup( Tree hierarchyTree, String name )
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

	public static Visualization createInstanceVisualization(
		HVContext context, int pointSize,
		int dimX, int dimY,
		boolean withLabels )
	{
		HVConfig config = context.getConfig();
		Visualization vis = new Visualization();

		String nameLabelsX = HVConstants.PREFUSE_INSTANCE_AXIS_X_COLUMN_NAME;
		String nameLabelsY = HVConstants.PREFUSE_INSTANCE_AXIS_Y_COLUMN_NAME;

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

		Table table = context.getHierarchy().getInstanceTable();
		vis.addTable( HVConstants.INSTANCE_DATA_NAME, table );

		Node root = context.getHierarchy().data.getRoot();
		Rectangle2D bounds = Utils.calculateBoundingRectForCluster( root, dimX, dimY );

		AxisLayout axisX = new AxisLayout(
			HVConstants.INSTANCE_DATA_NAME,
			table.getColumnName( dimX ),
			Constants.X_AXIS
		);
		ValuedRangeModel rangeModelX = new NumberRangeModel( bounds.getMinX(), bounds.getMaxX(), bounds.getMinX(), bounds.getMaxX() );
		axisX.setRangeModel( rangeModelX );

		AxisLayout axisY = new AxisLayout(
			HVConstants.INSTANCE_DATA_NAME,
			table.getColumnName( dimY ),
			Constants.Y_AXIS
		);
		ValuedRangeModel rangeModelY = new NumberRangeModel( bounds.getMinY(), bounds.getMaxY(), bounds.getMinY(), bounds.getMaxY() );
		axisY.setRangeModel( rangeModelY );

		ColorAction colorize = new ColorAction( HVConstants.INSTANCE_DATA_NAME, VisualItem.FILLCOLOR );
		colorize.setDefaultColor( Utils.rgba( Color.MAGENTA ) );
		colorize.add( getPredicateFor( ElementRole.CURRENT ), Utils.rgba( config.getCurrentGroupColor() ) );
		colorize.add( getPredicateFor( ElementRole.DIRECT_PARENT ), Utils.rgba( config.getParentGroupColor() ) );
		colorize.add( getPredicateFor( ElementRole.INDIRECT_PARENT ), Utils.rgba( config.getAncestorGroupColor() ) );
		colorize.add( getPredicateFor( ElementRole.CHILD ), Utils.rgba( config.getChildGroupColor() ) );
		colorize.add( getPredicateFor( ElementRole.OTHER ), Utils.rgba( config.getOtherGroupColor() ) );

		ActionList axisActions = new ActionList();
		axisActions.add( axisX );
		axisActions.add( axisY );

		if ( withLabels ) {
			AxisLabelLayout labelX = new AxisLabelLayout( nameLabelsX, axisX );
			labelX.setNumberFormat( NumberFormat.getNumberInstance() );
			labelX.setRangeModel( rangeModelX );
			labelX.setScale( Constants.LINEAR_SCALE );

			AxisLabelLayout labelY = new AxisLabelLayout( nameLabelsY, axisY );
			labelY.setNumberFormat( NumberFormat.getNumberInstance() );
			labelY.setRangeModel( rangeModelY );
			labelY.setScale( Constants.LINEAR_SCALE );

			axisActions.add( labelX );
			axisActions.add( labelY );
		}

		ActionList drawActions = new ActionList();
		drawActions.add( axisActions );
		drawActions.add( colorize );
		drawActions.add( new RepaintAction() );

		vis.putAction( "draw", drawActions );
		vis.putAction( "axis", axisActions );
		vis.putAction( "repaint", new RepaintAction() );

		return vis;
	}

	/**
	 * Updates the layout bounds of the specified instance visualization to the specified bounds,
	 * allowing the visualization to be rendered at a higher/lower resolution.
	 * 
	 * @param instanceVis
	 *            the instance visualization that will have its layout bounds changed
	 * @param newLayoutBounds
	 *            the new layout bounds
	 */
	public static void updateLayoutBounds( Visualization instanceVis, Rectangle2D newLayoutBounds )
	{
		ActionList axisList = (ActionList)instanceVis.getAction( "axis" );

		AxisLayout axisX = (AxisLayout)axisList.get( 0 );
		AxisLayout axisY = (AxisLayout)axisList.get( 1 );

		axisX.setLayoutBounds( newLayoutBounds );
		axisY.setLayoutBounds( newLayoutBounds );

		if ( axisList.size() > 2 ) {
			// Has labels as well.
			AxisLabelLayout labelX = (AxisLabelLayout)axisList.get( 2 );
			AxisLabelLayout labelY = (AxisLabelLayout)axisList.get( 3 );

			labelX.setLayoutBounds( newLayoutBounds );
			labelY.setLayoutBounds( newLayoutBounds );
		}
	}

	/**
	 * Layout bounds is the area in which the visualization is drawn, or in other words,
	 * the resolution at which it is rendered.
	 * 
	 * @param instanceVis
	 *            the instance visualization whose layout bounds are to be returned
	 * @return current layout bounds of the specified instance visualization
	 */
	public static Rectangle2D getLayoutBounds( Visualization instanceVis )
	{
		ActionList axisList = (ActionList)instanceVis.getAction( "axis" );
		AxisLayout axisX = (AxisLayout)axisList.get( 0 );
		return axisX.getLayoutBounds();
	}

	/**
	 * Disposes the specified visualization assuming it is a hierarchy visualization.
	 * A disposed visualization can no longer be utilized.
	 * 
	 * @param vis
	 *            hierarchy visualization to dispose
	 */
	public static void disposeHierarchyVis( Visualization vis )
	{
		disposeAction( vis.removeAction( "design" ) );
		disposeAction( vis.removeAction( "layout" ) );
		vis.reset();
	}

	/**
	 * Disposes the specified visualization assuming it is an instance visualization.
	 * A disposed visualization can no longer be utilized.
	 * 
	 * @param vis
	 *            instance visualization to dispose
	 */
	public static void disposeInstanceVis( Visualization vis )
	{
		disposeAction( vis.removeAction( "draw" ) );
		disposeAction( vis.removeAction( "axis" ) );
		disposeAction( vis.removeAction( "repaint" ) );

		TableEx.disposeCascadedTable(
			(VisualTable)vis.getVisualGroup( HVConstants.INSTANCE_DATA_NAME )
		);

		vis.reset();
	}

	/**
	 * Disposes the specified visualization assuming it is a histogram visualization.
	 * A disposed visualization can no longer be utilized.
	 * 
	 * @param vis
	 *            histogram visualization to dispose
	 */
	public static void disposeHistogramVis( Visualization vis )
	{
		disposeAction( vis.removeAction( "color" ) );
		disposeAction( vis.removeAction( "draw" ) );
		disposeAction( vis.removeAction( "x" ) );
		disposeAction( vis.removeAction( "xlabels" ) );
		disposeAction( vis.removeAction( "y" ) );
		disposeAction( vis.removeAction( "ylabels" ) );
		vis.reset();
	}

	/**
	 * Disposes the specified action and its member actions recursively (if it is a {@link CompositeAction}).
	 * 
	 * @param action
	 *            the action to dispose
	 */
	public static void disposeAction( Action action )
	{
		if ( action == null )
			return;
		action.cancel();
		action.setEnabled( false );
		action.setVisualization( null );

		if ( action instanceof CompositeAction ) {
			CompositeAction ca = (CompositeAction)action;

			for ( int i = ca.size() - 1; i >= 0; --i ) {
				disposeAction( ca.remove( i ) );
			}
		}
	}

	/**
	 * @param elementRole
	 *            the {@link ElementRole} to test for
	 * @return creates and returns a predicate which returns true for instances whose node's
	 *         {@link ElementRole} is the same as the one passed in argument.
	 */
	public static ComparisonPredicate getPredicateFor( ElementRole elementRole )
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
