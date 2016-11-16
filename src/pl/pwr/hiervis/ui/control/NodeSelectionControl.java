package pl.pwr.hiervis.ui.control;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

import pl.pwr.hiervis.core.ElementRole;
import pl.pwr.hiervis.core.HVContext;
import pl.pwr.hiervis.visualisation.NodeRenderer;
import prefuse.controls.ControlAdapter;
import prefuse.data.Node;
import prefuse.data.Tree;
import prefuse.visual.EdgeItem;
import prefuse.visual.VisualItem;


/**
 * <p>
 * Allows selection of node visual items, changing their role and role of their family nodes:
 * </p>
 * - {@link ElementRole#CURRENT} for the selected node;<br/>
 * - {@link ElementRole#DIRECT_PARENT} for the direct parent of the selected node;</br>
 * - {@link ElementRole#INDIRECT_PARENT} for ancestors of the selected node
 * - (other than the direct parent);<br/>
 * - {@link ElementRole#CHILD} for children of the selected node;</br>
 * - {@link ElementRole#OTHER} for nodes that are not related to the selected node.
 * 
 * <p>
 * This information is then used by {@link NodeRenderer} to color the nodes according
 * to their role in the family.
 * </p>
 * 
 * @author Tomasz Bachmiñski
 *
 */
public class NodeSelectionControl extends ControlAdapter
{
	protected HVContext context;


	public NodeSelectionControl( HVContext context )
	{
		this.context = context;
	}

	@Override
	public void itemClicked( VisualItem item, MouseEvent e )
	{
		if ( item instanceof EdgeItem ) {
			// Ignore clicks on edges.
			return;
		}

		context.setSelectedRow( item.getRow() );
	}

	@Override
	public void itemKeyPressed( VisualItem item, KeyEvent e )
	{
		selectNode( e );
	}

	@Override
	public void keyPressed( KeyEvent e )
	{
		selectNode( e );
	}

	private void selectNode( KeyEvent e )
	{
		if ( isArrowEvent( e ) ) {
			Tree hierarchyTree = context.getTree();
			Node n = hierarchyTree.getNode( context.getSelectedRow() );

			switch ( e.getKeyCode() ) {
				case KeyEvent.VK_UP:
				case KeyEvent.VK_KP_UP: {
					n = n.getParent();
					break;
				}

				case KeyEvent.VK_DOWN:
				case KeyEvent.VK_KP_DOWN: {
					n = n.getFirstChild();
					break;
				}

				case KeyEvent.VK_LEFT:
				case KeyEvent.VK_KP_LEFT: {
					Node s = n.getPreviousSibling();
					if ( s == null && n.getParent() != null ) {
						s = n.getParent().getLastChild();
					}
					n = s;
					break;
				}

				case KeyEvent.VK_RIGHT:
				case KeyEvent.VK_KP_RIGHT: {
					Node s = n.getNextSibling();
					if ( s == null && n.getParent() != null ) {
						s = n.getParent().getFirstChild();
					}
					n = s;
					break;
				}
			}

			if ( n != null ) {
				context.setSelectedRow( n.getRow() );
			}
		}
	}

	private boolean isArrowEvent( KeyEvent e )
	{
		switch ( e.getKeyCode() ) {
			case KeyEvent.VK_UP:
			case KeyEvent.VK_DOWN:
			case KeyEvent.VK_LEFT:
			case KeyEvent.VK_RIGHT:
			case KeyEvent.VK_KP_UP:
			case KeyEvent.VK_KP_DOWN:
			case KeyEvent.VK_KP_LEFT:
			case KeyEvent.VK_KP_RIGHT:
				return true;
			default:
				return false;
		}
	}
}
