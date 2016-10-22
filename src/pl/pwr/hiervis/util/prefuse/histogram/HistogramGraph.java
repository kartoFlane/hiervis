package pl.pwr.hiervis.util.prefuse.histogram;

import java.awt.Insets;
import java.awt.geom.Rectangle2D;

import javax.swing.BorderFactory;

import pl.pwr.hiervis.core.HVConfig;
import pl.pwr.hiervis.util.Utils;
import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.ActionList;
import prefuse.action.RepaintAction;
import prefuse.action.assignment.ColorAction;
import prefuse.action.layout.AxisLabelLayout;
import prefuse.action.layout.AxisLayout;
import prefuse.data.query.NumberRangeModel;
import prefuse.render.AxisRenderer;
import prefuse.render.PolygonRenderer;
import prefuse.render.Renderer;
import prefuse.render.RendererFactory;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.VisiblePredicate;


/**
 * A simple histogram visualization that allows different columns
 * in a data table to be histogramized and displayed.
 * The starting point was ScatterPlot.java by Jeffrey Heer, but
 * Kaitlin Duck Sherwood has modified it quite extensively.
 * 
 * Kaitlin Duck Sherwood's modifications are granted as is for any
 * commercial or non-commercial use, with or without attribution.
 * The only conditions are that you can't pretend that you wrote them,
 * and that you leave these notices about her authorship in the source.
 * 
 * Known bug: See the comments for HistogramFrame; there might
 * be a bug in HistogramGraph::updateAxes(), but I sure can't figure
 * out what it is. I suspect that it is a prefuse bug.
 * 
 * Note: I wanted to use Prefuse's StackedAreaChart, but couldn't
 * get it to work. If you figure out how to make it work, please
 * email me. -- KDS
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @author <a href="http://webfoot.com/ducky.home.html">Kaitlin Duck Sherwood</a>
 */
public class HistogramGraph extends Display
{
	private static final long serialVersionUID = 1L;
	protected static final String current = "current";
	protected static final String allData = "allData";
	protected static final String parent = "parent";
	public static final int DEFAULT_BIN_COUNT = 15;

	// KDS -- I tend to make things protected instead of private so
	// that people can subclass them. I'm not sure that's the right
	// thing to do.
	protected Rectangle2D m_dataB = new Rectangle2D.Double();
	protected Rectangle2D m_xlabB = new Rectangle2D.Double();
	protected Rectangle2D m_ylabB = new Rectangle2D.Double();

	protected BarRenderer m_shapeR;

	protected static HistogramTable m_allDataHistoTable;

	protected HistogramTable m_currentHistoTable;
	protected HistogramTable m_directParentHistoTable;

	private AxisLayout m_xAxis, m_yAxis;
	private AxisLabelLayout m_xLabels, m_yLabels;


	/**
	 * @param histoTable
	 *            a histogrammized version of dataTable
	 * @param dataTable
	 *            a prefuse Table which holds the raw (unhistogrammized) data.
	 * @param startingField
	 *            the name of the field (column) of the data table
	 *            whose histogram is to be shown in the histogram graph.
	 * 
	 *            Note that the data table isn't used much here, and could maybe be moved into
	 *            Histo
	 * @param nodeImgBorderWidth
	 * @param nodeImgRightBorderWidth
	 */
	public HistogramGraph( HistogramTable currentHistoTable, HistogramTable directParentHistoTable,
		String startingField, int width, int height, HVConfig params, int nodeImgLeftBorderWidth, int nodeImgRightBorderWidth )
	{
		super( new Visualization() );

		m_shapeR = new BarRenderer( 10, params );
		m_currentHistoTable = currentHistoTable;
		m_directParentHistoTable = directParentHistoTable;
		startingField = getStartingField( startingField );

		// --------------------------------------------------------------------
		// STEP 1: setup the visualized data

		if ( m_directParentHistoTable != null ) {
			for ( int i = 0; i < m_directParentHistoTable.getRowCount(); i++ ) {
				int newRowNumber = m_allDataHistoTable.addRow();
				for ( int j = 0; j < m_allDataHistoTable.getColumnCount(); j++ ) {
					m_allDataHistoTable.set( newRowNumber, j, m_directParentHistoTable.get( i, j ) );
				}
			}
		}

		for ( int i = 0; i < m_currentHistoTable.getRowCount(); i++ ) {
			int newRowNumber = m_allDataHistoTable.addRow();
			for ( int j = 0; j < m_allDataHistoTable.getColumnCount(); j++ ) {
				m_allDataHistoTable.set( newRowNumber, j, m_currentHistoTable.get( i, j ) );
			}
		}
		m_vis.addTable( allData, m_allDataHistoTable );

		// m_allDataHistoTable.printWholeTable();

		initializeRenderer();

		// --------------------------------------------------------------------
		// STEP 2: create actions to process the visual data
		initializeAxes( startingField );

		ColorAction allDataColor = new ColorAction(
			allData, VisualItem.FILLCOLOR,
			params.getOtherGroupColor().getRGB()
		);

		m_vis.putAction( "color", allDataColor );

		ActionList draw = new ActionList();
		draw.add( m_xAxis );
		draw.add( m_yAxis );
		draw.add( m_xLabels );
		draw.add( m_yLabels );

		draw.add( allDataColor );
		draw.add( new RepaintAction() );
		m_vis.putAction( "draw", draw );

		// --------------------------------------------------------------------
		// STEP 3: set up a display and ui components to show the visualization

		initializeWindowCharacteristics( width, height, nodeImgLeftBorderWidth, nodeImgRightBorderWidth );

		// --------------------------------------------------------------------
		// STEP 4: launching the visualization

		m_vis.run( "draw" );
		Utils.waitUntilActivitiesAreFinished();
	}

