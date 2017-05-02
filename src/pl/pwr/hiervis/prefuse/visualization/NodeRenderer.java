package pl.pwr.hiervis.prefuse.visualization;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;

import pl.pwr.hiervis.core.HVConfig;
import pl.pwr.hiervis.core.HVConstants;
import pl.pwr.hiervis.hierarchy.ElementRole;
import prefuse.render.AbstractShapeRenderer;
import prefuse.visual.VisualItem;


public class NodeRenderer extends AbstractShapeRenderer
{
	protected HVConfig params;

	protected Ellipse2D ellipse = new Ellipse2D.Double();
	protected int nodeSize;


	public NodeRenderer( int nodeSize, HVConfig params )
	{
		this.params = params;
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

		int role = item.getInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME );

		if ( role == ElementRole.CURRENT.getNumber() ) {
			item.setFillColor( params.getCurrentGroupColor().getRGB() );
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

		return ellipse;
	}
}
