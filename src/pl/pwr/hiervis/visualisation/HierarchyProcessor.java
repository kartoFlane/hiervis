package pl.pwr.hiervis.visualisation;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import org.apache.commons.lang3.tuple.Pair;

import basic_hierarchy.interfaces.Group;
import basic_hierarchy.interfaces.Instance;
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
import prefuse.action.layout.AxisLayout;
import prefuse.action.layout.graph.NodeLinkTreeLayout;
import prefuse.data.Node;
import prefuse.data.Table;
import prefuse.data.Tree;
import prefuse.data.query.NumberRangeModel;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.EdgeRenderer;
import prefuse.util.ColorLib;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.VisiblePredicate;


public class HierarchyProcessor
{
	public static Pair<Tree, TreeLayoutData> buildHierarchyTree( HVConfig config, Group sourceRoot )
	{
		Tree tree = new Tree();
		tree.addColumn( HVConstants.PREFUSE_NODE_ID_COLUMN_NAME, String.class );
		tree.addColumn( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, int.class );

		Node treeRoot = tree.addRoot();
		treeRoot.setString( HVConstants.PREFUSE_NODE_ID_COLUMN_NAME, sourceRoot.getId() );
		treeRoot.setInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, ElementRole.OTHER.getNumber() );

		// path from node to root
		int maxTreeDepth = 0;
		int maxTreeWidth = 0;
		// TODO: In order to improve performance, it might be better to change this to a LinkedList, because
		// HashMap is only quick once it is already built, but the building process itself could be slow.
		HashMap<Integer, Integer> treeLevelToWidth = new HashMap<>();
		treeLevelToWidth.put( 0, 1 );

		Queue<Map.Entry<Node, Group>> treeParentToSourceChild = new LinkedList<>();
		for ( Group sourceChild : sourceRoot.getChildren() ) {
			treeParentToSourceChild.add( new AbstractMap.SimpleEntry<Node, Group>( treeRoot, sourceChild ) );
		}

		while ( !treeParentToSourceChild.isEmpty() ) {
			Entry<Node, Group> treeParentAndSourceChild = treeParentToSourceChild.remove();
			Group sourceGroup = treeParentAndSourceChild.getValue();

			// Create a new tree node based on the source group
			Node newNode = tree.addChild( treeParentAndSourceChild.getKey() );
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
			for ( Group child : sourceGroup.getChildren() ) {
				treeParentToSourceChild.add( new AbstractMap.SimpleEntry<Node, Group>( newNode, child ) );
			}
		}

		// Tree is complete, now find the max tree width
		maxTreeWidth = Collections.max( treeLevelToWidth.values() );

		TreeLayoutData layoutData = new TreeLayoutData( config, tree, maxTreeDepth, maxTreeWidth );

