package pl.pwr.hiervis.hierarchy;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

import pl.pwr.hiervis.prefuse.histogram.HistogramGraph;
import pl.pwr.hiervis.util.Utils;
import prefuse.Display;


/**
 * A simple class for remembering visualization states.
 * 
 * @author Tomasz Bachmiński
 *
 */
public class VisualizationState
{
	private double resWidth;
	private double resHeight;
	private AffineTransform transform;


	public VisualizationState()
	{
		resWidth = -1;
		resHeight = -1;
		this.transform = new AffineTransform();
	}

	/**
	 * Updates this {@link VisualizationState} to represent the state of the specified display's visualization.
	 * 
	 * @param d
	 *            the display whose visualization's state is to be stored
	 */
	public void store( Display d )
	{
		if ( d instanceof HistogramGraph == false ) {
			Rectangle2D bounds = HierarchyProcessor.getLayoutBounds( d.getVisualization() );

			if ( bounds.getX() > 0 ) {
				resetResolution();
			}
			else {
				setResolution( bounds.getWidth(), bounds.getHeight() );
			}
		}

		setTransform( d.getTransform() );
	}

	/**
	 * Applies this {@link VisualizationState} to the specified display.
	 * 
	 * @param d
	 *            the display to apply the visualization state to
	 */
	public void applyTo( Display d )
	{
		if ( d == null )
			throw new IllegalArgumentException( "Display must not ne null!" );

		Utils.setTransform( d, transform );

		if ( d instanceof HistogramGraph == false ) {
			HierarchyProcessor.updateLayoutBounds( d.getVisualization(), getResolutionRect() );
		}
	}

	// --------------------------------------------------------------------------------------

	public void setResolutionWidth( double width )
	{
		this.resWidth = width;
	}

	public void setResolutionHeight( double height )
	{
		this.resHeight = height;
	}

	public void setResolution( double width, double height )
	{
		this.resWidth = width;
		this.resHeight = height;
	}

	public void resetResolution()
	{
		this.resWidth = -1;
		this.resHeight = -1;
	}

	public void setTransform( AffineTransform t )
	{
		if ( t == null )
			throw new IllegalArgumentException( "Transform must not be null!" );
		this.transform = t;
	}

	public double getResolutionWidth()
	{
		return resWidth;
	}

	public double getResolutionHeight()
	{
		return resHeight;
	}

	public Rectangle2D getResolutionRect()
	{
		if ( resWidth < 0 || resHeight < 0 ) {
			return null;
		}
		else {
			return new Rectangle2D.Double( 0, 0, resWidth, resHeight );
		}
	}

	public AffineTransform getTransform()
	{
		return transform;
	}
}
