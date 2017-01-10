package pl.pwr.hiervis.ui.control;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import pl.pwr.hiervis.util.Utils;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.controls.AbstractZoomControl;
import prefuse.visual.VisualItem;


/**
 * A prefuse control which allows the user to zoom a display in and out, with customizable zooming step.
 * 
 * @author Tomasz Bachmiński
 *
 */
public class ZoomScrollControl extends AbstractZoomControl
{
	private double zoomStep;


	public ZoomScrollControl()
	{
		this( 0.1, 10, 0.1 );
	}

	public ZoomScrollControl( double min, double max, double step )
	{
		setZoomOverItem( true );
		setMinScale( min );
		setMaxScale( max );
		setZoomStep( step );
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
		if ( m_zoomOverItem )
			mouseWheelMoved( e );
	}

	@Override
	public void mousePressed( MouseEvent e )
	{
		Display d = (Display)e.getComponent();
		d.requestFocus();
	}

	@Override
	public void mouseWheelMoved( MouseWheelEvent e )
	{
		Display d = (Display)e.getComponent();
		d.requestFocus();
		zoom( d, e.getPoint(), 1 - zoomStep * e.getWheelRotation(), false );
	}

	@Override
	public void keyPressed( KeyEvent e )
	{
		if ( e.isControlDown() && ( e.getKeyCode() == KeyEvent.VK_NUMPAD0 || e.getKeyCode() == KeyEvent.VK_0 ) ) {
			Utils.fitToBounds( (Display)e.getSource(), Visualization.ALL_ITEMS, 0, 500 );
		}
	}
}
