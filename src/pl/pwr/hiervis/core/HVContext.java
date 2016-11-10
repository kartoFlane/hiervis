package pl.pwr.hiervis.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import basic_hierarchy.interfaces.Group;
import basic_hierarchy.interfaces.Hierarchy;
import basic_hierarchy.reader.GeneratedCSVReader;
import pl.pwr.hiervis.visualisation.HierarchyProcessor;
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
	private HierarchyProcessor processor = null;
	private Hierarchy inputHierarchy = null;
	private Tree hierarchyTree = null;

	private int selectedRow = 0;


	public HVContext()
	{
		processor = new HierarchyProcessor();

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
		setHierarchy( loadHierarchy( path, config.hasInstanceNameAttribute(), config.hasClassAttribute(), config.hasDataNamesRow() ) );
		setTree( createHierarchyTree( this ) );

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
		return processor.createTreeDisplay( this );
	}

	public Visualization createHierarchyVisualization()
	{
		return processor.createTreeVisualization( this );
	}

	public Display createPointDisplay()
	{
		return processor.createPointDisplay( this );
	}

	public Visualization createPointVisualization( Group group )
	{
		return processor.createPointVisualization( this, group );
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

	private static Hierarchy loadHierarchy( Path path, boolean hasInstanceName, boolean hasClass, boolean hasNames ) throws IOException
	{
		return new GeneratedCSVReader().load( path.toString(), hasInstanceName, hasClass, hasNames, false );
	}

	private static Tree createHierarchyTree( HVContext context )
	{
		return context.processor.createHierarchyTree(
			context.inputHierarchy.getRoot(),
			context.getConfig()
		);
	}

	/**
	 * Finds the hierarchy node at the specified row.
	 * 
	 * @param row
	 *            the row in the data table at which the node is located.
	 * @return the node at the specified row, or null if not found.
	 */
	public Group findNode( int row )
	{
		Hierarchy h = getHierarchy();
		Group node = h.getRoot();

		if ( row == 0 ) {
			return node;
		}

		Queue<Group> stack = new LinkedList<>();
		for ( Group child : node.getChildren() ) {
			stack.add( child );
		}

		int currentRow = 0;
		while ( !stack.isEmpty() ) {
			node = stack.remove();

			++currentRow;
			if ( currentRow == row ) {
				return node;
			}

			for ( Group child : node.getChildren() ) {
				stack.add( child );
			}
		}

		return null;
	}
}
