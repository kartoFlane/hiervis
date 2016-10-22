package pl.pwr.hiervis.visualisation;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import javax.imageio.ImageIO;

import basic_hierarchy.interfaces.Hierarchy;
import basic_hierarchy.interfaces.Instance;
import basic_hierarchy.interfaces.Node;
import pl.pwr.hiervis.core.ElementRole;
import pl.pwr.hiervis.core.HVConfig;
import pl.pwr.hiervis.core.HVConstants;
import pl.pwr.hiervis.core.HVContext;
import pl.pwr.hiervis.core.HierarchyStatistics;
import pl.pwr.hiervis.util.ImageUtils;
import pl.pwr.hiervis.util.Utils;
import pl.pwr.hiervis.util.prefuse.histogram.HistogramGraph;
import pl.pwr.hiervis.util.prefuse.histogram.HistogramTable;
import prefuse.data.Table;


/**
 * This holds old code that is no longer used, but might be useful in the future.
 *
 */
public class HierarchyImager
{
	private HierarchyImager()
	{
		// Static class -- disallow instantiation.
		throw new RuntimeException( "Attempted to instantiate a static class: " + getClass().getName() );
	}

	public static void process( HVContext context, HierarchyStatistics stats )
	{
		Hierarchy input = context.getHierarchy();
		HVConfig config = context.getConfig();

		int nodeImgLeftBorderWidth = 5;
		int nodeImgRightBorderWidth = 30;// this value determine the width of labels on the OY axis
		int nodeImgTopBorderHeight = 5;
		int nodeImgBottomBorderHeight = 30;// this value determine the width of labels on the OX axis
		int nodeImgFinalWidth = (int)( config.getPointWidth() + Math.max( 1.0, config.getPointScallingFactor() / 2 ) );
		int nodeImgFinalHeight = (int)( config.getPointHeight() + Math.max( 1.0, config.getPointScallingFactor() / 2 ) );

		Rectangle2D bounds = Utils.calculateBoundingRectForCluster( input.getRoot() );

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
					nodeImg = ImageUtils.setBackgroud( nodeImg, config.getBackgroundColor() );

				nodeImg = fillImage( nodeImg, n, config, bounds, allPoints );
				nodeImg = ImageUtils.addBorder(
					nodeImg,
					nodeImgLeftBorderWidth, nodeImgRightBorderWidth,
					nodeImgTopBorderHeight, nodeImgBottomBorderHeight,
					Color.black
				);

				System.out.println( "Hierary img..." );
				// BufferedImage hierarchyImg = createTreeImage( context, n.getId() );

				System.out.println( "Prepare histogram data..." );
				HistogramTable allDataHistogramTable = createAllDataHistogramTable(
					input.getRoot().getSubtreeInstances(),
					config.getNumberOfHistogramBins()
				);
				HistogramGraph.setAllDataHistoTable( allDataHistogramTable );
				HistogramTable histogramTable = prepareHistogramTableUsingAllDataBins(
					n, allDataHistogramTable,
					ElementRole.CURRENT.getNumber()
				);
				HistogramTable directParentHistogramTable = n.getParent() == null ? null
					: prepareHistogramTableUsingAllDataBins(
						n.getParent(), allDataHistogramTable,
						ElementRole.DIRECT_PARENT.getNumber()
					);

				System.out.println( "Horizontal histogram..." );
				BufferedImage horizontalHistogram = createHorizontalHistogram(
					histogramTable, directParentHistogramTable, config,
					nodeImgFinalWidth,
					nodeImgFinalHeight, nodeImgLeftBorderWidth, nodeImgRightBorderWidth
				);

				System.out.println( "Vertical histogram..." );
				BufferedImage verticalHistogram = createVerticalHistogram(
					histogramTable, directParentHistogramTable, config,
					nodeImgFinalWidth,
					nodeImgFinalHeight, nodeImgTopBorderHeight, nodeImgBottomBorderHeight
				);

				System.out.println( "Statistics..." );
				BufferedImage statsImg = createImageWithStatistics(
					config, n.getId(), stats, horizontalHistogram.getHeight(),
					verticalHistogram.getWidth()
				);

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

	private static int rectCoordinateOnImage( double sourceValue, double min, double max, int dimSize, int pointSize )
	{
		double result = dimSize * ( sourceValue - min ) / ( max - min );
		result -= pointSize / 2.0;
		return (int)result;
	}

	private static BufferedImage createVerticalHistogram(
		HistogramTable histogramTable,
		HistogramTable directParentHistogramTable,
		HVConfig config,
		int nodeImgFinalWidth, int nodeImgFinalHeight,
		int nodeImgLeftBorderWidth, int nodeImgRightBorderWidth )
	{

		BufferedImage img = createHistogram(
			histogramTable, directParentHistogramTable, "y",
			config, nodeImgFinalHeight, nodeImgLeftBorderWidth, nodeImgRightBorderWidth
		);

		return ImageUtils.rotate( img, 90 );
	}

	private static BufferedImage createHistogram(
		HistogramTable histogramTable,
		HistogramTable directParentHistogramTable,
		String field,
		HVConfig config,
		int imgSize,
		int nodeImgLeftBorderWidth, int nodeImgRightBorderWidth )
	{

		HistogramGraph histogramGraph = new HistogramGraph(
			histogramTable,
			directParentHistogramTable,
			field,
			imgSize,
			(int)( 0.5 * imgSize ),
			config,
			nodeImgLeftBorderWidth,
			nodeImgRightBorderWidth
		);

		return Utils.getDisplaySnapshot( histogramGraph );
	}

	private static BufferedImage createHorizontalHistogram(
		HistogramTable histogramTable,
		HistogramTable directParentHistogramTable,
		HVConfig config,
		int nodeImgFinalWidth, int nodeImgFinalHeight,
		int nodeImgLeftBorderWidth, int nodeImgRightBorderWidth )
	{

		return createHistogram(
			histogramTable, directParentHistogramTable, "x", config,
			nodeImgFinalWidth, nodeImgLeftBorderWidth, nodeImgRightBorderWidth
		);
	}

	private static BufferedImage createPointImage( HVContext context, Node node )
	{
		Rectangle2D bounds = Utils.calculateBoundingRectForCluster( node );
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
			nodeImg = ImageUtils.setBackgroud( nodeImg, context.getConfig().getBackgroundColor() );

		nodeImg = fillImage( nodeImg, node, context.getConfig(), bounds, allPoints );

		return nodeImg;
	}

	private static BufferedImage createImageWithStatistics(
		HVConfig config,
		String nodeId,
		HierarchyStatistics stats,
		int height, int width )
	{
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


	private static void drawPoints(
		Graphics2D imgContent,
		LinkedList<Instance> points,
		Color color,
		double pointScallingFactor,
		int imgWidth, int imgHeight,
		Rectangle2D bounds )
	{

		Color oldColor = imgContent.getColor();
		imgContent.setColor( color );

		int pointSize = (int)pointScallingFactor - 1;

		for ( Instance i : points ) {
			double x = i.getData()[0];
			double y = i.getData()[1];
			int pointLeftEdge = rectCoordinateOnImage(
				x,
				bounds.getMinX(), bounds.getMaxX(),
				imgWidth, pointSize
			);
			int pointTopEdge = rectCoordinateOnImage(
				y,
				bounds.getMinY(), bounds.getMaxY(),
				imgHeight, pointSize
			);

			imgContent.fillRect( pointLeftEdge, pointTopEdge, pointSize + 1, pointSize + 1 ); // 1 px * scallingFactor
		}

		imgContent.setColor( oldColor );
	}

	private static void drawParentAncestorsPoints(
		Graphics2D imgContent,
		Node parent,
		Color parentAncestorsColor,
		double pointScallingFactor,
		int imageWidth, int imageHeight,
		Rectangle2D bounds )
	{

		Node n = parent;
		while ( n.getParent() != null ) {
			drawPoints(
				imgContent,
				n.getParent().getNodeInstances(),
				parentAncestorsColor,
				pointScallingFactor,
				imageWidth, imageHeight,
				bounds
			);
			n = n.getParent();
		}
	}

	private static BufferedImage fillImage(
		BufferedImage nodeImg,
		Node node,
		HVConfig config,
		Rectangle2D bounds,
		LinkedList<Instance> allPoints )
	{

		Graphics2D imgContent = nodeImg.createGraphics();

		if ( node != null ) {
			if ( config.isDisplayAllPoints() ) {
				drawPoints(
					imgContent,
					allPoints,
					config.getOtherGroupColor(),
					config.getPointScallingFactor(),
					config.getPointWidth(), config.getPointHeight(),
					bounds
				);

				if ( node.getParent() != null ) {
					drawParentAncestorsPoints(
						imgContent,
						node,
						config.getAncestorGroupColor(),
						config.getPointScallingFactor(),
						config.getPointWidth(), config.getPointHeight(),
						bounds
					);

					drawPoints(
						imgContent,
						node.getParent().getNodeInstances(),
						config.getParentGroupColor(),
						config.getPointScallingFactor(),
						config.getPointWidth(), config.getPointHeight(),
						bounds
					);
				}
			}

			drawPoints(
				imgContent,
				node.getSubtreeInstances(),
				config.getChildGroupColor(),
				config.getPointScallingFactor(),
				config.getPointWidth(), config.getPointHeight(),
				bounds
			);
			drawPoints(
				imgContent,
				node.getNodeInstances(),
				config.getCurrentLevelColor(),
				config.getPointScallingFactor(),
				config.getPointWidth(), config.getPointHeight(),
				bounds
			);
		}

		imgContent.dispose();
		return nodeImg;
	}

	private static HistogramTable createAllDataHistogramTable(
		LinkedList<Instance> subtreeInstances,
		int numberOfHistogramBins )
	{
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

	private static HistogramTable prepareHistogramTableUsingAllDataBins(
		Node node,
		HistogramTable allDataHistogramTable,
		int roleNum )
	{
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

	private static void concatenateImagesAndSave(
		String outputFolderPath,
		String imgName,
		BufferedImage nodeImg,
		BufferedImage hierarchyImg,
		BufferedImage horizontalHistogram,
		BufferedImage verticalHistogram,
		BufferedImage statsImg )
	{

		int finalImgWidth = nodeImg.getWidth() + hierarchyImg.getWidth() + verticalHistogram.getWidth();
		int finalImgHeight = nodeImg.getHeight() + horizontalHistogram.getHeight();

		BufferedImage finalImg = new BufferedImage( finalImgWidth, finalImgHeight, BufferedImage.TYPE_INT_ARGB );
		Graphics2D g = finalImg.createGraphics();

		g.drawImage( hierarchyImg, 0, 0, hierarchyImg.getWidth(), hierarchyImg.getHeight(), null );
		g.drawImage( nodeImg, hierarchyImg.getWidth(), 0, nodeImg.getWidth(), nodeImg.getHeight(), null );
		g.drawImage(
			verticalHistogram, hierarchyImg.getWidth() + nodeImg.getWidth(), 0, verticalHistogram.getWidth(),
			verticalHistogram.getHeight(), null
		);
		g.drawImage(
			horizontalHistogram, hierarchyImg.getWidth(), nodeImg.getHeight(), horizontalHistogram.getWidth(),
			horizontalHistogram.getHeight(), null
		);
		g.drawImage(
			statsImg, hierarchyImg.getWidth() + nodeImg.getWidth(), nodeImg.getHeight(), statsImg.getWidth(),
			statsImg.getHeight(),
			null
		);

		try {
			ImageIO.write( finalImg, "PNG", new File( outputFolderPath + File.separator + imgName + ".png" ) );
		}
		catch ( IOException e ) {
			e.printStackTrace();
		}
	}
}
