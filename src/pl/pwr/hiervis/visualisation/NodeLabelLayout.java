package pl.pwr.hiervis.visualisation;

import java.awt.geom.Rectangle2D;
import java.util.Iterator;

import prefuse.action.layout.Layout;
import prefuse.visual.DecoratorItem;
import prefuse.visual.VisualItem;


public class NodeLabelLayout extends Layout
{
	public NodeLabelLayout( String group )
	{
		super( group );
	}

	@Override
	public void run( double frac )
	{
		Iterator<?> iter = m_vis.items( m_group );

		while ( iter.hasNext() ) {
			DecoratorItem decorator = (DecoratorItem)iter.next();
			VisualItem decoratedItem = decorator.getDecoratedItem();
			Rectangle2D bounds = decoratedItem.getBounds();

			double x = bounds.getCenterX();
			double y = bounds.getCenterY();

			setX( decorator, null, x );
			setY( decorator, null, y );
		}
	}
}