	/**
	 * This sets up various things about the window, including the size.
	 * TODO include the size in the constructor
	 * 
	 * @param nodeImgBorderWidth
	 * @param nodeImgRightBorderWidth
	 */
	private void initializeWindowCharacteristics( int width, int height, int nodeImgLeftBorderWidth, int nodeImgRightBorderWidth )
	{
		setBorder( BorderFactory.createEmptyBorder( height / 100, nodeImgLeftBorderWidth, height / 100, /* nodeImgBorderWidth */0 ) );// zamiast prawego marginesu bedzie os OY
		setSize( width + nodeImgLeftBorderWidth + nodeImgRightBorderWidth, height );

		setHighQuality( true );
		double barWidthOnCanvas = initializeLayoutBoundsForDisplay( nodeImgLeftBorderWidth, nodeImgRightBorderWidth );
		m_shapeR.setBarWidth( barWidthOnCanvas );
		m_shapeR.setBounds( m_dataB );
	}

	/**
	 * @param fieldName
	 *            the name of the field (column) to display
	 */
	private void initializeAxes( String fieldName )
	{
		m_xAxis = new AxisLayout(
			allData, fieldName,
			Constants.X_AXIS, VisiblePredicate.TRUE
		);
		m_xAxis.setLayoutBounds( m_dataB );
		m_xAxis.setDataType( Constants.NUMERICAL );
		m_vis.putAction( "x", m_xAxis );

		String countField = HistogramTable.getCountField( fieldName );
		m_yAxis = new AxisLayout(
			allData, countField,
			Constants.Y_AXIS, VisiblePredicate.TRUE
		);

		m_yAxis.setLayoutBounds( m_dataB );
		m_vis.putAction( "y", m_yAxis );

		m_xLabels = new AxisLabelLayout( "xlabels", m_xAxis, m_xlabB );
		m_vis.putAction( "xlabels", m_xLabels );

		m_yLabels = new AxisLabelLayout( "ylabels", m_yAxis, m_ylabB );
		m_vis.putAction( "ylabels", m_yLabels );
	}


	private void initializeRenderer()
	{
		m_vis.setRendererFactory(
			new RendererFactory() {
				Renderer yAxisRenderer = new AxisRenderer( Constants.RIGHT, Constants.TOP );
				Renderer xAxisRenderer = new AxisRenderer( Constants.CENTER, Constants.FAR_BOTTOM );
				Renderer barRenderer = new PolygonRenderer( Constants.POLY_TYPE_LINE );


				public Renderer getRenderer( VisualItem item )
				{
					if ( item.isInGroup( "ylabels" ) )
						return yAxisRenderer;
					if ( item.isInGroup( "xlabels" ) )
						return xAxisRenderer;
					if ( item.isInGroup( "barchart" ) )
						return barRenderer;
					return m_shapeR;

				}
			}
		);
	}

