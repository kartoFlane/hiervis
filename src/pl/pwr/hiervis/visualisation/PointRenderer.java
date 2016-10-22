package pl.pwr.hiervis.visualisation;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import pl.pwr.hiervis.core.ElementRole;
import pl.pwr.hiervis.core.HVConfig;
import prefuse.render.AbstractShapeRenderer;
import prefuse.visual.VisualItem;


public class PointRenderer extends AbstractShapeRenderer {
	private HVConfig config;

	protected Ellipse2D m_box = new Ellipse2D.Double();
	protected int pointSize;


	public PointRenderer( int pointSize, HVConfig config ) {
		this.config = config;
		this.pointSize = pointSize;
	}

	@Override
	protected Shape getRawShape( VisualItem item ) {
		m_box.setFrame(
				item.getX() - pointSize * 0.5,
				item.getY() - pointSize * 0.5,
				pointSize, pointSize );

		// int role = item.getInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME );
		int role = 0;

		// item.setStrokeColor( Color.black.getRGB() );
		item.setFillColor( Color.red.getRGB() );

		if ( role == ElementRole.CURRENT.getNumber() ) {
			item.setFillColor( config.getCurrentLevelColor().getRGB() );
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

		return m_box;
	}
}
