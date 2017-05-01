package pl.pwr.hiervis.prefuse.control;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;

import prefuse.Display;
import prefuse.controls.Control;
import prefuse.controls.ControlAdapter;
import prefuse.util.ui.UILib;
import prefuse.visual.VisualItem;


/**
 * Pans the display, changing the viewable region of the visualization.
 * By default, panning is accomplished by clicking on the background of a
 * visualization with the left mouse button and then dragging.
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @author Tomasz Bachmiński - modified to add filtering by VisualItem class
 */
public class PanControl extends ControlAdapter
{
	private boolean m_panOverItem;
	private int m_xDown, m_yDown;
	private int m_button;
	private Class<?>[] m_classes = null;


	/**
	 * Create a new PanControl.
	 */
	public PanControl()
	{
		this( LEFT_MOUSE_BUTTON, false );
	}

	/**
	 * Create a new PanControl.
	 * 
	 * @param panOverItem
	 *            if true, the panning control will work even while
	 *            the mouse is over a visual item.
	 */
	public PanControl( boolean panOverItem )
	{
		this( LEFT_MOUSE_BUTTON, panOverItem );
	}

	/**
	 * Create a new PanControl.
	 * 
	 * @param mouseButton
	 *            the mouse button that should initiate a pan. One of
	 *            {@link Control#LEFT_MOUSE_BUTTON}, {@link Control#MIDDLE_MOUSE_BUTTON},
	 *            or {@link Control#RIGHT_MOUSE_BUTTON}.
	 */
	public PanControl( int mouseButton )
	{
		this( mouseButton, false );
	}

	/**
	 * Create a new PanControl
	 * 
	 * @param mouseButton
	 *            the mouse button that should initiate a pan. One of
	 *            {@link Control#LEFT_MOUSE_BUTTON}, {@link Control#MIDDLE_MOUSE_BUTTON},
	 *            or {@link Control#RIGHT_MOUSE_BUTTON}.
	 * @param panOverItem
	 *            if true, the panning control will work even while
	 *            the mouse is over a visual item.
	 */
	public PanControl( int mouseButton, boolean panOverItem )
	{
		m_button = mouseButton;
		m_panOverItem = panOverItem;
	}

	/**
	 * Create a new PanControl. Using this constructor sets panOverItem to true.
	 * 
	 * @param classes
	 *            array of {@link VisualItem} classes that can be panned over.
	 */
	public PanControl( Class<?>[] classes )
	{
		this( LEFT_MOUSE_BUTTON, classes );
	}

	/**
	 * Create a new PanControl. Using this constructor sets panOverItem to true.
	 * 
	 * @param mouseButton
	 *            the mouse button that should initiate a pan. One of
	 *            {@link Control#LEFT_MOUSE_BUTTON}, {@link Control#MIDDLE_MOUSE_BUTTON},
	 *            or {@link Control#RIGHT_MOUSE_BUTTON}.
	 * @param classes
	 *            array of {@link VisualItem} classes that can be panned over.
	 */
	public PanControl( int mouseButton, Class<?>[] classes )
	{
		m_button = mouseButton;
		m_panOverItem = true;
		m_classes = classes;
	}

	// ------------------------------------------------------------------------

	/**
	 * @see MouseListener#mousePressed(MouseEvent)
	 */
	public void mousePressed( MouseEvent e )
	{
		if ( UILib.isButtonPressed( e, m_button ) ) {
			e.getComponent().setCursor( Cursor.getPredefinedCursor( Cursor.MOVE_CURSOR ) );
			m_xDown = e.getX();
			m_yDown = e.getY();
		}
	}

	/**
	 * @see MouseMotionListener#mouseDragged(MouseEvent)
	 */
	public void mouseDragged( MouseEvent e )
	{
		if ( UILib.isButtonPressed( e, m_button ) ) {
			Display display = (Display)e.getComponent();
			int x = e.getX(), y = e.getY();
			int dx = x - m_xDown, dy = y - m_yDown;
			display.pan( dx, dy );
			m_xDown = x;
			m_yDown = y;
			display.repaint();
		}
	}

	/**
	 * @see MouseListener#mouseReleased(MouseEvent)
	 */
	public void mouseReleased( MouseEvent e )
	{
		if ( UILib.isButtonPressed( e, m_button ) ) {
			e.getComponent().setCursor( Cursor.getDefaultCursor() );
			m_xDown = -1;
			m_yDown = -1;
		}
	}

	/**
	 * @see Control#itemPressed(VisualItem, MouseEvent)
	 */
	public void itemPressed( VisualItem item, MouseEvent e )
	{
		if ( m_panOverItem && !instanceofAny( item, m_classes ) )
			mousePressed( e );
	}

	/**
	 * @see Control#itemDragged(VisualItem, MouseEvent)
	 */
	public void itemDragged( VisualItem item, MouseEvent e )
	{
		if ( m_panOverItem && !instanceofAny( item, m_classes ) )
			mouseDragged( e );
	}

	/**
	 * @see Control#itemReleased(VisualItem, MouseEvent)
	 */
	public void itemReleased( VisualItem item, MouseEvent e )
	{
		if ( m_panOverItem && !instanceofAny( item, m_classes ) )
			mouseReleased( e );
	}

	private static boolean instanceofAny( Object o, Class<?>[] classes )
	{
		if ( classes != null && o != null ) {
			for ( Class<?> c : classes ) {
				if ( c.isInstance( o ) )
					return true;
			}
		}
		return false;
	}
}
