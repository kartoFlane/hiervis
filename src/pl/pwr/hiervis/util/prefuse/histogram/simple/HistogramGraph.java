package pl.pwr.hiervis.util.prefuse.histogram.simple;

/*
 * Adapted for HocusLocus by Ajish George <ajishg@gmail.com>
 * from code by
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @author <a href="http://webfoot.com/ducky.home.html">Kaitlin Duck Sherwood</a>
 *
 * See HistogramTable.java for details 
 */

import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;

import javax.swing.BorderFactory;

import prefuse.Constants;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.action.Action;
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
import prefuse.util.ColorLib;
import prefuse.util.GraphicsLib;
import prefuse.util.display.DisplayLib;
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
	protected static final String group = "data";

	// KDS -- I tend to make things protected instead of private so
	// that people can subclass them. I'm not sure that's the right
	// thing to do.
	protected Rectangle2D m_dataB = new Rectangle2D.Double();
	protected Rectangle2D m_xlabB = new Rectangle2D.Double();
	protected Rectangle2D m_ylabB = new Rectangle2D.Double();

	protected BarRenderer m_shapeR = new BarRenderer( 5 );

	protected HistogramTable m_histoTable;


	/**
	 * @param histoTable
	 *            a histogrammized version of dataTable
	 * @param startingField
	 *            the name of the field (column) of the data table
	 *            whose histogram is to be shown in the histogram graph.
	 */
	public HistogramGraph( HistogramTable histoTable, String startingField )
	{
		super( new Visualization() );

		m_histoTable = histoTable;
		startingField = getStartingField( startingField );

		// --------------------------------------------------------------------
		// STEP 1: setup the visualized data

		m_vis.addTable( group, m_histoTable );

		initializeRenderer();

		// --------------------------------------------------------------------
		// STEP 2: create actions to process the visual data

		ColorAction color = new ColorAction(
			group,
			VisualItem.FILLCOLOR, ColorLib.rgb( 255, 100, 100 )
		);
		m_vis.putAction( "color", color );

		ActionList draw = new ActionList();
		draw.add( initializeAxes( startingField ) );
		draw.add( color );
		draw.add( new RepaintAction() );
		m_vis.putAction( "draw", draw );

		// --------------------------------------------------------------------
		// STEP 3: set up a display and ui components to show the visualization

		initializeWindowCharacteristics();

		// --------------------------------------------------------------------
		// STEP 4: launching the visualization

		m_vis.run( "draw" );
	}

	public void setColorizeAction( ColorAction color )
	{
		Action oldColor = m_vis.removeAction( "color" );
		m_vis.putAction( "color", color );

		ActionList draw = (ActionList)m_vis.getAction( "draw" );
		draw.remove( oldColor );
		draw.add( 1, color );
	}

	public void setBarWidth( int width )
	{
		m_shapeR = new BarRenderer( width );
	}

	/**
	 * This sets up various things about the window, including the size.
	 * TODO include the size in the constructor
	 */
	private void initializeWindowCharacteristics()
	{
		setBorder( BorderFactory.createEmptyBorder() );

		// main display controls

		addComponentListener(
			new ComponentAdapter() {
				public void componentResized( ComponentEvent e )
				{
					Display display = (Display)e.getComponent();

					if ( !display.isTranformInProgress() ) {
						int margin = 10;

						String m_group = Visualization.ALL_ITEMS;
						Visualization vis = display.getVisualization();
						Rectangle2D bounds = vis.getBounds( m_group );
						GraphicsLib.expand( bounds, margin + (int)( 1 / display.getScale() ) );
						DisplayLib.fitViewToBounds( display, bounds, 0 );
					}
				}
			}
		);

		// addControlListener(new ZoomToFitControl());
		setHighQuality( true );

		initializeLayoutBoundsForDisplay();
		m_shapeR.setBounds( m_dataB );
	}

	/**
	 * @param fieldName
	 *            the name of the field (column) to display
	 */
	private ActionList initializeAxes( String fieldName )
	{
		AxisLayout xAxis = new AxisLayout(
			group, fieldName,
			Constants.X_AXIS, VisiblePredicate.TRUE
		);
		xAxis.setLayoutBounds( m_dataB );
		m_vis.putAction( "x", xAxis );

		String countField = HistogramTable.getCountField( fieldName );
		AxisLayout yAxis = new AxisLayout(
			group, countField,
			Constants.Y_AXIS, VisiblePredicate.TRUE
		);

		yAxis.setLayoutBounds( m_dataB );
		m_vis.putAction( "y", yAxis );

		DecimalFormat valueNumberFormat = new DecimalFormat();
		valueNumberFormat.setMaximumFractionDigits( 2 );
		valueNumberFormat.setMinimumFractionDigits( 2 );

		DecimalFormat countNumberFormat = new DecimalFormat();
		countNumberFormat.setMaximumFractionDigits( 0 );
		countNumberFormat.setMinimumFractionDigits( 0 );

		AxisLabelLayout xLabels = new AxisLabelLayout( "xlabels", xAxis, m_xlabB );
		xLabels.setNumberFormat( valueNumberFormat );
		m_vis.putAction( "xlabels", xLabels );

		AxisLabelLayout yLabels = new AxisLabelLayout( "ylabels", yAxis, m_ylabB );
		yLabels.setNumberFormat( countNumberFormat );
		m_vis.putAction( "ylabels", yLabels );

		updateAxes( fieldName, xAxis, yAxis, xLabels, yLabels );

		ActionList axes = new ActionList();
		axes.add( xAxis );
		axes.add( yAxis );
		axes.add( xLabels );
		axes.add( yLabels );

		return axes;
	}

	private void initializeRenderer()
	{
		m_vis.setRendererFactory(
			new RendererFactory() {
				Renderer xAxisRenderer = new AxisRenderer( Constants.CENTER, Constants.FAR_BOTTOM );
				Renderer yAxisRenderer = new AxisRenderer( Constants.FAR_LEFT, Constants.CENTER );
				Renderer barRenderer = new PolygonRenderer( Constants.POLY_TYPE_LINE );


				public Renderer getRenderer( VisualItem item )
				{
					if ( item.isInGroup( "xlabels" ) )
						return xAxisRenderer;
					if ( item.isInGroup( "ylabels" ) )
						return yAxisRenderer;
					if ( item.isInGroup( "barchart" ) )
						return barRenderer;
					return m_shapeR;
				}
			}
		);
	}

	public void initializeLayoutBoundsForDisplay()
	{
		Insets i = getInsets();
		int w = getWidth();
		int h = getHeight();
		int insetWidth = i.left + i.right;
		int insetHeight = i.top + i.bottom;
		int yAxisWidth = 5;
		int xAxisHeight = 5;

		m_dataB.setRect( i.left, i.top, w - insetWidth - yAxisWidth, h - insetHeight - xAxisHeight );
		m_xlabB.setRect( i.left, h - xAxisHeight - i.bottom, w - insetWidth - yAxisWidth, xAxisHeight );
		m_ylabB.setRect( i.left, i.top, w - insetWidth, h - insetHeight - xAxisHeight );

		m_vis.run( "update" );
	}

	/**
	 * @param dataField
	 *            the name of the column in histoTable to display
	 */
	private void updateAxes(
		String dataField,
		AxisLayout xAxis, AxisLayout yAxis,
		AxisLabelLayout xLabel, AxisLabelLayout yLabel )
	{
		xAxis.setScale( Constants.LINEAR_SCALE );
		xAxis.setDataField( dataField );
		xAxis.setDataType( getAxisType( dataField ) );

		double min = m_histoTable.getBinMin( dataField );
		double max = m_histoTable.getBinMax( dataField );
		NumberRangeModel xrangeModel = new NumberRangeModel( min, max, min, max );
		xLabel.setRangeModel( null );  // setting to null seems to force a recalc -> redraw
		xLabel.setRangeModel( xrangeModel );

		// yaxis is the bin counts, which are always numeric
		yAxis.setDataField( HistogramTable.getCountField( dataField ) );
		yAxis.setScale( Constants.LINEAR_SCALE );
		NumberRangeModel rangeModel = new NumberRangeModel(
			0, m_histoTable.getCountMax( dataField ),
			0, m_histoTable.getCountMax( dataField )
		);
		yAxis.setRangeModel( rangeModel );
		yLabel.setRangeModel( rangeModel );

		m_vis.run( "draw" );
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
		return m_histoTable.getColumn( dataField ).canGetDouble();
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
		if ( startingField == null ) {
			startingField = m_histoTable.getColumnName( 0 );
		}
		return startingField;
	}

	protected HistogramTable getHistoTable()
	{
		return m_histoTable;
	}
}
