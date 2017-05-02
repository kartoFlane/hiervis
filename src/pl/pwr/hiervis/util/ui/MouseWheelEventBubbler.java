package pl.pwr.hiervis.util.ui;

import java.awt.Component;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.function.Predicate;

import javax.swing.JScrollPane;


/**
 * A listener that bubbles the event to its parent scroll pane (if it has one)
 * if the predicate passed in constructor is true.
 * 
 * Based on: http://stackoverflow.com/a/1379695
 *
 */
public class MouseWheelEventBubbler implements MouseWheelListener
{
	private Component thisComponent;
	private JScrollPane parentScrollPane;
	private Predicate<MouseWheelEvent> predicate;


	public MouseWheelEventBubbler( Component thisComponent, Predicate<MouseWheelEvent> predicate )
	{
		this.thisComponent = thisComponent;
		this.predicate = predicate;
	}

	private JScrollPane getParentScrollPane()
	{
		if ( parentScrollPane == null ) {
			Component parent = thisComponent.getParent();
			while ( !( parent instanceof JScrollPane ) && parent != null ) {
				parent = parent.getParent();
			}
			parentScrollPane = (JScrollPane)parent;
		}

		return parentScrollPane;
	}

	public void mouseWheelMoved( MouseWheelEvent e )
	{
		JScrollPane parent = getParentScrollPane();
		if ( parent != null ) {
			if ( predicate.test( e ) ) {
				parent.dispatchEvent( cloneEvent( e, parent ) );
			}
		}
		else {
			/*
			 * If parent scrollpane doesn't exist, remove this as a listener.
			 * We have to defer this till now (vs doing it in constructor)
			 * because in the constructor this item has no parent yet.
			 */
			thisComponent.removeMouseWheelListener( this );
		}
	}

	private MouseWheelEvent cloneEvent( MouseWheelEvent e, Component newSource )
	{
		return new MouseWheelEvent(
			newSource, e.getID(), e.getWhen(),
			e.getModifiers(), 1, 1, e.getClickCount(),
			false, e.getScrollType(), e.getScrollAmount(),
			e.getWheelRotation()
		);
	}
}

