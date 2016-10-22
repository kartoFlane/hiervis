package pl.pwr.basic_hierarchy.reader;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

import pl.pwr.basic_hierarchy.common.Constants;
import pl.pwr.basic_hierarchy.common.HierarchyFiller;
import pl.pwr.basic_hierarchy.implementation.BasicHierarchy;
import pl.pwr.basic_hierarchy.implementation.BasicInstance;
import pl.pwr.basic_hierarchy.implementation.BasicNode;
import pl.pwr.basic_hierarchy.interfaces.DataReader;
import pl.pwr.basic_hierarchy.interfaces.Hierarchy;
import pl.pwr.basic_hierarchy.interfaces.Instance;
import pl.pwr.basic_hierarchy.interfaces.Node;


//REFACTOR maybe create a factory pattern to generate nodes
public class GeneratedCSVReader implements DataReader
{
	/*
	 * I assume that data are generated using Michal Spytkowski's data generator with the use of TSSB method.
	 * Assume that:
	 * "The first node listed is always
	 * the root node, it is also the only node without a parent and the node specified
	 * in the first section to be the root of the tree. The nodes are given in depth first
	 * order."
	 * 
	 * @see interfaces.DataReader#load(java.lang.String)
	 */
	@Override
	public Hierarchy load( String filePath, boolean withInstancesNameAttribute, boolean withClassAttribute, boolean fillBreathGaps )
	{
		// REFACTOR instead of nodes and AdditionalNodes data structures we could use one or more hash maps
		// REFACTOR skip nodes' elements containing "gen" prefix and assume that every ID prefix always begins with "gen"
		File inputFile = new File( filePath );
		if ( !inputFile.exists() && inputFile.isDirectory() ) {
			System.err.println(
				"Cannot access to file: " + filePath + ". Does it exist and is it a "
					+ Constants.DELIMITER + "-separated text file?\n"
			);
			System.exit( 1 );
		}

		BasicNode root = null;
		ArrayList<BasicNode> nodes = new ArrayList<BasicNode>();
		int rootIndexInNodes = -1;
		int numberOfInstances = 0;
		HashMap<String, Integer> eachClassAndItsCount = new HashMap<String, Integer>();
		try ( Scanner scanner = new Scanner( inputFile ) ) {
			int numberOfDataDimensions = Integer.MIN_VALUE;
			while ( scanner.hasNextLine() ) {
				String inputLine = scanner.nextLine();
				String[] lineValues = inputLine.split( Constants.DELIMITER );

				if ( numberOfDataDimensions == Integer.MIN_VALUE ) {
					if ( lineValues.length < 2 + ( withClassAttribute ? 1 : 0 ) + ( withInstancesNameAttribute ? 1 : 0 ) ) {
						System.err.println(
							"Input data not formatted correctly, each line should contain "
								+ "at least a node id and a value (and optionally class attribute and/or instance name). "
								+ "Line: " + inputLine + "\n"
						);
						System.exit( 1 );
					}
					numberOfDataDimensions = lineValues.length - 1 - ( withClassAttribute ? 1 : 0 ) - ( withInstancesNameAttribute ? 1 : 0 );
				}

				if ( lineValues.length != numberOfDataDimensions + 1 + ( withClassAttribute ? 1 : 0 ) + ( withInstancesNameAttribute ? 1 : 0 ) ) {
					System.err.println(
						"Input data not formatted corectly, each line should contain a node id " +
							" and " + numberOfDataDimensions + " data values. Line: " + inputLine + "\n"
					);
					System.exit( 1 );
				}

				double[] values = new double[lineValues.length - 1 - ( withClassAttribute ? 1 : 0 ) - ( withInstancesNameAttribute ? 1 : 0 )];
				for ( int j = 0; j < lineValues.length - 1 - ( withClassAttribute ? 1 : 0 ) - ( withInstancesNameAttribute ? 1 : 0 ); j++ ) {
					try {
						values[j] = Double.parseDouble( lineValues[j + 1 + ( withClassAttribute ? 1 : 0 ) + ( withInstancesNameAttribute ? 1 : 0 )] );
					}
					catch ( NumberFormatException e ) {
						System.err.println(
							"Cannot parse " + j + "-th value of line: " + inputLine +
								". All instance features should be valid floating point numbers.\n"
						);
						System.exit( 1 );
					}
				}

				String classAttrib = null;
				if ( withClassAttribute ) {
					classAttrib = lineValues[1];
					if ( eachClassAndItsCount.containsKey( classAttrib ) ) {
						eachClassAndItsCount.put( classAttrib, eachClassAndItsCount.get( classAttrib ) + 1 );
					}
					else {
						eachClassAndItsCount.put( classAttrib, 1 );
					}
				}

				String instanceNameAttrib = null;
				if ( withInstancesNameAttribute ) {
					instanceNameAttrib = lineValues[1 + ( withClassAttribute ? 1 : 0 )];
				}

				// assuming that node's instances are grouped in input file
				// REFACTOR: below could the binary-search be utilised with sorting by ID-comparator
				// boolean nodeExist = !nodes.isEmpty() && nodes.get(nodes.size()-1).getId().equalsIgnoreCase(lineValues[0]);
				boolean nodeExist = false;
				int nodeIndex = -1;
				for ( int nodeIndexIter = 0; nodeIndexIter < nodes.size() && !nodeExist; nodeIndexIter++ ) {
					if ( nodes.get( nodeIndexIter ).getId().equalsIgnoreCase( lineValues[0] ) ) {
						nodeExist = true;
						nodeIndex = nodeIndexIter;
					}
				}
				if ( nodeExist ) {
					// nodes.get(nodes.size()-1).addInstance(new BasicInstance(nodes.get(nodes.size()-1).getId(), values, classAttrib));
					nodes.get( nodeIndex )
						.addInstance( new BasicInstance( instanceNameAttrib, nodes.get( nodeIndex ).getId(), values, classAttrib ) );
					numberOfInstances++;
				}
				else {
					BasicNode nodeToAdd = new BasicNode( lineValues[0], null, new LinkedList<Node>(), new LinkedList<Instance>() );
					nodeToAdd.addInstance( new BasicInstance( instanceNameAttrib, nodeToAdd.getId(), values, classAttrib ) );
					numberOfInstances++;
					nodes.add( nodeToAdd );
					if ( root == null && lineValues[0].equalsIgnoreCase( Constants.ROOT_ID ) ) {
						root = nodes.get( nodes.size() - 1 );
						rootIndexInNodes = nodes.size() - 1;
					}
				}
			}
		}
		catch ( Exception e ) {
			System.err.println( "While reading input file: " + filePath + "\n" );
			e.printStackTrace();
		}

		LinkedList<Node> allNodes = HierarchyFiller.addMissingEmptyNodes( root, nodes, rootIndexInNodes, fillBreathGaps );
		return new BasicHierarchy( root, allNodes, eachClassAndItsCount, numberOfInstances );
	}
}
