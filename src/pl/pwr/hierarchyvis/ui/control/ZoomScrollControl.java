package pl.pwr.hierarchyvis.ui.control;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import prefuse.Display;
import prefuse.controls.ControlAdapter;
import prefuse.visual.VisualItem;


public class ZoomScrollControl extends ControlAdapter {

	@Override
	public void itemWheelMoved( VisualItem item, MouseWheelEvent e ) {
		mouseWheelMoved( e );
	}

	@Override
	public void mousePressed( MouseEvent e ) {
		Display d = (Display)e.getSource();
		d.requestFocus();
	}

	@Override
	public void mouseWheelMoved( MouseWheelEvent e ) {
		Display d = (Display)e.getSource();
		d.requestFocus();

		final double zoomStep = 0.1;
		final double zoomMin = 0.1;
		final double zoomMax = 10;

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
	public void keyPressed( KeyEvent e ) {
		if ( e.isControlDown() && e.getKeyCode() == KeyEvent.VK_NUMPAD0 ) {
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
					rect.getCenterY() );

			d.animatePanAndZoomToAbs( p, 1 / d.getScale(), 500 );
		}
	}
}
