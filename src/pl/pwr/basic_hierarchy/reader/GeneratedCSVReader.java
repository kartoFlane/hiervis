package pl.pwr.basic_hierarchy.reader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Scanner;

import pl.pwr.basic_hierarchy.common.Constants;
import pl.pwr.basic_hierarchy.common.HierarchyFiller;
import pl.pwr.basic_hierarchy.implementation.BasicHierarchy;
import pl.pwr.basic_hierarchy.implementation.BasicInstance;
import pl.pwr.basic_hierarchy.implementation.BasicGroup;
import pl.pwr.basic_hierarchy.interfaces.DataReader;
import pl.pwr.basic_hierarchy.interfaces.Hierarchy;
import pl.pwr.basic_hierarchy.interfaces.Instance;
import pl.pwr.basic_hierarchy.interfaces.Group;


// REFACTOR maybe create a factory pattern to generate nodes
public class GeneratedCSVReader implements DataReader
{
	/**
	 * This method assumes that data are generated using Micha³ Spytkowski's data generator, using TSSB method.
	 * <p>
	 * The first node listed is always the root node. It is also the only node without a parent.
	 * Nodes are given in depth-first order.
	 * </p>
	 * 
	 * @see interfaces.DataReader#load(java.lang.String)
	 */
	@Override
	public Hierarchy load( String filePath, boolean withInstancesNameAttribute, boolean withClassAttribute, boolean fillBreadthGaps )
	{
		// REFACTOR instead of nodes and AdditionalNodes data structures we could use one or more hash maps
		// REFACTOR skip nodes' elements containing "gen" prefix and assume that every ID prefix always begins with "gen"
		File inputFile = new File( filePath );
		if ( !inputFile.exists() && inputFile.isDirectory() ) {
			throw new RuntimeException(
				String.format(
					"Cannot access file: '%s'. Does it exist, and is it a %s-separated text file?",
					filePath, Constants.DELIMITER
				)
			);
		}

		BasicGroup root = null;
		ArrayList<BasicGroup> groups = new ArrayList<BasicGroup>();
		int rootIndexInGroups = -1;
		HashMap<String, Integer> eachClassAndItsCount = new HashMap<String, Integer>();

		try ( Scanner scanner = new Scanner( inputFile ) ) {
			int numberOfDataDimensions = Integer.MIN_VALUE;
			while ( scanner.hasNextLine() ) {
				String inputLine = scanner.nextLine();
				String[] lineValues = inputLine.split( Constants.DELIMITER );

				if ( numberOfDataDimensions == Integer.MIN_VALUE ) {
					if ( lineValues.length < 2 + ( withClassAttribute ? 1 : 0 ) + ( withInstancesNameAttribute ? 1 : 0 ) ) {
						throw new RuntimeException(
							String.format(
								"Input data is not formatted correctly, each line should contain at least a node ID and a value " +
									"(and optionally class attribute and/or instance name).%nLine: %s",
								inputLine
							)
						);
					}
					numberOfDataDimensions = lineValues.length - 1 - ( withClassAttribute ? 1 : 0 ) - ( withInstancesNameAttribute ? 1 : 0 );
				}

				if ( lineValues.length != numberOfDataDimensions + 1 + ( withClassAttribute ? 1 : 0 ) + ( withInstancesNameAttribute ? 1 : 0 ) ) {
					throw new RuntimeException(
						String.format(
							"Input data not formatted corectly, each line should contain a node id and %s data values.%nLine: %s",
							numberOfDataDimensions, inputLine
						)
					);
				}

				double[] values = new double[lineValues.length - 1 - ( withClassAttribute ? 1 : 0 ) - ( withInstancesNameAttribute ? 1 : 0 )];
				for ( int j = 0; j < lineValues.length - 1 - ( withClassAttribute ? 1 : 0 ) - ( withInstancesNameAttribute ? 1 : 0 ); j++ ) {
					try {
						values[j] = Double.parseDouble( lineValues[j + 1 + ( withClassAttribute ? 1 : 0 ) + ( withInstancesNameAttribute ? 1 : 0 )] );
					}
					catch ( NumberFormatException e ) {
						throw new RuntimeException(
							String.format(
								"Cannot parse %sth value of line: %s. All instance features should be valid floating point numbers.",
								j, inputLine
							)
						);
					}
				}

				String classAttr = null;
				if ( withClassAttribute ) {
					classAttr = lineValues[1];
					if ( eachClassAndItsCount.containsKey( classAttr ) ) {
						eachClassAndItsCount.put( classAttr, eachClassAndItsCount.get( classAttr ) + 1 );
					}
					else {
						eachClassAndItsCount.put( classAttr, 1 );
					}
				}

				String instanceNameAttr = null;
				if ( withInstancesNameAttribute ) {
					instanceNameAttr = lineValues[1 + ( withClassAttribute ? 1 : 0 )];
				}

				// assuming that node's instances are grouped in input file
				// REFACTOR: below could the binary-search be utilized with sorting by ID-comparator
				// boolean nodeExist = !nodes.isEmpty() && nodes.get(nodes.size()-1).getId().equalsIgnoreCase(lineValues[0]);
				boolean groupExists = false;
				int groupIndex = -1;
				for ( int i = 0; i < groups.size() && !groupExists; ++i ) {
					if ( groups.get( i ).getId().equalsIgnoreCase( lineValues[0] ) ) {
						groupExists = true;
						groupIndex = i;
					}
				}

				if ( groupExists ) {
					// nodes.get(nodes.size()-1).addInstance(new BasicInstance(nodes.get(nodes.size()-1).getId(), values, classAttrib));
					groups.get( groupIndex )
						.addInstance( new BasicInstance( instanceNameAttr, groups.get( groupIndex ).getId(), values, classAttr ) );
				}
				else {
					BasicGroup newGroup = new BasicGroup( lineValues[0], null, new LinkedList<Group>(), new LinkedList<Instance>() );
					newGroup.addInstance( new BasicInstance( instanceNameAttr, newGroup.getId(), values, classAttr ) );

					groups.add( newGroup );
					if ( root == null && lineValues[0].equalsIgnoreCase( Constants.ROOT_ID ) ) {
						root = groups.get( groups.size() - 1 );
						rootIndexInGroups = groups.size() - 1;
					}
				}
			}
		}
		catch ( IOException e ) {
			System.err.println( "While reading input file: " + filePath + "\n" );
			e.printStackTrace();
		}

		LinkedList<Group> allNodes = HierarchyFiller.addMissingEmptyNodes( root, groups, rootIndexInGroups, fillBreadthGaps );
		return new BasicHierarchy( root, allNodes, eachClassAndItsCount );
	}
}
