package pl.pwr.hiervis.ui.control;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import pl.pwr.hiervis.util.Utils;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.controls.AbstractZoomControl;
import prefuse.controls.WheelZoomControl;
import prefuse.visual.VisualItem;


/**
 * A much more customizable variant of {@link WheelZoomControl }.
 * 
 * @author Tomasz Bachmiński
 *
 */
public class ZoomScrollControl extends AbstractZoomControl
{
	private double zoomStep;
	private int zoomDirection = -1;
	private int modifierMask = 0;


	public ZoomScrollControl()
	{
		this( 0.1, 10, 0.1 );
	}

	public ZoomScrollControl( double min, double max, double step )
	{
		setZoomOverItem( true );
		setScrollDownZoomsOut( true );
		setMinScale( min );
		setMaxScale( max );
		setZoomStep( step );
	}

	/**
	 * @return the value the zoom will be incremented by each time the user scrolls.
	 */
	public double getZoomStep()
	{
		return zoomStep;
	}

	/**
	 * @param step
	 *            the value the zoom will be incremented by each time the user scrolls.
	 */
	public void setZoomStep( double step )
	{
		if ( step <= 0 ) {
			throw new IllegalArgumentException(
				String.format( "step must be a positive number (%s)", step )
			);
		}

		zoomStep = step;
	}

	/**
	 * @return true if scrolling the mouse wheel down causes the display to zoom out, false otherwise.
	 */
	public boolean getScrollDownZoomsOut()
	{
		return zoomDirection == -1;
	}

	/**
	 * Sets the zoom behavior in response to mouse scrolls.
	 * 
	 * @param scrollDownZoomsOut
	 *            If true, scrolling the mouse wheel down will cause the display to zoom out.
	 *            If false, scrolling the mouse wheel down will cause the display to zoom in.
	 */
	public void setScrollDownZoomsOut( boolean scrollDownZoomsOut )
	{
		zoomDirection = scrollDownZoomsOut ? -1 : 1;
	}

	/**
	 * @return whether the Alt key has to be held down while scrolling for zooming to occur.
	 */
	public boolean isModifierAlt()
	{
		return ( modifierMask & InputEvent.ALT_DOWN_MASK ) != 0;
	}

	/**
	 * @return whether the Shift key has to be held down while scrolling for zooming to occur.
	 */
	public boolean isModifierShift()
	{
		return ( modifierMask & InputEvent.SHIFT_DOWN_MASK ) != 0;
	}

	/**
	 * @return whether the Control key has to be held down while scrolling for zooming to occur.
	 */
	public boolean isModifierControl()
	{
		return ( modifierMask & InputEvent.CTRL_DOWN_MASK ) != 0;
	}

	/**
	 * @param alt
	 *            if true, Alt key will need to be held down
	 */
	public void setModifierAlt( boolean alt )
	{
		modifierMask = alt
			? ( modifierMask | InputEvent.ALT_DOWN_MASK )
			: ( modifierMask & ~InputEvent.ALT_DOWN_MASK );
	}

	/**
	 * @param shift
	 *            if true, Shift key will need to be held down
	 */
	public void setModifierShift( boolean shift )
	{
		modifierMask = shift
			? ( modifierMask | InputEvent.SHIFT_DOWN_MASK )
			: ( modifierMask & ~InputEvent.SHIFT_DOWN_MASK );
	}

	/**
	 * @param ctrl
	 *            if true, Control key will need to be held down
	 */
	public void setModifierControl( boolean ctrl )
	{
		modifierMask = ctrl
			? ( modifierMask | InputEvent.CTRL_DOWN_MASK )
			: ( modifierMask & ~InputEvent.CTRL_DOWN_MASK );
	}

	/**
	 * Sets modifiers that need to be down while scrolling for zooming to occur.
	 * 
	 * @param alt
	 *            if true, Alt key will need to be held down
	 * @param shift
	 *            if true, Shift key will need to be held down
	 * @param ctrl
	 *            if true, Control key will need to be held down
	 */
	public void setModifiers( boolean alt, boolean shift, boolean ctrl )
	{
		modifierMask = 0
			| ( alt ? InputEvent.ALT_DOWN_MASK : 0 )
			| ( shift ? InputEvent.SHIFT_DOWN_MASK : 0 )
			| ( ctrl ? InputEvent.CTRL_DOWN_MASK : 0 );
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
		if ( ( e.getModifiersEx() & modifierMask ) == modifierMask ) {
			Display d = (Display)e.getComponent();
			d.requestFocus();

			double zoomDelta = 1 + zoomDirection * zoomStep * e.getWheelRotation();
			zoom( d, e.getPoint(), zoomDelta, false );
		}
	}

	@Override
	public void keyPressed( KeyEvent e )
	{
		// TODO: Remove this from here. Instead, make this a keybind that's bound to Frame housing the display
		// using SwingUIUtils.installOperation
		if ( e.isControlDown() && ( e.getKeyCode() == KeyEvent.VK_NUMPAD0 || e.getKeyCode() == KeyEvent.VK_0 ) ) {
			Utils.fitToBounds( (Display)e.getComponent(), Visualization.ALL_ITEMS, 0, 500 );
		}
	}
}
