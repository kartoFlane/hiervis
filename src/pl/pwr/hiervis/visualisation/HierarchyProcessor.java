package pl.pwr.hiervis.visualisation;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import javax.imageio.ImageIO;

import basic_hierarchy.interfaces.Hierarchy;
import basic_hierarchy.interfaces.Instance;
import basic_hierarchy.interfaces.Node;
import pl.pwr.hiervis.core.ElementRole;
import pl.pwr.hiervis.core.HVConfig;
import pl.pwr.hiervis.core.HVConstants;
import pl.pwr.hiervis.core.HVContext;
import pl.pwr.hiervis.core.HierarchyStatistics;
import pl.pwr.hiervis.util.Utils;
import pl.pwr.hiervis.util.prefuse.histogram.HistogramGraph;
import pl.pwr.hiervis.util.prefuse.histogram.HistogramTable;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.layout.graph.NodeLinkTreeLayout;
import prefuse.data.Table;
import prefuse.data.Tree;
import prefuse.render.DefaultRendererFactory;
import prefuse.render.EdgeRenderer;
import prefuse.util.ColorLib;
import prefuse.visual.VisualItem;


//when the histogram is totally flat (every bar have the same height) then the histogram is not drawn


// Previously called 'Visualisation'
public class HierarchyProcessor {

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

	private double nodeSizeToBetweenLevelSpaceRatio = 2.0;// minimum value
	private double nodeSizeToBetweenSiblingsSpaceRatio = 4.0;// minimum value


	public void process( HVContext context, HierarchyStatistics stats ) {
		Hierarchy input = context.getHierarchy();
		HVConfig config = context.getConfig();

		int nodeImgLeftBorderWidth = 5;
		int nodeImgRightBorderWidth = 30;// this value determine the width of labels on the OY axis
		int nodeImgTopBorderHeight = 5;
		int nodeImgBottomBorderHeight = 30;// this value determine the width of labels on the OX axis
		int nodeImgFinalWidth = (int)( config.getPointWidth() + Math.max( 1.0, config.getPointScallingFactor() / 2 ) );
		int nodeImgFinalHeight = (int)( config.getPointHeight() + Math.max( 1.0, config.getPointScallingFactor() / 2 ) );

		Rectangle2D bounds = calculateBoundingRectForCluster( input.getRoot() );

		LinkedList<Instance> allPoints = null;
		if ( config.isDisplayAllPoints() ) {
			allPoints = input.getRoot().getSubtreeInstances();
		}

		int imgCounter = 0;
		for ( Node n : input.getGroups() ) {
			if ( n.getNodeInstances().size() > 0 ) {
				System.out.println( "==== " + n.getId() + " ====" );
				System.out.println( "Point visualisation..." );
				BufferedImage nodeImg = new BufferedImage( nodeImgFinalWidth, nodeImgFinalHeight, BufferedImage.TYPE_INT_ARGB );

				if ( config.getBackgroundColor() != null )
					nodeImg = setBackgroud( nodeImg, config.getBackgroundColor() );

				nodeImg = fillImage( nodeImg, n, config, bounds, allPoints );
				nodeImg = addBorder( nodeImg, nodeImgLeftBorderWidth, nodeImgRightBorderWidth, nodeImgTopBorderHeight,
						nodeImgBottomBorderHeight, Color.black );

				System.out.println( "Hierary img..." );
				BufferedImage hierarchyImg = createTreeImage( context, n.getId() );

				System.out.println( "Prepare histogram data..." );
				HistogramTable allDataHistogramTable = createAllDataHistogramTable( input.getRoot().getSubtreeInstances(),
						config.getNumberOfHistogramBins() );
				HistogramGraph.setAllDataHistoTable( allDataHistogramTable );
				HistogramTable histogramTable = prepareHistogramTableUsingAllDataBins( n, allDataHistogramTable,
						ElementRole.CURRENT.getNumber() );
				HistogramTable directParentHistogramTable = n.getParent() == null ? null
						: prepareHistogramTableUsingAllDataBins( n.getParent(), allDataHistogramTable,
								ElementRole.DIRECT_PARENT.getNumber() );

				System.out.println( "Horizontal histogram..." );
				BufferedImage horizontalHistogram = createHorizontalHistogram( histogramTable, directParentHistogramTable, config,
						nodeImgFinalWidth,
						nodeImgFinalHeight, nodeImgLeftBorderWidth, nodeImgRightBorderWidth );

				System.out.println( "Vertical histogram..." );
				BufferedImage verticalHistogram = createVerticalHistogram( histogramTable, directParentHistogramTable, config,
						nodeImgFinalWidth,
						nodeImgFinalHeight, nodeImgTopBorderHeight, nodeImgBottomBorderHeight );

				System.out.println( "Statistics..." );
				BufferedImage statsImg = createImageWithStatistics( config, n.getId(), stats, horizontalHistogram.getHeight(),
						verticalHistogram.getWidth() );

				String finalImgName = imgCounter + "_" + n.getId();
				System.out.println( "Concatenating imgs and saving as: " + finalImgName );
				// BufferedImage trimmedImg = trimImg( hierarchyImg, config );
				// concatenateImagesAndSave( config.getOutputFolder().toString(), finalImgName, nodeImg, trimmedImg,
				// horizontalHistogram, verticalHistogram, statsImg );
				imgCounter++;
				// if(finalImgName.startsWith("2_"))
				// {
				// System.exit(0);
				// }
			}
		}
	}

