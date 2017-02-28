package pl.pwr.hiervis.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.stream.Collectors;

import basic_hierarchy.implementation.BasicHierarchy;
import basic_hierarchy.interfaces.Hierarchy;
import basic_hierarchy.interfaces.Instance;
import basic_hierarchy.interfaces.Node;
import basic_hierarchy.reader.GeneratedCSVReader;


public class HierarchyUtils
{
	private HierarchyUtils()
	{
		// Static class -- disallow instantiation.
		throw new RuntimeException( "Attempted to instantiate a static class: " + getClass().getName() );
	}

	/**
	 * Creates a sub-hierarchy of the specified {@link Hierarchy}, which contains the specified node as root,
	 * and all its child nodes.
	 * The newly created hierarchy is a shallow copy; nodes added to it are shared with the source hierarchy.
	 * 
	 * @param source
	 *            the hierarchy to create the sub-hierarchy from
	 * @param nodeId
	 *            id of the node within the source hierarchy, which will be the root in the sub-hierarchy
	 * @return the sub-hierarchy
	 */
	public static Hierarchy subHierarchy( Hierarchy source, String nodeId )
	{
		Node node = Arrays.stream( source.getGroups() ).filter( n -> n.getId().equals( nodeId ) ).findFirst().get();

		Map<String, Integer> classToCountMap = new HashMap<>();
		List<Node> nodes = new LinkedList<>();

		Arrays.stream( source.getGroups() )
			.filter( n -> n.getId().startsWith( node.getId() ) )
			.forEach(
				n -> {
					nodes.add( n );
					classToCountMap.put( n.getId(), source.getParticularClassCount( n.getId(), false ) );
				}
			);

		return new BasicHierarchy( node, nodes, source.getDataNames(), classToCountMap );
	}

	/**
	 * Finds the hierarchy group at the specified row.
	 * 
	 * @param h
	 *            the hierarchy to search in
	 * @param row
	 *            the row in the data table at which the node is located.
	 * @return the group at the specified row, or null if not found.
	 */
	public static Node findGroup( Hierarchy h, int row )
	{
		Node group = h.getRoot();

		if ( row == 0 ) {
			return group;
		}

		Queue<Node> stack = new LinkedList<>();
		for ( Node child : group.getChildren() ) {
			stack.add( child );
		}

		int currentRow = 0;
		while ( !stack.isEmpty() ) {
			group = stack.remove();

			++currentRow;
			if ( currentRow == row ) {
				return group;
			}

			for ( Node child : group.getChildren() ) {
				stack.add( child );
			}
		}

		return null;
	}

	public static Hierarchy load(
		String path,
		boolean hasInstanceNames, boolean hasTrueClass, boolean hasHeader,
		boolean fillBreadthGaps, boolean useSubtree ) throws IOException
	{
		return new GeneratedCSVReader().load(
			path,
			hasInstanceNames, hasTrueClass, hasHeader,
			fillBreadthGaps, useSubtree
		);
	}

	// -----------------------------------------------------------------------------------------------

