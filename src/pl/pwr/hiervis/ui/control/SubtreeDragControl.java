package pl.pwr.hiervis.ui.control;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.util.Iterator;

import prefuse.Display;
import prefuse.controls.Control;
import prefuse.controls.ControlAdapter;
import prefuse.controls.DragControl;
import prefuse.util.ui.UILib;
import prefuse.visual.NodeItem;
import prefuse.visual.VisualItem;


/**
 * Control that changes the location of a whole subtree when dragged on screen.
 * This is similar to the {@link DragControl DragControl} class, except that it
 * moves the entire visible subtree rooted at an item, rather than just the
 * item itself.
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @author Tomasz Bachmiñski - modified to include customizable mouse button
 */
public class SubtreeDragControl extends ControlAdapter {

	private Point2D down = new Point2D.Double();
	private Point2D tmp = new Point2D.Double();
	private boolean wasFixed;
	private int m_button;


	/**
	 * Creates a new subtree drag control that issues repaint requests as an
	 * item is dragged.
	 */
	public SubtreeDragControl() {
	}

	/**
	 * Creates a new subtree drag control that issues repaint requests as an
	 * item is dragged.
	 * 
	 * @param mouseButton
	 *            the mouse button that should initiate a pan. One of
	 *            {@link Control#LEFT_MOUSE_BUTTON}, {@link Control#MIDDLE_MOUSE_BUTTON},
	 *            or {@link Control#RIGHT_MOUSE_BUTTON}.
	 */
	public SubtreeDragControl( int mouseButton ) {
		m_button = mouseButton;
	}

	/**
	 * @see Control#itemEntered(VisualItem, MouseEvent)
	 */
	public void itemEntered( VisualItem item, MouseEvent e ) {
		if ( !( item instanceof NodeItem ) ) return;
		Display d = (Display)e.getSource();
		d.setCursor( Cursor.getPredefinedCursor( Cursor.HAND_CURSOR ) );
	}

	/**
	 * @see Control#itemExited(VisualItem, MouseEvent)
	 */
	public void itemExited( VisualItem item, MouseEvent e ) {
		if ( !( item instanceof NodeItem ) ) return;
		Display d = (Display)e.getSource();
		d.setCursor( Cursor.getDefaultCursor() );
	}

	/**
	 * @see Control#itemPressed(VisualItem, MouseEvent)
	 */
	public void itemPressed( VisualItem item, MouseEvent e ) {
		if ( !UILib.isButtonPressed( e, m_button ) ) return;
		if ( !( item instanceof NodeItem ) ) return;
		Display d = (Display)e.getComponent();
		down = d.getAbsoluteCoordinate( e.getPoint(), down );
		wasFixed = item.isFixed();
		item.setFixed( true );
	}

	/**
	 * @see Control#itemReleased(VisualItem, MouseEvent)
	 */
	public void itemReleased( VisualItem item, MouseEvent e ) {
		if ( !UILib.isButtonPressed( e, m_button ) ) return;
		if ( !( item instanceof NodeItem ) ) return;
		item.setFixed( wasFixed );
	}

	/**
	 * @see Control#itemDragged(VisualItem, MouseEvent)
	 */
	public void itemDragged( VisualItem item, MouseEvent e ) {
		if ( !UILib.isButtonPressed( e, m_button ) ) return;
		if ( !( item instanceof NodeItem ) ) return;
		Display d = (Display)e.getComponent();
		tmp = d.getAbsoluteCoordinate( e.getPoint(), tmp );
		double dx = tmp.getX() - down.getX();
		double dy = tmp.getY() - down.getY();
		updateLocations( (NodeItem)item, dx, dy );
		down.setLocation( tmp );
		item.getVisualization().repaint();
	}

	private void updateLocations( NodeItem n, double dx, double dy ) {
		double x = n.getX(), y = n.getY();
		n.setStartX( x );
		n.setStartY( y );
		x += dx;
		y += dy;
		n.setX( x );
		n.setY( y );
		n.setEndX( x );
		n.setEndY( y );

		Iterator<?> children = n.children();
		while ( children.hasNext() )
			updateLocations( (NodeItem)children.next(), dx, dy );
	}

} // end of class SubtreeDragControl
