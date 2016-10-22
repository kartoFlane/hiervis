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

import pl.pwr.basic_hierarchy.interfaces.Instance;
import pl.pwr.basic_hierarchy.interfaces.Node;
import pl.pwr.hiervis.core.ElementRole;
import pl.pwr.hiervis.core.HVConfig;
import pl.pwr.hiervis.core.HVConstants;
import pl.pwr.hiervis.core.HVContext;
import pl.pwr.hiervis.util.Utils;
import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.layout.AxisLayout;
import prefuse.action.layout.graph.NodeLinkTreeLayout;
import prefuse.data.Table;
import prefuse.data.Tree;
import prefuse.render.AbstractShapeRenderer;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.EdgeRenderer;
import prefuse.util.ColorLib;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.VisiblePredicate;


// Previously called 'Visualisation'
public class HierarchyProcessor
{
	private int finalSizeOfNodes;
	private int treeOrientation;

	/**
	 * The spacing to maintain between depth levels of the tree.
	 */
	private double levelGap;

	/**
	 * The spacing to maintain between sibling nodes.
	 */
	private double siblingNodeGap;
	/**
	 * The spacing to maintain between neighboring subtrees.
	 */
	private double subtreeGap;

	private double nodeSizeToBetweenLevelSpaceRatio = 2.0; // minimum value
	private double nodeSizeToBetweenSiblingsSpaceRatio = 4.0; // minimum value


	public Tree createHierarchyTree( Node root, HVConfig config )
	{
		Tree tree = new Tree();
		tree.addColumn( HVConstants.PREFUSE_NODE_ID_COLUMN_NAME, String.class );
		tree.addColumn( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, int.class );
		// tree.addColumn(HVConstants.PREFUSE_NUMBER_OF_INSTANCES_COLUMN_NAME, Integer.class);

		prefuse.data.Node n = tree.addRoot();
		n.set( HVConstants.PREFUSE_NODE_ID_COLUMN_NAME, root.getId() );
		// n.set(HVConstants.PREFUSE_NUMBER_OF_INSTANCES_COLUMN_NAME, root.getNodeInstances().size());
		n.setInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, ElementRole.OTHER.getNumber() );

		int maxTreeDepth = 0; // path from node to root
		int maxTreeWidth = 0;
		HashMap<Integer, Integer> treeLevelWithWidth = new HashMap<>();// FIXME: in case of better performance it would be better to change it to LinkedList
		// because HashMap is quick only when it is built, but the building process could be slow
		treeLevelWithWidth.put( 0, 1 );

		int nodesCounter = 1;

		Queue<Map.Entry<prefuse.data.Node, Node>> stackParentAndChild = new LinkedList<>(); // FIFO
		for ( Node child : root.getChildren() ) {
			stackParentAndChild.add( new AbstractMap.SimpleEntry<prefuse.data.Node, Node>( n, child ) );
		}

		while ( !stackParentAndChild.isEmpty() ) {
			Entry<prefuse.data.Node, Node> sourceNodeWithItsParent = stackParentAndChild.remove();
			Node sourceNode = sourceNodeWithItsParent.getValue();

			n = tree.addChild( sourceNodeWithItsParent.getKey() );
			nodesCounter++;
			n.set( HVConstants.PREFUSE_NODE_ID_COLUMN_NAME, sourceNode.getId() );
			// n.set(HVConstants.PREFUSE_NUMBER_OF_INSTANCES_COLUMN_NAME, sourceNode.getNodeInstances().size());
			n.setInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, ElementRole.OTHER.getNumber() );

			int nodeDepth = n.getDepth();
			if ( nodeDepth > maxTreeDepth ) {
				maxTreeDepth = nodeDepth;
			}

			Integer depthCount = treeLevelWithWidth.get( nodeDepth );
			if ( depthCount == null ) {
				treeLevelWithWidth.put( nodeDepth, 1 );
			}
			else {
				treeLevelWithWidth.put( nodeDepth, depthCount + 1 );
			}