	private BufferedImage createImageWithStatistics(
			HVConfig config,
			String nodeId,
			HierarchyStatistics stats,
			int height, int width ) {
		BufferedImage statsImg = new BufferedImage( width, height, BufferedImage.TYPE_INT_RGB );
		String out = stats.getSummaryString();
		String[] splittedOut = out.split( "\n" );
		String[] outputStrings = new String[splittedOut.length - 1];
		int[] eachLineWidth = new int[outputStrings.length];
		outputStrings[0] = splittedOut[0].replace( HVConstants.CSV_FILE_SEPARATOR, ": " );
		eachLineWidth[0] = outputStrings[0].length();
		Graphics2D g2d = statsImg.createGraphics();
		float fontSize = 10.0f;
		int horizontalBorder = 10;
		int verticalBorder = 10;
		Font normalFont = new Font( null, Font.PLAIN, (int)fontSize );

		// create final lines
		String[] partsOfHeader = splittedOut[1].split( HVConstants.CSV_FILE_SEPARATOR );
		for ( int i = 2; i < splittedOut.length; i++ ) {
			String[] lineVals = splittedOut[i].split( HVConstants.CSV_FILE_SEPARATOR );
			outputStrings[i - 1] = partsOfHeader[0] + " " + ( i - 2 ) + ": " + partsOfHeader[1] + " " + lineVals[1] + " ("
					+ lineVals[2] + "%)";
			eachLineWidth[i - 1] = outputStrings[i - 1].length();
		}
		int linesHeight = splittedOut.length - 1;
		int maxLetterWidth = -1;
		int maxLetterWidthIndex = -1;

		for ( int i = 0; i < eachLineWidth.length; i++ ) {
			if ( eachLineWidth[i] > maxLetterWidth ) {
				maxLetterWidth = eachLineWidth[i];
				maxLetterWidthIndex = i;
			}
		}

		if ( maxLetterWidth > linesHeight )// we fit the bigger dimension to available space
		{
			do {
				fontSize++;
				normalFont = normalFont.deriveFont( fontSize );
			} while ( g2d.getFontMetrics( normalFont ).stringWidth( outputStrings[maxLetterWidthIndex] ) <= width
					- 2 * horizontalBorder );

			fontSize--;
			normalFont = normalFont.deriveFont( fontSize );
		}
		else {
			do {
				fontSize++;
				normalFont = normalFont.deriveFont( fontSize );
			} while ( g2d.getFontMetrics( normalFont ).getAscent()
					+ g2d.getFontMetrics( normalFont ).getHeight() * ( outputStrings.length - 1 ) <= height
							- 2 * verticalBorder );

			fontSize--;
			normalFont = normalFont.deriveFont( fontSize );
		}

		int nodeHeight = nodeId.split( HVConstants.HIERARCHY_LEVEL_SEPARATOR ).length - 2;

		g2d.setFont( normalFont );
		for ( int i = 0; i < outputStrings.length; i++ ) {
			int yCord = g2d.getFontMetrics().getAscent() + verticalBorder + g2d.getFontMetrics().getHeight() * i;
			if ( nodeHeight + 1 == i ) {
				Font normalF = g2d.getFont();
				Color normalC = g2d.getColor();
				g2d.setFont( new Font( null, Font.BOLD, (int)fontSize ) );
				g2d.setColor( config.getCurrentLevelColor() );
				g2d.drawString( outputStrings[i], horizontalBorder, yCord );
				g2d.setFont( normalF );
				g2d.setColor( normalC );
			}
			else {
				g2d.drawString( outputStrings[i], horizontalBorder, yCord );
			}
		}

		return statsImg;
	}

