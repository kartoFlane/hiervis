package pl.pwr.hiervis.ui.control;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import prefuse.Display;
import prefuse.controls.ControlAdapter;
import prefuse.visual.VisualItem;


/**
 * A prefuse control which allows the user to zoom a display in and out.
 * The zoom bounds, as well as zooming step, can be customized.
 * 
 * @author Tomasz Bachmiński
 *
 */
public class ZoomScrollControl extends ControlAdapter
{
	private double zoomMin;
	private double zoomMax;
	private double zoomStep;


	public ZoomScrollControl()
	{
		this( 0.1, 10, 0.1 );
	}

	public ZoomScrollControl( double min, double max, double step )
	{
		if ( min <= 0 ) {
			throw new IllegalArgumentException(
				String.format( "min must be a positive number (%s)", min )
			);
		}
		if ( max <= 0 ) {
			throw new IllegalArgumentException(
				String.format( "max must be a positive number (%s)", max )
			);
		}
		if ( step <= 0 ) {
			throw new IllegalArgumentException(
				String.format( "step must be a positive number (%s)", step )
			);
		}
		if ( min > max ) {
			throw new IllegalArgumentException(
				String.format( "max must be greater than or equal to min (%s <= %s)", min, max )
			);
		}

		zoomMin = min;
		zoomMax = max;
		zoomStep = step;
	}

	public double getZoomMin()
	{
		return zoomMin;
	}

	public void setZoomMin( double min )
	{
		if ( min <= 0 ) {
			throw new IllegalArgumentException(
				String.format( "min must be a positive number (%s)", min )
			);
		}
		if ( min > zoomMax ) {
			throw new IllegalArgumentException(
				String.format( "max must be greater than or equal to min (%s <= %s)", min, zoomMax )
			);
		}

		zoomMin = min;
	}

	public double getZoomMax()
	{
		return zoomMax;
	}

	public void setZoomMax( double max )
	{
		if ( max <= 0 ) {
			throw new IllegalArgumentException(
				String.format( "max must be a positive number (%s)", max )
			);
		}
		if ( zoomMin > max ) {
			throw new IllegalArgumentException(
				String.format( "max must be greater than or equal to min (%s <= %s)", zoomMin, max )
			);
		}

		zoomMax = max;
	}

	public void setZoomMinMax( double min, double max )
	{
		if ( min <= 0 ) {
			throw new IllegalArgumentException(
				String.format( "min must be a positive number (%s)", min )
			);
		}
		if ( max <= 0 ) {
			throw new IllegalArgumentException(
				String.format( "max must be a positive number (%s)", max )
			);
		}
		if ( min > max ) {
			throw new IllegalArgumentException(
				String.format( "max must be greater than or equal to min (%s <= %s)", min, max )
			);
		}

		zoomMin = min;
		zoomMax = max;
	}

	public double getZoomStep()
	{
		return zoomStep;
	}

	public void setZoomStep( double step )
	{
		if ( step <= 0 ) {
			throw new IllegalArgumentException(
				String.format( "step must be a positive number (%s)", step )
			);
		}

		zoomStep = step;
	}

	@Override
	public void itemWheelMoved( VisualItem item, MouseWheelEvent e )
	{
		mouseWheelMoved( e );
	}

	@Override
	public void mousePressed( MouseEvent e )
	{
		Display d = (Display)e.getSource();
		d.requestFocus();
	}

	@Override
	public void mouseWheelMoved( MouseWheelEvent e )
	{
		Display d = (Display)e.getSource();
		d.requestFocus();

		double dz = -e.getWheelRotation() * zoomStep;
		double hz = d.getTransform().getScaleX();

		hz *= ( 1 + dz );

		// Clamp zoom level to the min/max values
		if ( hz >= zoomMin && hz <= zoomMax ) {
			d.zoom( e.getPoint(), 1 + dz );
			d.repaint();
		}
	}

	@Override
	public void keyPressed( KeyEvent e )
	{
		if ( e.isControlDown() && ( e.getKeyCode() == KeyEvent.VK_NUMPAD0 || e.getKeyCode() == KeyEvent.VK_0 ) ) {
			Display d = (Display)e.getSource();

			Rectangle2D rect = null;
			if ( d.getVisibleItemCount() == 0 ) {
				// If the display has no visual items, then just use its dimensions directly
				rect = new Rectangle2D.Double( 0, 0, d.getWidth(), d.getHeight() );
			}
			else {
				// If the display does have some visual items, then use their bounding box
				rect = d.getItemBounds();
			}

			Point2D p = new Point2D.Double(
				rect.getCenterX(),
				rect.getCenterY()
			);

			d.animatePanAndZoomToAbs( p, 1 / d.getScale(), 500 );
		}
	}
}