	/**
	 * Serializes the specified hierarchy into CSV format with the specified settings, and saves it
	 * in the specified file with UTF-8 encoding.
	 * 
	 * @param path
	 *            the file to save the hierarchy in
	 * @param h
	 *            the hierarchy to serialize
	 * @param withAssignClass
	 *            whether the serialized form should include assign class attribute
	 * @param withTrueClass
	 *            whether the serialized form should include true class attribute
	 * @param withInstanceNames
	 *            whether the serialized form should include instance name attribute
	 * @param withHeader
	 *            whether the serialized CSV should include a header with column names
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	public static void save(
		String path,
		Hierarchy h,
		boolean withAssignClass, boolean withTrueClass, boolean withInstanceNames, boolean withHeader ) throws IOException
	{
		// Via: http://stackoverflow.com/a/9853261
		OutputStreamWriter writer = new OutputStreamWriter(
			new FileOutputStream( path ),
			Charset.forName( "UTF-8" ).newEncoder()
		);

		writer.write( toCSV( h, withAssignClass, withTrueClass, withInstanceNames, withHeader ) );
		writer.flush();
		writer.close();
	}

	/**
	 * Serializes the specified hierarchy into CSV format, with the specified additional options.
	 * 
	 * @param h
	 *            the hierarchy to serialize
	 * @param withAssignClass
	 *            whether the serialized form should include assign class attribute
	 * @param withTrueClass
	 *            whether the serialized form should include true class attribute
	 * @param withInstanceNames
	 *            whether the serialized form should include instance name attribute
	 * @param withHeader
	 *            whether the serialized CSV should include a header with column names
	 * @return the serialized form of the hierarchy
	 */
	public static String toCSV(
		Hierarchy h,
		boolean withAssignClass, boolean withTrueClass, boolean withInstanceNames, boolean withHeader )
	{
		int capacity = 0;
		try {
			estimateBufferSize( h, withAssignClass, withTrueClass, withInstanceNames );
		}
		catch ( ArithmeticException e ) {
			capacity = Integer.MAX_VALUE;
		}

		StringBuilder buf = new StringBuilder( capacity );

		if ( withHeader ) {
			appendCSVHeader( buf, h, withAssignClass, withTrueClass, withInstanceNames );
			buf.append( '\n' );
		}

		for ( Node node : h.getGroups() ) {
			for ( Instance instance : node.getNodeInstances() ) {
				appendInstance( buf, instance, withAssignClass, withTrueClass, withInstanceNames );
				buf.append( '\n' );
			}
		}

		return buf.toString();
	}

	/**
	 * Attempts to estimate the length of string required to serialize the specified hierarchy, given the specified options.
	 * If the required capacity overflows range of 32-bit integer, this method throws an {@link ArithmeticException}.
	 * 
	 * @param h
	 *            the hierarchy to serialize
	 * @param withAssignClass
	 *            whether the serialized form should include assign class attribute
	 * @param withTrueClass
	 *            whether the serialized form should include true class attribute
	 * @param withInstanceNames
	 *            whether the serialized form should include instance name attribute
	 * @return the estimated buffer size
	 */
	private static int estimateBufferSize( Hierarchy h, boolean withAssignClass, boolean withTrueClass, boolean withInstanceNames )
	{
		final int charsForNodeId = 32;
		final int charsForInstanceName = 64;
		final int charsForFeatureValue = 16;

		int instances = h.getOverallNumberOfInstances();
		int dims = h.getRoot().getSubtreeInstances().get( 0 ).getData().length;

		int result = instances * dims * charsForFeatureValue;

		if ( withAssignClass ) result = Math.addExact( result, charsForNodeId );
		if ( withTrueClass ) result = Math.addExact( result, charsForNodeId );
		if ( withInstanceNames ) result = Math.addExact( result, instances * charsForInstanceName );

		return result;
	}

	/**
	 * Constructs a CSV file header for the specified hierarchy and settings,
	 * and appends it to the buffer without a newline at the end.
	 */
	private static void appendCSVHeader(
		StringBuilder buf, Hierarchy h,
		boolean withAssignClass, boolean withTrueClass, boolean withInstanceNames )
	{
		if ( withAssignClass ) buf.append( "class;" );
		if ( withTrueClass ) buf.append( "true_class;" );
		if ( withInstanceNames ) buf.append( "instance_name;" );

		String[] dataNames = h.getDataNames();
		if ( dataNames == null ) {
			int dims = h.getRoot().getNodeInstances().get( 0 ).getData().length;

			dataNames = new String[dims];
			for ( int i = 0; i < dims; ++i )
				dataNames[i] = "dimension_" + i;
		}

		buf.append( Arrays.stream( dataNames ).collect( Collectors.joining( ";" ) ) );
	}

	/**
	 * Constructs a CSV file row for the specified instance and settings,
	 * and appends it to the buffer without a newline at the end.
	 */
	private static void appendInstance(
		StringBuilder buf, Instance instance,
		boolean withAssignClass, boolean withTrueClass, boolean withInstanceNames )
	{
		if ( withAssignClass ) buf.append( instance.getNodeId() ).append( ';' );
		if ( withTrueClass ) buf.append( instance.getTrueClass() ).append( ';' );
		if ( withInstanceNames ) buf.append( instance.getInstanceName() ).append( ';' );

		buf.append(
			Arrays.stream( instance.getData() )
				.mapToObj( d -> Double.toString( d ) )
				.collect( Collectors.joining( ";" ) )
		);
	}
}