	private HistogramTable createAllDataHistogramTable(
			LinkedList<Instance> subtreeInstances,
			int numberOfHistogramBins ) {
		Table histogramData = new Table();
		histogramData.addColumn( "x", double.class );
		histogramData.addColumn( "y", double.class );
		for ( Instance i : subtreeInstances ) {
			int newRowNum = histogramData.addRow();
			histogramData.set( newRowNum, "x", i.getData()[0] );
			histogramData.set( newRowNum, "y", i.getData()[1] );
		}

		HistogramTable histogramTable = new HistogramTable( histogramData, numberOfHistogramBins, ElementRole.OTHER.getNumber() );
		return histogramTable;
	}

	private BufferedImage addBorder(
			BufferedImage img,
			int leftWidth, int rightWidth,
			int topHeight, int bottomHeight,
			Color color ) {

		BufferedImage borderedImg = new BufferedImage(
				img.getWidth() + leftWidth + rightWidth,
				img.getHeight() + topHeight + bottomHeight,
				img.getType() );
		Graphics2D g = borderedImg.createGraphics();

		g.setColor( color );
		g.fillRect( 0, 0, borderedImg.getWidth(), borderedImg.getHeight() );
		g.drawImage( img, leftWidth, topHeight, img.getWidth(), img.getHeight(), null );

		return borderedImg;
	}

	private void concatenateImagesAndSave(
			String outputFolderPath,
			String imgName,
			BufferedImage nodeImg,
			BufferedImage hierarchyImg,
			BufferedImage horizontalHistogram,
			BufferedImage verticalHistogram,
			BufferedImage statsImg ) {

		int finalImgWidth = nodeImg.getWidth() + hierarchyImg.getWidth() + verticalHistogram.getWidth();
		int finalImgHeight = nodeImg.getHeight() + horizontalHistogram.getHeight();

		BufferedImage finalImg = new BufferedImage( finalImgWidth, finalImgHeight, BufferedImage.TYPE_INT_ARGB );
		Graphics2D g = finalImg.createGraphics();

		g.drawImage( hierarchyImg, 0, 0, hierarchyImg.getWidth(), hierarchyImg.getHeight(), null );
		g.drawImage( nodeImg, hierarchyImg.getWidth(), 0, nodeImg.getWidth(), nodeImg.getHeight(), null );
		g.drawImage( verticalHistogram, hierarchyImg.getWidth() + nodeImg.getWidth(), 0, verticalHistogram.getWidth(),
				verticalHistogram.getHeight(), null );
		g.drawImage( horizontalHistogram, hierarchyImg.getWidth(), nodeImg.getHeight(), horizontalHistogram.getWidth(),
				horizontalHistogram.getHeight(), null );
		g.drawImage( statsImg, hierarchyImg.getWidth() + nodeImg.getWidth(), nodeImg.getHeight(), statsImg.getWidth(),
				statsImg.getHeight(),
				null );

		try {
			ImageIO.write( finalImg, "PNG", new File( outputFolderPath + File.separator + imgName + ".png" ) );
		}
		catch ( IOException e ) {
			e.printStackTrace();
		}
	}

