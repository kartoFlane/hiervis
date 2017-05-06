package pl.pwr.hiervis.prefuse.visualization;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import prefuse.render.AbstractShapeRenderer;
import prefuse.visual.VisualItem;


public class NodeRenderer extends AbstractShapeRenderer
{
	protected Ellipse2D ellipse = new Ellipse2D.Double();
	protected int nodeSize;


	public NodeRenderer( int nodeSize )
	{
		this.nodeSize = nodeSize;
	}

	@Override
	protected Shape getRawShape( VisualItem item )
	{
		ellipse.setFrame(
			item.getX() - nodeSize * 0.5,
			item.getY() - nodeSize * 0.5,
			nodeSize, nodeSize
		);

		return ellipse;
	}
}
