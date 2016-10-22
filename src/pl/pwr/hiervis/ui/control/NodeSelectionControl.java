package pl.pwr.hiervis.ui.control;

import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.Iterator;
import java.util.LinkedList;

import basic_hierarchy.interfaces.Node;
import pl.pwr.hiervis.core.ElementRole;
import pl.pwr.hiervis.core.HVConfig;
import pl.pwr.hiervis.core.HVConstants;
import pl.pwr.hiervis.core.HVContext;
import pl.pwr.hiervis.util.Utils;
import pl.pwr.hiervis.visualisation.NodeRenderer;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.controls.ControlAdapter;
import prefuse.data.Tree;
import prefuse.visual.VisualItem;
import prefuse.visual.tuple.TableEdgeItem;


/**
 * <p>
 * Allows selection of node visual items, changing their role and role
 * of their family nodes:
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
	protected Display pointDisplay;


	public NodeSelectionControl( HVContext context, Display pointDisplay )
	{
		this.context = context;
		this.pointDisplay = pointDisplay;
	}

	@Override
	public void itemClicked( VisualItem item, MouseEvent e )
	{
		if ( item instanceof TableEdgeItem ) {
			// Ignore clicks on edges.
			return;
		}

		Display d = (Display)e.getSource();
		selectNode( context, d, pointDisplay, item.getRow() );
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
			Display d = (Display)e.getSource();

			Tree hierarchyTree = context.getTree();
			prefuse.data.Node n = hierarchyTree.getNode( context.getSelectedRow() );

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
					prefuse.data.Node s = n.getPreviousSibling();
					if ( s == null && n.getParent() != null ) {
						s = n.getParent().getLastChild();
					}
					n = s;
					break;
				}

				case KeyEvent.VK_RIGHT:
				case KeyEvent.VK_KP_RIGHT: {
					prefuse.data.Node s = n.getNextSibling();
					if ( s == null && n.getParent() != null ) {
						s = n.getParent().getFirstChild();
					}
					n = s;
					break;
				}
			}

			if ( n != null ) {
				selectNode( context, d, pointDisplay, n.getRow() );
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

	public static void selectNode( HVContext context, Display treeDisplay, Display pointDisplay, int row )
	{
		context.setSelectedRow( row );

		updateNodeRoles( context, context.getSelectedRow() );
		treeDisplay.damageReport();
		treeDisplay.repaint();

		Node node = context.findNode( context.getSelectedRow() );
		Visualization vis = context.createPointVisualization( node );

		Utils.resetDisplayZoom( pointDisplay );

		pointDisplay.setVisualization( vis );

		vis.run( "draw" );
		Utils.waitUntilActivitiesAreFinished();

		// Set the entire display area as dirty, so that it is redrawn.
		pointDisplay.damageReport();
		pointDisplay.repaint();
	}

	@SuppressWarnings("unchecked")
	private static void updateNodeRoles( HVContext context, int row )
	{
		Tree hierarchyTree = context.getTree();
		HVConfig config = context.getConfig();

		boolean isFound = false;

		// Reset all nodes back to 'other'
		for ( int i = 0; i < hierarchyTree.getNodeCount(); i++ ) {
			prefuse.data.Node n = hierarchyTree.getNode( i );
			n.setInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, ElementRole.OTHER.getNumber() );
		}

		// If no node is selected, then there's no point in trying to recategorize nodes, since
		// all will be classified as 'other' anyway.
		if ( row < 0 )
			return;

		// Recategorize nodes based on the currently selected node
		for ( int i = 0; i < hierarchyTree.getNodeCount() && !isFound; i++ ) {
			prefuse.data.Node n = hierarchyTree.getNode( i );
			if ( n.getRow() == row ) {
				isFound = true;
				// colour child groups
				LinkedList<prefuse.data.Node> stack = new LinkedList<>();
				stack.add( n );
				while ( !stack.isEmpty() ) {
					prefuse.data.Node current = stack.removeFirst();
					current.setInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, ElementRole.CHILD.getNumber() );
					for ( Iterator<prefuse.data.Node> children = current.children(); children.hasNext(); ) {
						prefuse.data.Node child = children.next();
						stack.add( child );
					}
				}

				if ( config.isDisplayAllPoints() && n.getParent() != null ) {
					stack = new LinkedList<>();
					// when the parent is empty, then we need to search up in the hierarchy because empty
					// parents are skipped, but displayed on output images
					prefuse.data.Node directParent = n.getParent();
					stack.add( directParent );
					while ( !stack.isEmpty() ) {
						prefuse.data.Node current = stack.removeFirst();
						current.setInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, ElementRole.INDIRECT_PARENT.getNumber() );
						if ( current.getParent() != null ) {
							stack.add( current.getParent() );
						}
					}
					directParent.setInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, ElementRole.DIRECT_PARENT.getNumber() );
				}
				n.setInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME, ElementRole.CURRENT.getNumber() );
			}
		}
	}
}