	// This is taken from CongressDemo.displayLayout.
	// This puts the axes on the right
	public double initializeLayoutBoundsForDisplay( int nodeImgLeftBorderWidth, int nodeImgRightBorderWidth )
	{
		Insets i = getInsets();
		int w = getWidth();
		int h = getHeight();
		int insetWidth = i.left + i.right;
		int insetHeight = i.top + i.bottom;
		int xAxisHeightWithLabels = 20;
		int xAxisLabelsHeight = 10;
		int numberOfBins = m_allDataHistoTable.getBinCount();

		double widthForBars = w - insetWidth - nodeImgRightBorderWidth;
		double eachBarWidth = widthForBars / numberOfBins;
		double finalWidthForBars = ( widthForBars - eachBarWidth );// there is a need to subtract one bar width, because when rendering we have access to
		// LEFT border only and we need to simulate the width of bar

		m_dataB.setRect( i.left, i.top, finalWidthForBars, h - xAxisHeightWithLabels - insetHeight ); // TODO: when implementing the possibility of chart
		// orientation change we should get rid of barWidthOnCanvas and add something in height

		m_xlabB.setRect(
			i.left, h - xAxisHeightWithLabels - i.bottom, w - insetWidth - nodeImgRightBorderWidth,
			xAxisHeightWithLabels - xAxisLabelsHeight
		);
		m_ylabB.setRect( i.left, i.top, w - insetWidth, h - insetHeight - xAxisHeightWithLabels );

		m_vis.run( "update" );
		Utils.waitUntilActivitiesAreFinished();
		return eachBarWidth;
	}


	/**
	 * @param dataField
	 *            the name of the column in histoTable to display
	 */
	public void updateAxes( String dataField )
	{

		// The extra variable defs are probably unneeded, but
		// date from the time I was trying to debug the
		// xaxis labelling bug. Left in to make future
		// debugging easier.
		AxisLayout xaxis = getXAxis();
		AxisLayout yaxis = getYAxis();
		AxisLabelLayout xlabels = getXLabels();
		AxisLabelLayout ylabels = getYLabels();

		xaxis.setScale( Constants.LINEAR_SCALE );
		xaxis.setDataField( dataField );

		xaxis.setDataType( getAxisType( dataField ) );
		xlabels.setRangeModel( null );  // setting to null seems to force a recalc -> redraw

		// yaxis is the bin counts, which are always numeric
		String countField = HistogramTable.getCountField( dataField );
		yaxis.setDataField( countField );
		// I could set the range model to null as above, but with histograms,
		// you really want the bars to go from 0 to max, not from min to max. -- KDS
		NumberRangeModel rangeModel = new NumberRangeModel(
			0, m_allDataHistoTable.getCountMax( dataField ), 0,
			m_allDataHistoTable.getCountMax( dataField )
		);
		yaxis.setRangeModel( rangeModel );
		ylabels.setRangeModel( rangeModel );

		m_vis.run( "draw" );
		Utils.waitUntilActivitiesAreFinished();
	}


	/**
	 * @param dataField
	 *            the name of a column in histoTable to display
	 * @return isNumeric boolean which says if the column named by dataField
	 *         is int, float, or double or if it is not. Note that booleans are
	 *         treated as non-numerics under this logic.
	 */
	private boolean isNumeric( String dataField )
	{
		return m_currentHistoTable.getColumn( dataField ).canGetDouble();
	}

	/**
	 * @param dataField
	 *            the name of a column to display
	 * @return the type of the axis (NUMERICAL for numbers and ORDINAL for strings)
	 *         Note that HistogramGraph hasn't been tested with boolean or derived fields.
	 *         I believe that HistogramTable treats booleans as strings. -- KDS
	 */
	protected int getAxisType( String dataField )
	{
		if ( isNumeric( dataField ) ) {
			return Constants.NUMERICAL;
		}
		else {     // completely untested with derived columns or strings
			return Constants.ORDINAL;
		}
	}

	/**
	 * @param startingField
	 * @return either the input or the first field in the data table
	 */
	protected String getStartingField( String startingField )
	{
		if ( null == startingField ) {
			startingField = m_currentHistoTable.getColumnName( 0 );
		}
		return startingField;
	}

	// These getters were purely for help debugging the axis problem.
	// As I haven't chased that down completely, I've left them in. -- KDS
	protected AxisLayout getXAxis()
	{
		return m_xAxis;
	}

	protected AxisLayout getYAxis()
	{
		return m_yAxis;
	}

	protected AxisLabelLayout getXLabels()
	{
		return m_xLabels;
	}

	protected AxisLabelLayout getYLabels()
	{
		return m_yLabels;
	}

	protected HistogramTable getHistoTable()
	{
		return m_currentHistoTable;
	}


	public static int getDefaultBinCount()
	{
		return DEFAULT_BIN_COUNT;
	}

	public static void setAllDataHistoTable( HistogramTable m_allDataHistoTable )
	{
		HistogramGraph.m_allDataHistoTable = m_allDataHistoTable;
	}
}
