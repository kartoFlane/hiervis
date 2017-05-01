package pl.pwr.hiervis.prefuse.visualization;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.RectangularShape;

import pl.pwr.hiervis.core.HVConstants;
import prefuse.Constants;
import prefuse.render.AbstractShapeRenderer;
import prefuse.visual.VisualItem;


public class PointRenderer extends AbstractShapeRenderer
{
	private final StringRenderer stringRenderer;
	private final RectangularShape shape;

	private boolean drawLabels = false;


	public PointRenderer( RectangularShape shape )
	{
		this.shape = shape;

		this.stringRenderer = new StringRenderer();
		this.stringRenderer.setHorizontalAlignment( Constants.LEFT );
		this.stringRenderer.setVerticalAlignment( Constants.BOTTOM );
		this.stringRenderer.setTextField( HVConstants.PREFUSE_INSTANCE_LABEL_COLUMN_NAME );
	}

	@Override
	protected Shape getRawShape( VisualItem item )
	{
		shape.setFrame(
			item.getX() - shape.getWidth() * 0.5,
			item.getY() - shape.getHeight() * 0.5,
			shape.getWidth(), shape.getHeight()
		);

		if ( !drawLabels || stringRenderer.getText( item ) == null ) {
			return shape;
		}
		else {
			Area a = new Area( stringRenderer.getRawShape( item ) );
			a.add( new Area( shape ) );
			return a;
		}
	}

	/**
	 * @see prefuse.render.Renderer#render(java.awt.Graphics2D, prefuse.visual.VisualItem)
	 */
	public void render( Graphics2D g, VisualItem item )
	{
		super.render( g, item );
		if ( drawLabels )
			stringRenderer.render( g, item );
	}
}