		return Pair.of( tree, layoutData );
	}

	@SuppressWarnings("unchecked")
	public static void updateTreeNodeRoles( HVContext context, String currentGroupId )
	{
		Tree hierarchyTree = context.getTree();
		HVConfig config = context.getConfig();

		if ( context.isHierarchyDataLoaded() ) {
			boolean found = false;

			for ( int i = 0; i < hierarchyTree.getNodeCount(); ++i ) {
				Node n = hierarchyTree.getNode( i );

				// Reset node role to 'other'
				n.setInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, ElementRole.OTHER.getNumber() );

				if ( !found && n.getString( HVConstants.PREFUSE_NODE_ID_COLUMN_NAME ).equals( currentGroupId ) ) {
					found = true;

					n.setInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, ElementRole.CURRENT.getNumber() );

					// Color child groups
					LinkedList<Node> stack = new LinkedList<>();
					stack.add( n );

					while ( !stack.isEmpty() ) {
						Node current = stack.removeFirst();
						current.setInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, ElementRole.CHILD.getNumber() );

						for ( Iterator<Node> children = current.children(); children.hasNext(); ) {
							Node child = children.next();
							stack.add( child );
						}
					}

					if ( config.isDisplayAllPoints() && n.getParent() != null ) {
						stack.clear();

						// IF the parent is empty, then we need to search up in the hierarchy because empty
						// parents are skipped, but displayed on output images
						Node directParent = n.getParent();
						stack.add( directParent );

						while ( !stack.isEmpty() ) {
							Node current = stack.removeFirst();
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

		int hierarchyImageWidth = config.getTreeWidth();
		int hierarchyImageHeight = config.getTreeHeight();

		Visualization vis = new Visualization();

		if ( context.isHierarchyDataLoaded() ) {

			vis.add( HVConstants.NAME_OF_HIERARCHY, hierarchyTree );

			NodeRenderer r = new NodeRenderer( layoutData.getNodeSize(), config );
			DefaultRendererFactory drf = new DefaultRendererFactory( r );
			EdgeRenderer edgeRenderer = new EdgeRenderer( prefuse.Constants.EDGE_TYPE_LINE );
			drf.setDefaultEdgeRenderer( edgeRenderer );
			vis.setRendererFactory( drf );

			ColorAction edgesColor = new ColorAction(
				HVConstants.NAME_OF_HIERARCHY + ".edges",
				VisualItem.STROKECOLOR,
				ColorLib.color( Color.lightGray )
			);

			NodeLinkTreeLayout treeLayout = new NodeLinkTreeLayout(
				HVConstants.NAME_OF_HIERARCHY,
				layoutData.getTreeOrientation(),
				layoutData.getDepthSpace(),
				layoutData.getSiblingSpace(),
				layoutData.getSubtreeSpace()
			);
			treeLayout.setLayoutBounds( new Rectangle2D.Float( 0, 0, hierarchyImageWidth, hierarchyImageHeight ) );
			treeLayout.setRootNodeOffset( 0 );// 0.5*finalSizeOfNodes);//offset is set in order to show all nodes on images
			ActionList layout = new ActionList();
			layout.add( treeLayout );
			layout.add( new RepaintAction() );

			vis.putAction( HVConstants.NAME_OF_HIERARCHY + ".edges", edgesColor );
			vis.putAction( HVConstants.NAME_OF_HIERARCHY + ".layout", layout );
			// TODO we can here implement a heuristic that will check if after enlarging
			// the border lines (rows and columns) of pixels do not contain other values
			// than background colour. If so, then we are expanding one again, otherwise
			// we have appropriate size of image
		}

		return vis;
	}

	public static void layoutVisualization( Visualization vis )
	{
		// TODO: in run function a threads are used, so threads could be somehow used
		// to fill the images more efficiently
		vis.run( HVConstants.NAME_OF_HIERARCHY + ".edges" );
		vis.run( HVConstants.NAME_OF_HIERARCHY + ".layout" );

		Utils.waitUntilActivitiesAreFinished();
	}

	public static Visualization createInstanceVisualization( HVContext context, Group group )
	{
		HVConfig config = context.getConfig();
		int pointImageWidth = config.getInstanceWidth();
		int pointImageHeight = config.getInstanceHeight();

		// TODO: Make this a config property?
		int pointSize = 2;

		Visualization vis = new Visualization();

		vis.setRendererFactory( new DefaultRendererFactory( new PointRenderer( pointSize, config ) ) );

		String datasetName = "data";
		String xField = "x";
		String yField = "y";

		Table table = new Table();
		table.addColumn( xField, double.class );
		table.addColumn( yField, double.class );
		table.addColumn( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, int.class );
		table.addColumn( HVConstants.PREFUSE_NODE_LABEL_COLUMN_NAME, String.class );

		// TODO: Make this a parameter
		int dimX = 0;
		int dimY = 1;

		Group root = context.getHierarchy().getRoot();
		Rectangle2D bounds = Utils.calculateBoundingRectForCluster( root, dimX, dimY );

		for ( Instance i : group.getSubgroupInstances() ) {
			double sourceX = i.getData()[dimX];
			double sourceY = i.getData()[dimY];

			double normalizedX = Utils.normalize(
				sourceX,
				bounds.getMinX(), bounds.getMaxX(),
				0, pointImageWidth
			);
			double normalizedY = Utils.normalize(
				sourceY,
				bounds.getMinY(), bounds.getMaxY(),
				0, pointImageHeight
			);

			int row = table.addRow();
			// NOTE: Prefuse shows (0, 0) in bottom-left corner.
			// Might want to provide the option to invert Y for convenience?
			table.set( row, 0, normalizedX );
			table.set( row, 1, normalizedY );
			table.set( row, HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, 0 );
			table.setString( row, HVConstants.PREFUSE_NODE_LABEL_COLUMN_NAME, i.getInstanceName() );
		}

		vis.addTable( datasetName, table );

		AxisLayout axisX = new AxisLayout(
			datasetName, xField,
			Constants.X_AXIS, VisiblePredicate.TRUE
		);
		axisX.setRangeModel( new NumberRangeModel( 0, pointImageWidth, 0, pointImageWidth ) );
		vis.putAction( "x", axisX );

		AxisLayout axisY = new AxisLayout(
			datasetName, yField,
			Constants.Y_AXIS, VisiblePredicate.TRUE
		);
		axisY.setRangeModel( new NumberRangeModel( 0, pointImageHeight, 0, pointImageHeight ) );
		vis.putAction( "y", axisY );

		ActionList actions = new ActionList();
		actions.add( axisX );
		actions.add( axisY );
		actions.add( new RepaintAction() );

		vis.putAction( "draw", actions );

		return vis;
	}
}
