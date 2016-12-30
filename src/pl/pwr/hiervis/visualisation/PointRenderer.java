package pl.pwr.hiervis.visualisation;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;

import pl.pwr.hiervis.core.HVConstants;
import prefuse.Constants;
import prefuse.render.AbstractShapeRenderer;
import prefuse.visual.VisualItem;


public class PointRenderer extends AbstractShapeRenderer
{
	protected int pointSize;

	private StringRenderer stringRenderer;
	private boolean drawLabels = false;


	public PointRenderer( int pointSize )
	{
		this.pointSize = pointSize;

		this.stringRenderer = new StringRenderer();
		this.stringRenderer.setHorizontalAlignment( Constants.LEFT );
		this.stringRenderer.setVerticalAlignment( Constants.BOTTOM );
		this.stringRenderer.setTextField( HVConstants.PREFUSE_INSTANCE_LABEL_COLUMN_NAME );
	}

	@Override
	protected Shape getRawShape( VisualItem item )
	{
		Ellipse2D ellipse = new Ellipse2D.Double(
			item.getX() - pointSize * 0.5,
			item.getY() - pointSize * 0.5,
			pointSize, pointSize
		);

		if ( !drawLabels || stringRenderer.getText( item ) == null ) {
			return ellipse;
		}
		else {
			Area a = new Area( stringRenderer.getRawShape( item ) );
			a.add( new Area( ellipse ) );
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