			for ( Node child : sourceNode.getChildren() ) {
				stackParentAndChild.add( new AbstractMap.SimpleEntry<prefuse.data.Node, Node>( n, child ) );
			}
		}
		System.out.println( "Number of nodes: " + nodesCounter );

		maxTreeWidth = Collections.max( treeLevelWithWidth.values() );

		// predict height and width of hierarchy image
		finalSizeOfNodes = 0;
		int widthBasedSizeOfNodes = 0;
		int heightBasedSizeOfNodes = 0;
		treeOrientation = prefuse.Constants.ORIENT_TOP_BOTTOM; // TODO: the orientation of charts could be set automatically depending on the
		// size of hierarchy
		levelGap = 0.0; // the spacing to maintain between depth levels of the tree
		siblingNodeGap = 0.0; // the spacing to maintain between sibling nodes
		subtreeGap = 0.0; // the spacing to maintain between neighboring subtrees
		int hierarchyImageWidth = config.getTreeWidth();
		int hierarchyImageHeight = config.getTreeHeight();

		levelGap = hierarchyImageHeight
			/ (double)( nodeSizeToBetweenLevelSpaceRatio * maxTreeDepth + nodeSizeToBetweenLevelSpaceRatio + maxTreeDepth );
		levelGap = Math.max( 1.0, levelGap );

		// based on above calculation - compute "optimal" size of each node on image
		heightBasedSizeOfNodes = (int)( nodeSizeToBetweenLevelSpaceRatio * levelGap );

		System.out.println( "Between level space: " + levelGap + " node size: " + heightBasedSizeOfNodes );

		siblingNodeGap = ( hierarchyImageWidth ) / ( maxTreeWidth * nodeSizeToBetweenSiblingsSpaceRatio + maxTreeWidth - 1.0 );
		siblingNodeGap = Math.max( 1.0, siblingNodeGap );

		subtreeGap = siblingNodeGap;
		widthBasedSizeOfNodes = (int)( nodeSizeToBetweenSiblingsSpaceRatio * siblingNodeGap );
		System.out.println( "Between siblings space: " + siblingNodeGap + " node size: " + widthBasedSizeOfNodes );

		// below use MAXIMUM height/width
		if ( widthBasedSizeOfNodes < heightBasedSizeOfNodes ) {
			finalSizeOfNodes = widthBasedSizeOfNodes;
			// assume maximum possible size
			levelGap = ( hierarchyImageHeight - maxTreeDepth * finalSizeOfNodes - finalSizeOfNodes ) / (double)maxTreeDepth;
			levelGap = Math.max( 1.0, levelGap );
		}
		else {
			finalSizeOfNodes = heightBasedSizeOfNodes;
			// assume maximum possible size
			siblingNodeGap = ( hierarchyImageWidth - maxTreeWidth * finalSizeOfNodes ) / ( maxTreeWidth - 1.0 );
			siblingNodeGap = Math.max( 1.0, siblingNodeGap );
			subtreeGap = siblingNodeGap;
		}

		return tree;
	}

	public Visualization createTreeVisualization( HVContext context )
	{
		return createTreeVisualization( context, null );
	}

	@SuppressWarnings("unchecked")
	public Visualization createTreeVisualization( HVContext context, String currentNodeId )
	{
		boolean isFound = false;

		Tree hierarchyTree = context.getTree();
		HVConfig config = context.getConfig();

		int hierarchyImageWidth = config.getTreeWidth();
		int hierarchyImageHeight = config.getTreeHeight();

		Visualization vis = new Visualization();

		if ( context.isHierarchyDataLoaded() ) {
			for ( int i = 0; i < hierarchyTree.getNodeCount(); i++ ) {
				prefuse.data.Node n = hierarchyTree.getNode( i );
				n.setInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, ElementRole.OTHER.getNumber() );
			}

			for ( int i = 0; i < hierarchyTree.getNodeCount() && !isFound; i++ ) {
				prefuse.data.Node n = hierarchyTree.getNode( i );
				if ( n.getString( HVConstants.PREFUSE_NODE_ID_COLUMN_NAME ).equals( currentNodeId ) ) {
					isFound = true;
					// colour child groups
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
						prefuse.data.Node directParent = n.getParent();// when the parent is empty, then we need to search up in the hierarchy because empty
						// parents are skipped,but displayed on output images
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
			}

			vis.add( HVConstants.NAME_OF_HIERARCHY, hierarchyTree );

			NodeRenderer r = new NodeRenderer( finalSizeOfNodes, config );
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
				treeOrientation,
				levelGap,
				siblingNodeGap,
				subtreeGap
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

	private Display createTreeDisplay( HVContext context, String currentNodeId )
	{
		Visualization vis = createTreeVisualization( context, currentNodeId );

		HVConfig config = context.getConfig();

		int hierarchyImageWidth = config.getTreeWidth();
		int hierarchyImageHeight = config.getTreeHeight();

		Display display = new Display( vis );
		display.setBackground( config.getBackgroundColor() );
		display.setHighQuality( true );
		display.setSize( (int)( 5.0 * hierarchyImageWidth ), hierarchyImageHeight );

		layoutVisualization( vis );

		return display;
	}

	public Display createTreeDisplay( HVContext context )
	{
		return createTreeDisplay( context, null );
	}

	public Visualization createPointVisualization( HVContext context, Node node )
	{
		HVConfig config = context.getConfig();
		int pointImageWidth = config.getPointWidth();
		int pointImageHeight = config.getPointHeight();

		int pointSize = 2;

		Visualization vis = new Visualization();

		AbstractShapeRenderer asr = new PointRenderer( pointSize, config );
		DefaultRendererFactory drf = new DefaultRendererFactory( asr );
		vis.setRendererFactory( drf );

		String group = "data";
		String xField = "x";
		String yField = "y";

		Table table = new Table();
		table.addColumn( xField, int.class );
		table.addColumn( yField, int.class );
		table.addColumn( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, int.class );

		Node root = context.getHierarchy().getRoot();
		Rectangle2D bounds = Utils.calculateBoundingRectForCluster( root );

		for ( Instance i : node.getSubtreeInstances() ) {
			double x = i.getData()[0];
			double y = i.getData()[1];

			int pointLeftEdge = rectCoordinateOnImage(
				x,
				bounds.getMinX(), bounds.getMaxX(),
				pointImageWidth, pointSize
			);
			int pointTopEdge = rectCoordinateOnImage(
				y,
				bounds.getMinY(), bounds.getMaxY(),
				pointImageHeight, pointSize
			);

			int row = table.addRow();
			table.set( row, 0, pointLeftEdge );
			table.set( row, 1, pointImageHeight - pointTopEdge );
		}

		vis.addTable( group, table );

		AxisLayout x_axis = new AxisLayout(
			group, xField,
			Constants.X_AXIS, VisiblePredicate.TRUE
		);
		vis.putAction( "x", x_axis );

		AxisLayout y_axis = new AxisLayout(
			group, yField,
			Constants.Y_AXIS, VisiblePredicate.TRUE
		);
		vis.putAction( "y", y_axis );

		ActionList actions = new ActionList();
		actions.add( x_axis );
		actions.add( y_axis );
		actions.add( new RepaintAction() );

		vis.putAction( "draw", actions );

		return vis;
	}

	private static int rectCoordinateOnImage( double sourceValue, double min, double max, int dimSize, int pointSize )
	{
		double result = dimSize * ( sourceValue - min ) / ( max - min );
		result -= pointSize / 2.0;
		return (int)result;
	}

	private Display createPointDisplay( HVContext context, Node node )
	{
		if ( node == null )
			node = context.getHierarchy().getRoot();

		HVConfig config = context.getConfig();

		int pointImageWidth = config.getPointWidth();
		int pointImageHeight = config.getPointHeight();

		Visualization vis = createPointVisualization( context, node );

		Display display = new Display( vis );
		display.setBackground( config.getBackgroundColor() );
		display.setHighQuality( true );
		display.setSize( pointImageWidth, pointImageHeight );

		vis.run( "draw" );
		Utils.waitUntilActivitiesAreFinished();

		return display;
	}

	public Display createPointDisplay( HVContext context )
	{
		return createPointDisplay( context, null );
	}
}
