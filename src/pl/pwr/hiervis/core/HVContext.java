package pl.pwr.hiervis.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import basic_hierarchy.interfaces.Group;
import basic_hierarchy.interfaces.Hierarchy;
import basic_hierarchy.reader.GeneratedCSVReader;
import pl.pwr.hiervis.visualisation.HierarchyProcessor;
import pl.pwr.hiervis.visualisation.TreeLayoutData;
import prefuse.Display;
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

	private HVConfig config = null;
	private Hierarchy inputHierarchy = null;
	private Tree hierarchyTree = null;
	private TreeLayoutData hierarchyTreeLayout = null;

	private int selectedRow = 0;


	public HVContext()
	{
		setConfig( loadConfig() );
	}

	/**
	 * Loads the specified CSV file and uses its data to visualize the hierarchy.
	 * 
	 * @param path
	 *            path to the CSV file to load.
	 */
	public void load( Path path ) throws IOException
	{
		setHierarchy(
			loadHierarchy(
				path,
				config.hasInstanceNameAttribute(),
				config.hasTrueClassAttribute(),
				config.hasDataNamesRow(),
				config.isFillBreadthGaps()
			)
		);

		Pair<Tree, TreeLayoutData> treeData = HierarchyProcessor.buildHierarchyTree(
			config,
			inputHierarchy.getRoot()
		);

		setTree( treeData.getLeft() );
		setTreeLayoutData( treeData.getRight() );

		selectedRow = 0;
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

	public void setSelectedRow( int i )
	{
		selectedRow = i;
	}

	/**
	 * @return row of the currently selected node in the tree hierarchy view.
	 */
	public int getSelectedRow()
	{
		return selectedRow;
	}

	public Display createHierarchyDisplay()
	{
		return HierarchyProcessor.createTreeDisplay( this );
	}

	public Visualization createHierarchyVisualization()
	{
		return HierarchyProcessor.createTreeVisualization( this );
	}

	public Display createPointDisplay()
	{
		return HierarchyProcessor.createInstanceDisplay( this );
	}

	public Visualization createInstanceVisualization( Group group )
	{
		return HierarchyProcessor.createInstanceVisualization( this, group );
	}

	private static HVConfig loadConfig()
	{
		File configFile = new File( HVConfig.FILE_PATH );
		HVConfig config = null;

		if ( configFile.exists() ) {
			try {
				config = HVConfig.from( configFile );
			}
			catch ( Exception e ) {
				log.error( "Error while loading config file: ", e );
			}
		}
		else {
			config = new HVConfig();
		}

		return config;
	}

	private static Hierarchy loadHierarchy(
		Path path,
		boolean hasInstanceName,
		boolean hasClass,
		boolean hasNames,
		boolean fixGaps ) throws IOException
	{
		return new GeneratedCSVReader().load( path.toString(), hasInstanceName, hasClass, hasNames, fixGaps );
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
}
