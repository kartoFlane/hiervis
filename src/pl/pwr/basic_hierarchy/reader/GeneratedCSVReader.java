package pl.pwr.basic_hierarchy.reader;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

import pl.pwr.basic_hierarchy.common.Constants;
import pl.pwr.basic_hierarchy.common.HierarchyFiller;
import pl.pwr.basic_hierarchy.implementation.BasicGroup;
import pl.pwr.basic_hierarchy.implementation.BasicHierarchy;
import pl.pwr.basic_hierarchy.implementation.BasicInstance;
import pl.pwr.basic_hierarchy.interfaces.DataReader;
import pl.pwr.basic_hierarchy.interfaces.Group;
import pl.pwr.basic_hierarchy.interfaces.Hierarchy;
import pl.pwr.basic_hierarchy.interfaces.Instance;


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
	public Hierarchy load( String filePath, boolean withInstancesNameAttribute, boolean withClassAttribute, boolean fixBreadthGaps )
	{
		// REFACTOR: Could create a factory class to generate nodes.
		// REFACTOR: Skip nodes' elements containing "gen" prefix and assume that every ID prefix always begins with "gen"
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
		HashMap<String, Integer> eachClassAndItsCount = new HashMap<String, Integer>();

		try ( Scanner scanner = new Scanner( inputFile ) ) {
			final int optionalColumns = ( withClassAttribute ? 1 : 0 ) + ( withInstancesNameAttribute ? 1 : 0 );
			final int expectedMinimumColumnCount = 1 + optionalColumns;
			int dataColumnCount = -1;
			int totalColumnCount = -1;

			while ( scanner.hasNextLine() ) {
				String inputLine = scanner.nextLine();
				String[] lineValues = inputLine.split( Constants.DELIMITER );

				if ( dataColumnCount == -1 ) {
					// First line encountered.

					// Make sure that the file is valid -- it needs to have a node ID column,
					// at most 2 optional columns, and at least one data column.
					if ( lineValues.length <= expectedMinimumColumnCount ) {
						throw new RuntimeException(
							String.format(
								"Input data is not formatted correctly. Each line should contain at least a node ID columm and a value column " +
									"(and optionally class attribute and/or instance name).%nLine: %s",
								inputLine
							)
						);
					}
					else {
						// File seems to be valid -- compute column counts for all the other rows.
						dataColumnCount = lineValues.length - 1 - optionalColumns;
						totalColumnCount = expectedMinimumColumnCount + dataColumnCount;
					}
				}

				// Assert that the row has the expected number of columns.
				if ( lineValues.length != totalColumnCount ) {
					throw new RuntimeException(
						String.format(
							"Input data not formatted corectly - each line should contain a total of %s columns.%nLine: %s",
							totalColumnCount, inputLine
						)
					);
				}

				String trueClassAttr = null;
				if ( withClassAttribute ) {
					trueClassAttr = lineValues[1];
					if ( eachClassAndItsCount.containsKey( trueClassAttr ) ) {
						eachClassAndItsCount.put( trueClassAttr, eachClassAndItsCount.get( trueClassAttr ) + 1 );
					}
					else {
						eachClassAndItsCount.put( trueClassAttr, 1 );
					}
				}

				String instanceNameAttr = null;
				if ( withInstancesNameAttribute ) {
					instanceNameAttr = lineValues[1 + ( withClassAttribute ? 1 : 0 )];
				}

				double[] values = new double[dataColumnCount];
				for ( int j = 0; j < values.length; j++ ) {
					try {
						// Data columns are always last.
						values[j] = Double.parseDouble( lineValues[j + 1 + optionalColumns] );
					}
					catch ( NumberFormatException e ) {
						throw new RuntimeException(
							String.format(
								"Cannot parse %sth data value of line: %s. All instance features should be valid floating point numbers.",
								j, inputLine
							)
						);
					}
				}

				// Assuming that nodes instances are grouped in input file
				// REFACTOR: could sort groups by ID, and then use binary search to find the group quicker.
				int groupIndex = -1;
				for ( int i = 0; i < groups.size() && groupIndex == -1; ++i ) {
					if ( groups.get( i ).getId().equalsIgnoreCase( lineValues[0] ) ) {
						groupIndex = i;
					}
				}

				if ( groupIndex == -1 ) {
					// Group for this id doesn't exist yet. Create it.
					BasicGroup newGroup = new BasicGroup( lineValues[0], null, new LinkedList<Group>(), new LinkedList<Instance>() );
					groups.add( newGroup );

					newGroup.addInstance( new BasicInstance( instanceNameAttr, newGroup.getId(), values, trueClassAttr ) );

					if ( root == null && lineValues[0].equalsIgnoreCase( Constants.ROOT_ID ) ) {
						root = newGroup;
					}
				}
				else {
					groups.get( groupIndex ).addInstance(
						new BasicInstance( instanceNameAttr, groups.get( groupIndex ).getId(), values, trueClassAttr )
					);
				}
			}
		}
		catch ( IOException e ) {
			System.err.println( "Error while reading input file: " + filePath + "\n" );
			e.printStackTrace();
		}

		List<? extends Group> allNodes = HierarchyFiller.buildCompleteGroupHierarchy( root, groups, fixBreadthGaps );
		return new BasicHierarchy( root, allNodes, eachClassAndItsCount );
	}
}