	private BufferedImage createHistogram(
			HistogramTable histogramTable,
			HistogramTable directParentHistogramTable,
			String field,
			HVConfig config,
			int imgSize,
			int nodeImgLeftBorderWidth, int nodeImgRightBorderWidth ) {

		HistogramGraph histogramGraph = new HistogramGraph(
				histogramTable,
				directParentHistogramTable,
				field,
				imgSize,
				(int)( 0.5 * imgSize ),
				config,
				nodeImgLeftBorderWidth,
				nodeImgRightBorderWidth );

		return getDisplaySnapshot( histogramGraph );
	}

	private BufferedImage createHorizontalHistogram(
			HistogramTable histogramTable,
			HistogramTable directParentHistogramTable,
			HVConfig config,
			int nodeImgFinalWidth, int nodeImgFinalHeight,
			int nodeImgLeftBorderWidth, int nodeImgRightBorderWidth ) {

		return createHistogram( histogramTable, directParentHistogramTable, "x", config,
				nodeImgFinalWidth, nodeImgLeftBorderWidth, nodeImgRightBorderWidth );
	}

	private BufferedImage createVerticalHistogram(
			HistogramTable histogramTable,
			HistogramTable directParentHistogramTable,
			HVConfig config,
			int nodeImgFinalWidth, int nodeImgFinalHeight,
			int nodeImgLeftBorderWidth, int nodeImgRightBorderWidth ) {

		BufferedImage img = createHistogram( histogramTable, directParentHistogramTable, "y",
				config, nodeImgFinalHeight, nodeImgLeftBorderWidth, nodeImgRightBorderWidth );

		return Utils.rotate( img, 90 );
	}

	private HistogramTable prepareHistogramTableUsingAllDataBins(
			Node node,
			HistogramTable allDataHistogramTable,
			int roleNum ) {
		Table histogramData = new Table();
		histogramData.addColumn( "x", double.class );
		histogramData.addColumn( "y", double.class );
		for ( Instance i : node.getNodeInstances() ) {
			int newRowNum = histogramData.addRow();
			histogramData.set( newRowNum, "x", i.getData()[0] );
			histogramData.set( newRowNum, "y", i.getData()[1] );
		}

		HistogramTable histogramTable = new HistogramTable( histogramData, /* 100, */ allDataHistogramTable, roleNum );
		return histogramTable;
	}

	public Tree createHierarchyTree( Node root, HVConfig config ) {
		Tree hierarchyVisualisation = new Tree();
		hierarchyVisualisation.addColumn( HVConstants.PREFUSE_NODE_ID_COLUMN_NAME, String.class );
		// hierarchyVisualisation.addColumn(HVConstants.PREFUSE_NUMBER_OF_INSTANCES_COLUMN_NAME, Integer.class);
		hierarchyVisualisation.addColumn( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, int.class );

		prefuse.data.Node n = hierarchyVisualisation.addRoot();
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

			n = hierarchyVisualisation.addChild( sourceNodeWithItsParent.getKey() );
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

		maxTreeWidth = java.util.Collections.max( treeLevelWithWidth.values() );

		// predict height and width of hierarchy image
		finalSizeOfNodes = 0;
		int widthBasedSizeOfNodes = 0;
		int heightBasedSizeOfNodes = 0;
		treeOrientation = prefuse.Constants.ORIENT_TOP_BOTTOM;// TODO: the orientation of charts could be set automatically depending on the
		// size of hierarchy
		levelGap = 0.0;// the spacing to maintain between depth levels of the tree
		siblingNodeGap = 0.0;// the spacing to maintain between sibling nodes
		subtreeGap = 0.0;// the spacing to maintain between neighboring subtrees
		int hierarchyImageWidth = config.getTreeWidth();
		int hierarchyImageHeight = config.getTreeHeight();
		levelGap = Math.max( 1.0, ( hierarchyImageHeight ) /
				(double)( nodeSizeToBetweenLevelSpaceRatio * (double)maxTreeDepth + nodeSizeToBetweenLevelSpaceRatio
						+ (double)maxTreeDepth ) );
		// based on above calculation - compute "optimal" size of each node on image
		heightBasedSizeOfNodes = (int)( nodeSizeToBetweenLevelSpaceRatio * levelGap );

		System.out.println( "Between level space: " + levelGap + " node size: " + heightBasedSizeOfNodes );

		siblingNodeGap = Math.max( 1.0,
				( (double)hierarchyImageWidth )
						/ ( (double)maxTreeWidth * nodeSizeToBetweenSiblingsSpaceRatio + (double)maxTreeWidth - 1.0 ) );
		subtreeGap = siblingNodeGap;
		widthBasedSizeOfNodes = (int)( nodeSizeToBetweenSiblingsSpaceRatio * siblingNodeGap );
		System.out.println( "Between siblings space: " + siblingNodeGap + " node size: " + widthBasedSizeOfNodes );

		// below use MAXIMUM height/width
		if ( widthBasedSizeOfNodes < heightBasedSizeOfNodes ) {
			finalSizeOfNodes = widthBasedSizeOfNodes;
			// assume maximum possible size
			levelGap = Math.max( 1.0,
					( (double)hierarchyImageHeight - (double)maxTreeDepth * (double)finalSizeOfNodes - (double)finalSizeOfNodes )
							/ (double)maxTreeDepth );
		}
		else {
			finalSizeOfNodes = heightBasedSizeOfNodes;
			// assume maximum possible size
			siblingNodeGap = Math.max( 1.0,
					( (double)hierarchyImageWidth - (double)maxTreeWidth * (double)finalSizeOfNodes )
							/ ( (double)maxTreeWidth - 1.0 ) );
			subtreeGap = siblingNodeGap;
		}

		return hierarchyVisualisation;
	}

