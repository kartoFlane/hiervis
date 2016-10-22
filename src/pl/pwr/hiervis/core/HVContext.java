package pl.pwr.hiervis.core;

import java.io.File;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import basic_hierarchy.interfaces.Hierarchy;
import basic_hierarchy.interfaces.Node;
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
public class HVContext {

	private static final Logger log = LogManager.getLogger( HVContext.class );

	private HVConfig config = null;
	private HierarchyProcessor processor = null;
	private Hierarchy inputHierarchy = null;
	private Tree hierarchyTree = null;

	private int selectedRow = 0;


	public HVContext() {
		processor = new HierarchyProcessor();

		setConfig( loadConfig() );
	}

	/**
	 * Loads the specified *.csv file and uses its data to visualize the hierarchy.
	 * 
	 * @param path
	 *            path to the *.csv file to load.
	 */
	public void load( Path path ) {
		setHierarchy( loadHierarchy( path, config.hasInstanceNameAttribute(), config.hasClassAttribute() ) );
		setTree( createHierarchyTree( this ) );

		selectedRow = 0;
	}

	/**
	 * @return true if there is hierarchy data available (ie. has been loaded),
	 *         false otherwise.
	 */
	public boolean isHierarchyDataLoaded() {
		return inputHierarchy != null;
	}

	public void setConfig( HVConfig config ) {
		this.config = config;
	}

	public HVConfig getConfig() {
		return config;
	}

	public void setHierarchy( Hierarchy hierarchy ) {
		inputHierarchy = hierarchy;
	}

	public Hierarchy getHierarchy() {
		return inputHierarchy;
	}

	public void setTree( Tree tree ) {
		hierarchyTree = tree;
	}

	public Tree getTree() {
		return hierarchyTree;
	}

	public void setSelectedRow( int i ) {
		selectedRow = i;
	}

	/**
	 * @return row of the currently selected node in the tree hierarchy view.
	 */
	public int getSelectedRow() {
		return selectedRow;
	}

	public Display createHierarchyDisplay() {
		return processor.createTreeDisplay( this );
	}

	public Visualization createHierarchyVisualization() {
		return processor.createTreeVisualization( this );
	}

	public Display createPointDisplay() {
		return processor.createPointDisplay( this );
	}

	public Visualization createPointVisualization( Node node ) {
		return processor.createPointVisualization( this, node );
	}

	private static HVConfig loadConfig() {
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

	private static Hierarchy loadHierarchy( Path path, boolean hasInstanceName, boolean hasClass ) {
		return new GeneratedCSVReader().load( path.toString(), hasInstanceName, hasClass, false );
	}

	private static Tree createHierarchyTree( HVContext context ) {
		return context.processor.createHierarchyTree(
				context.inputHierarchy.getRoot(),
				context.getConfig() );
	}

	public Node findNode( int row ) {
		Hierarchy h = getHierarchy();
		Tree tree = getTree();

		Node root = h.getRoot();
		prefuse.data.Node n = tree.getRoot();

		if ( row == 0 ) {
			return root;
		}
		else if ( row < 0 ) {
			return null;
		}

		Queue<Map.Entry<prefuse.data.Node, Node>> stackParentAndChild = new LinkedList<>(); // FIFO
		for ( Node child : root.getChildren() ) {
			stackParentAndChild.add( new AbstractMap.SimpleEntry<prefuse.data.Node, Node>( n, child ) );
		}

		int currentRow = 0;
		while ( !stackParentAndChild.isEmpty() ) {
			Entry<prefuse.data.Node, Node> sourceNodeWithItsParent = stackParentAndChild.remove();
			Node sourceNode = sourceNodeWithItsParent.getValue();

			++currentRow;
			if ( currentRow == row ) {
				return sourceNode;
			}

			for ( Node child : sourceNode.getChildren() ) {
				stackParentAndChild.add( new AbstractMap.SimpleEntry<prefuse.data.Node, Node>( n, child ) );
			}
		}

		return null;
	}
}
