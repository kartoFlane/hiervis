package pl.pwr.hiervis.core;

import java.util.LinkedList;
import java.util.Queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import basic_hierarchy.interfaces.Group;
import basic_hierarchy.interfaces.Hierarchy;
import pl.pwr.hiervis.util.Event;
import pl.pwr.hiervis.visualisation.HierarchyProcessor;
import pl.pwr.hiervis.visualisation.TreeLayoutData;
import prefuse.Visualization;
import prefuse.data.Tree;


/**
 * A way to pass various application data around, without having to rely on
 * statically-accessible variables and states, or the singleton pattern.
 * 
 * @author Tomasz Bachmiñski
 *
 */
public class HVContext
{
	private static final Logger log = LogManager.getLogger( HVContext.class );

	public final Event<Integer> rowSelected = new Event<>();

	private HVConfig config = null;
	private Hierarchy inputHierarchy = null;
	private Tree hierarchyTree = null;
	private TreeLayoutData hierarchyTreeLayout = null;

	private int selectedRow = 0;


	public HVContext()
	{
		setConfig( new HVConfig() );
	}

	/**
	 * @return true if there is hierarchy data available (ie. has been loaded),
	 *         false otherwise.
	 */
	public boolean isHierarchyDataLoaded()
	{
		return inputHierarchy != null;
	}

	public void setConfig( HVConfig config )
	{
		this.config = config;
	}

	public HVConfig getConfig()
	{
		return config;
	}

	public void setHierarchy( Hierarchy hierarchy )
	{
		inputHierarchy = hierarchy;
	}

	public Hierarchy getHierarchy()
	{
		return inputHierarchy;
	}

	public void setTree( Tree tree )
	{
		hierarchyTree = tree;
	}

	public Tree getTree()
	{
		return hierarchyTree;
	}

	public void setTreeLayoutData( TreeLayoutData layoutData )
	{
		hierarchyTreeLayout = layoutData;
	}

	public TreeLayoutData getTreeLayoutData()
	{
		return hierarchyTreeLayout;
	}

	/**
	 * @return row of the currently selected node in the hierarchy view.
	 */
	public int getSelectedRow()
	{
		return selectedRow;
	}

	public Visualization createHierarchyVisualization()
	{
		return HierarchyProcessor.createTreeVisualization( this );
	}

	public Visualization createInstanceVisualization( Group group )
	{
		return HierarchyProcessor.createInstanceVisualization( this, group );
	}

	/**
	 * Finds the hierarchy group at the specified row.
	 * 
	 * @param row
	 *            the row in the data table at which the group is located.
	 * @return the group at the specified row, or null if not found.
	 */
	public Group findGroup( int row )
	{
		Hierarchy h = getHierarchy();
		Group group = h.getRoot();

		if ( row == 0 ) {
			return group;
		}

		Queue<Group> stack = new LinkedList<>();
		for ( Group child : group.getChildren() ) {
			stack.add( child );
		}

		int currentRow = 0;
		while ( !stack.isEmpty() ) {
			group = stack.remove();

			++currentRow;
			if ( currentRow == row ) {
				return group;
			}

			for ( Group child : group.getChildren() ) {
				stack.add( child );
			}
		}

		return null;
	}

	public void selectNode( int row )
	{
		if ( selectedRow != row ) {
			selectedRow = row;
			rowSelected.broadcast( row );
		}
	}
}