	private BufferedImage createTreeImage( HVContext context, String currentNodeId ) {
		return getDisplaySnapshot( createTreeDisplay( context, currentNodeId ) );
	}

	public Visualization createTreeVisualization( HVContext context ) {
		return createTreeVisualization( context, null );
	}

	@SuppressWarnings("unchecked")
	public Visualization createTreeVisualization( HVContext context, String currentNodeId ) {
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
					ColorLib.color( Color.lightGray ) );

			NodeLinkTreeLayout treeLayout = new NodeLinkTreeLayout(
					HVConstants.NAME_OF_HIERARCHY,
					treeOrientation,
					levelGap,
					siblingNodeGap,
					subtreeGap );
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

	public static void layoutVisualization( Visualization vis ) {
		// TODO: in run function a threads are used, so threads could be somehow used
		// to fill the images more efficiently
		vis.run( HVConstants.NAME_OF_HIERARCHY + ".edges" );
		vis.run( HVConstants.NAME_OF_HIERARCHY + ".layout" );

		Utils.waitUntilActivitiesAreFinished();
	}

	private Display createTreeDisplay( HVContext context, String currentNodeId ) {
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

	public Display createTreeDisplay( HVContext context ) {
		return createTreeDisplay( context, null );
	}

	public BufferedImage createPointImage( HVContext context, Node node ) {
		Rectangle2D bounds = calculateBoundingRectForCluster( node );
		int nodeImgFinalWidth = (int)( context.getConfig().getPointWidth() +
				Math.max( 1.0, context.getConfig().getPointScallingFactor() / 2 ) );
		int nodeImgFinalHeight = (int)( context.getConfig().getPointHeight() +
				Math.max( 1.0, context.getConfig().getPointScallingFactor() / 2 ) );

		LinkedList<Instance> allPoints = null;
		if ( context.getConfig().isDisplayAllPoints() ) {
			allPoints = context.getHierarchy().getRoot().getSubtreeInstances();
		}

		BufferedImage nodeImg = new BufferedImage( nodeImgFinalWidth, nodeImgFinalHeight, BufferedImage.TYPE_INT_ARGB );

		if ( context.getConfig().getBackgroundColor() != null )
			nodeImg = setBackgroud( nodeImg, context.getConfig().getBackgroundColor() );

		nodeImg = fillImage( nodeImg, node, context.getConfig(), bounds, allPoints );

		return nodeImg;
	}

	private BufferedImage trimImg( BufferedImage img, HVConfig config ) {
		// TODO instead of iterating through the columns of image, we can use BINARY SEARCH through columns
		// and check if column doesn't contain at least 1 non-background colour pixel
		int imgHeight = img.getHeight();
		int imgWidth = img.getWidth();

		// TRIM WIDTH - LEFT
		int startWidth = 0;
		for ( int x = 0; x < imgWidth; x++ ) {
			if ( startWidth == 0 ) {
				for ( int y = 0; y < imgHeight; y++ ) {
					if ( img.getRGB( x, y ) != config.getBackgroundColor().getRGB() ) {
						startWidth = x;
						break;
					}
				}
			}
			else
				break;
		}

		// TRIM WIDTH - RIGHT
		int endWidth = 0;
		for ( int x = imgWidth - 1; x >= 0; x-- ) {
			if ( endWidth == 0 ) {
				for ( int y = 0; y < imgHeight; y++ ) {
					if ( img.getRGB( x, y ) != config.getBackgroundColor().getRGB() ) {
						endWidth = x;
						break;
					}
				}
			}
			else
				break;
		}

		int newWidth = endWidth - startWidth;

		BufferedImage newImg = new BufferedImage(
				newWidth,
				imgHeight,
				BufferedImage.TYPE_INT_RGB );
		Graphics g = newImg.createGraphics();
		g.drawImage( img, 0, 0, newImg.getWidth(), newImg.getHeight(), startWidth, 0, endWidth, imgHeight, null );
		img = newImg;

		return img;
	}

	private BufferedImage setBackgroud( BufferedImage image, Color backgroundColor ) {
		Graphics2D g2d = image.createGraphics();
		g2d.setPaint( backgroundColor );
		g2d.fillRect( 0, 0, image.getWidth(), image.getHeight() );
		g2d.dispose();
		return image;
	}

	private BufferedImage fillImage(
			BufferedImage nodeImg,
			Node node,
			HVConfig config,
			Rectangle2D bounds,
			LinkedList<Instance> allPoints ) {

		Graphics2D imgContent = nodeImg.createGraphics();

		if ( node != null ) {
			if ( config.isDisplayAllPoints() ) {
				drawPoints(
						imgContent,
						allPoints,
						config.getOtherGroupColor(),
						config.getPointScallingFactor(),
						config.getPointWidth(), config.getPointHeight(),
						bounds );

				if ( node.getParent() != null ) {
					drawParentAncestorsPoints(
							imgContent,
							node,
							config.getAncestorGroupColor(),
							config.getPointScallingFactor(),
							config.getPointWidth(), config.getPointHeight(),
							bounds );

					drawPoints(
							imgContent,
							node.getParent().getNodeInstances(),
							config.getParentGroupColor(),
							config.getPointScallingFactor(),
							config.getPointWidth(), config.getPointHeight(),
							bounds );
				}
			}

			drawPoints(
					imgContent,
					node.getSubtreeInstances(),
					config.getChildGroupColor(),
					config.getPointScallingFactor(),
					config.getPointWidth(), config.getPointHeight(),
					bounds );
			drawPoints(
					imgContent,
					node.getNodeInstances(),
					config.getCurrentLevelColor(),
					config.getPointScallingFactor(),
					config.getPointWidth(), config.getPointHeight(),
					bounds );
		}

		imgContent.dispose();
		return nodeImg;
	}

	private void drawParentAncestorsPoints(
			Graphics2D imgContent,
			Node parent,
			Color parentAncestorsColor,
			double pointScallingFactor,
			int imageWidth, int imageHeight,
			Rectangle2D bounds ) {

		Node n = parent;
		while ( n.getParent() != null ) {
			drawPoints(
					imgContent,
					n.getParent().getNodeInstances(),
					parentAncestorsColor,
					pointScallingFactor,
					imageWidth, imageHeight,
					bounds );
			n = n.getParent();
		}
	}

	private void drawPoints(
			Graphics2D imgContent,
			LinkedList<Instance> points,
			Color color,
			double pointScallingFactor,
			int imgWidth, int imgHeight,
			Rectangle2D bounds ) {

		Color oldColor = imgContent.getColor();
		imgContent.setColor( color );

		int pointSize = (int)pointScallingFactor - 1;

		for ( Instance i : points ) {
			double x = i.getData()[0];
			double y = i.getData()[1];
			int pointLeftEdge = rectCoordinateOnImage(
					x,
					bounds.getMinX(), bounds.getMaxX(),
					imgWidth, pointSize );
			int pointTopEdge = rectCoordinateOnImage(
					y,
					bounds.getMinY(), bounds.getMaxY(),
					imgHeight, pointSize );

			imgContent.fillRect( pointLeftEdge, pointTopEdge, pointSize + 1, pointSize + 1 ); // 1 px * scallingFactor
		}

		imgContent.setColor( oldColor );
	}

	private int rectCoordinateOnImage( double sourceValue, double min, double max, int dimSize, int pointSize ) {
		double result = dimSize * ( sourceValue - min ) / ( max - min );
		result -= pointSize / 2.0;
		return (int)result;
	}

	/**
	 * Calculates the smallest rectangle containing all points within
	 * the specified node's cluster.
	 * 
	 * @param node
	 *            the node for which the extents are to be computed
	 * @return the smallest bounding rectangle
	 */
	private Rectangle2D calculateBoundingRectForCluster( Node node ) {
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;

		for ( Instance i : node.getSubtreeInstances() ) {
			double x = i.getData()[0];
			double y = i.getData()[1];

			minX = Math.min( minX, x );
			minY = Math.min( minY, y );
			maxX = Math.max( maxX, x );
			maxY = Math.max( maxY, y );
		}

		return new Rectangle2D.Double( minX, minY, maxX - minX, maxY - minY );
	}

	public static BufferedImage getDisplaySnapshot( Display dis ) {
		BufferedImage img = null;
		try {
			// get an image to draw into
			Dimension d = new Dimension( dis.getWidth(), dis.getHeight() );
			if ( !GraphicsEnvironment.isHeadless() ) {
				try {
					img = (BufferedImage)dis.createImage( dis.getWidth(), dis.getHeight() );
				}
				catch ( Exception e ) {
					img = null;
				}
			}

			if ( img == null ) {
				img = new BufferedImage(
						dis.getWidth(),
						dis.getHeight(),
						BufferedImage.TYPE_INT_RGB );
			}
			Graphics2D g = (Graphics2D)img.getGraphics();

			// set up the display, render, then revert to normal settings
			Point2D p = new Point2D.Double( 0, 0 );
			dis.zoom( p, 1.0 ); // also takes care of damage report
			boolean q = dis.isHighQuality();
			dis.setHighQuality( true );
			dis.paintDisplay( g, d );
			dis.setHighQuality( q );
			dis.zoom( p, 1.0 ); // also takes care of damage report
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}

		return img;
	}

	public Node findNode( HVContext context, int row ) {
		Hierarchy h = context.getHierarchy();
		Tree tree = context.getTree();

		Node root = h.getRoot();
		prefuse.data.Node n = tree.getRoot();

		if ( row == 0 ) {
			return root;
		}

		Queue<Map.Entry<prefuse.data.Node, Node>> stackParentAndChild = new LinkedList<>(); // FIFO
		for ( Node child : root.getChildren() ) {
			stackParentAndChild.add( new AbstractMap.SimpleEntry<prefuse.data.Node, Node>( n, child ) );
		}

		int currentRow = 0;
		while ( !stackParentAndChild.isEmpty() ) {
			Entry<prefuse.data.Node, Node> sourceNodeWithItsParent = stackParentAndChild.remove();
			Node sourceNode = sourceNodeWithItsParent.getValue();

			++currentRow;
			if ( currentRow == row ) {
				return sourceNode;
			}

			for ( Node child : sourceNode.getChildren() ) {
				stackParentAndChild.add( new AbstractMap.SimpleEntry<prefuse.data.Node, Node>( n, child ) );
			}
		}

		return null;
	}
}
