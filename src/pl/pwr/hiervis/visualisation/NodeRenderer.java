package pl.pwr.hiervis.visualisation;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import pl.pwr.hiervis.core.ElementRole;
import pl.pwr.hiervis.core.HVConfig;
import pl.pwr.hiervis.core.HVConstants;
import prefuse.render.AbstractShapeRenderer;
import prefuse.visual.VisualItem;


public class NodeRenderer extends AbstractShapeRenderer {
	protected HVConfig params;

	protected Ellipse2D m_box = new Ellipse2D.Double();
	protected int nodeSize;


	public NodeRenderer( int nodeSize, HVConfig params ) {
		this.params = params;
		this.nodeSize = nodeSize;
	}

	@Override
	protected Shape getRawShape( VisualItem item ) {
		m_box.setFrame(
				item.getX() - nodeSize * 0.5,
				item.getY() - nodeSize * 0.5,
				nodeSize, nodeSize );

		// (Integer) item.get("age")/3, (Integer) item.get("age")/3);
		int role = item.getInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME );

		// item.setStrokeColor( Color.black.getRGB() );

		if ( role == ElementRole.CURRENT.getNumber() ) {
			item.setFillColor( params.getCurrentLevelColor().getRGB() );
		}
		else if ( role == ElementRole.DIRECT_PARENT.getNumber() ) {
			item.setFillColor( params.getParentGroupColor().getRGB() );
		}
		else if ( role == ElementRole.INDIRECT_PARENT.getNumber() ) {
			item.setFillColor( params.getAncestorGroupColor().getRGB() );
		}
		else if ( role == ElementRole.CHILD.getNumber() ) {
			item.setFillColor( params.getChildGroupColor().getRGB() );
		}
		else if ( role == ElementRole.OTHER.getNumber() ) {
			item.setFillColor( params.getOtherGroupColor().getRGB() );
		}

		return m_box;
	}
}
