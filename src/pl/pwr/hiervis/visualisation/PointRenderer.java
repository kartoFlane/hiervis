package pl.pwr.hiervis.visualisation;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;

import pl.pwr.hiervis.core.ElementRole;
import pl.pwr.hiervis.core.HVConfig;
import pl.pwr.hiervis.core.HVConstants;
import prefuse.Constants;
import prefuse.render.AbstractShapeRenderer;
import prefuse.visual.VisualItem;


public class PointRenderer extends AbstractShapeRenderer
{
	private HVConfig config;

	protected Ellipse2D ellipse = new Ellipse2D.Double();
	protected int pointSize;

	private StringRenderer stringRenderer;


	public PointRenderer( int pointSize, HVConfig config )
	{
		this.config = config;
		this.pointSize = pointSize;

		this.stringRenderer = new StringRenderer();
		this.stringRenderer.setHorizontalAlignment( Constants.LEFT );
		this.stringRenderer.setVerticalAlignment( Constants.BOTTOM );
		// this.stringRenderer.setTextField( HVConstants.PREFUSE_NODE_LABEL_COLUMN_NAME );
	}

	@Override
	protected Shape getRawShape( VisualItem item )
	{
		ellipse.setFrame(
			item.getX() - pointSize * 0.5,
			item.getY() - pointSize * 0.5,
			pointSize, pointSize
		);

		int role = item.getInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME );
		item.setTextColor( Color.black.getRGB() );

		if ( role == ElementRole.CURRENT.getNumber() ) {
			item.setFillColor( config.getCurrentGroupColor().getRGB() );
		}
		else if ( role == ElementRole.DIRECT_PARENT.getNumber() ) {
			item.setFillColor( config.getParentGroupColor().getRGB() );
		}
		else if ( role == ElementRole.INDIRECT_PARENT.getNumber() ) {
			item.setFillColor( config.getAncestorGroupColor().getRGB() );
		}
		else if ( role == ElementRole.CHILD.getNumber() ) {
			item.setFillColor( config.getChildGroupColor().getRGB() );
		}
		else if ( role == ElementRole.OTHER.getNumber() ) {
			item.setFillColor( config.getOtherGroupColor().getRGB() );
		}

		if ( stringRenderer.getText( item ) == null ) {
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
		// stringRenderer.render( g, item );
	}
}
