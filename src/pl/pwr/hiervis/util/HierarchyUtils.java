package pl.pwr.hiervis.util;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import basic_hierarchy.common.Constants;
import basic_hierarchy.common.HierarchyBuilder;
import basic_hierarchy.common.NodeIdComparator;
import basic_hierarchy.implementation.BasicHierarchy;
import basic_hierarchy.implementation.BasicInstance;
import basic_hierarchy.implementation.BasicNode;
import basic_hierarchy.interfaces.Hierarchy;
import basic_hierarchy.interfaces.Instance;
import basic_hierarchy.interfaces.Node;
import pl.pwr.hiervis.hierarchy.LoadedHierarchy;


public class HierarchyUtils
{
	private HierarchyUtils()
	{
		// Static class -- disallow instantiation.
		throw new RuntimeException( "Attempted to instantiate a static class: " + getClass().getName() );
	}

	/**
	 * Merges the two hierarchies into a single, new hierarchy, with the specified merging node id.
	 * 
	 * @param source
	 *            the source hierarchy that is to be merged into destination hierarchy
	 * @param dest
	 *            the destination hierarchy that will receive nodes from source hierarchy
	 * @param nodeId
	 *            id of the node in the destination hierarchy that will serve as the merging point for the two hierarchies.
	 *            Nodes from the source hierarchy will be rebased to have this id as root.
	 * @return the new, merged hierarchy (shallow copy of both source and dest hierarchies)
	 */
	public static LoadedHierarchy merge( LoadedHierarchy source, LoadedHierarchy dest, String nodeId )
	{
		// Check parameters...
		if ( source == null ) {
			throw new IllegalArgumentException( "Source hierarchy is null." );
		}
		if ( dest == null ) {
			throw new IllegalArgumentException( "Destination hierarchy is null." );
		}

		int sourceDims = getFeatureCount( source.getMainHierarchy() );
		int destDims = getFeatureCount( dest.getMainHierarchy() );

		if ( sourceDims != destDims ) {
			throw new IllegalArgumentException(
				String.format(
					"Cannot merge hierarchies, because they have different feature counts - source: %s, dest: %s",
					sourceDims, destDims
				)
			);
		}

		// Invoke the actual merging method...
		return new LoadedHierarchy(
			merge( source.getMainHierarchy(), dest.getMainHierarchy(), nodeId ),
			source.options
		);
	}

	public static Hierarchy merge( Hierarchy source, Hierarchy dest, String nodeId )
	{
		final boolean useSubtree = false;

		List<BasicNode> nodes = new LinkedList<>();
		String rootId = source.getRoot().getId();
		rebase( Arrays.stream( source.getGroups() ), rootId, nodeId, nodes, useSubtree );

		if ( Constants.ROOT_ID.equals( nodeId ) ) {
			// No point in replacing the root, just substitute the entire hierarchy.
			return buildHierarchy( nodes, source.getDataNames(), useSubtree );
		}
		else {
			// Remove the merging point node from the dest hierarchy to replace it with the one from source
			Optional<Node> optNode = Arrays.stream( dest.getGroups() )
				.filter( n -> n.getId().equals( nodeId ) )
				.findAny();
			if ( optNode.isPresent() ) {
				dest = remove( dest, nodeId );
			}
			else {
				dest = clone( dest, useSubtree, null );
			}

			Arrays.stream( dest.getGroups() ).forEach( n -> nodes.add( (BasicNode)n ) );

			return buildHierarchy( nodes, dest.getDataNames(), useSubtree );
		}
	}

	/**
	 * Returns a copy of the specified hierarchy. The returned copy is a flat
	 * hierarchy containing all instances from the source hierarchy in the root node.
	 * 
	 * @param source
	 *            the hierarchy to flatten
	 * @return flattened hierarchy
	 */
	public static LoadedHierarchy flattenHierarchy( LoadedHierarchy source )
	{
		final boolean useSubtree = false;

		BasicNode root = new BasicNode( Constants.ROOT_ID, null, false );

		List<BasicNode> nodes = new LinkedList<>();
		nodes.add( root );

		source.getMainHierarchy().getRoot().getSubtreeInstances().forEach(
			in -> {
				root.addInstance(
					new BasicInstance(
						in.getInstanceName(),
						Constants.ROOT_ID,
						in.getData(),
						in.getTrueClass()
					)
				);
			}
		);

		Hierarchy flatHierarchy = buildHierarchy( nodes, source.getMainHierarchy().getDataNames(), useSubtree );
		return new LoadedHierarchy( flatHierarchy, source.options );
	}

	/**
	 * @param h
	 *            the hierarchy to get the class count map for
	 * @return the ground-truth class counts map, as reported by the hierarchy
	 */
	public static Map<String, Integer> getClassCountMap( Hierarchy h )
	{
		Map<String, Integer> classCountMap = new HashMap<>();

		String[] classes = h.getClasses();
		int[] counts = h.getClassesCount();

		for ( int i = 0; i < classes.length; ++i ) {
			classCountMap.put( classes[i], counts[i] );
		}

		return classCountMap;
	}

	/**
	 * Recomputes the ground-truth class count map for the specified hierarchy.
	 * 
	 * @param root
	 *            root node of the hierarchy to recompute the class count map for
	 * @return the recomputed ground-truth class count map
	 */
	public static Map<String, Integer> computeClassCountMap( Node root )
	{
		Map<String, Integer> classCountMap = new HashMap<>();

		for ( Instance in : root.getSubtreeInstances() ) {
			String trueClass = in.getTrueClass();
			if ( trueClass == null )
				break;
			classCountMap.put( trueClass, classCountMap.getOrDefault( trueClass, 0 ) + 1 );
		}

		return classCountMap;
	}

	/**
	 * Creates a new Hierarchy (a deep copy) that doesn't contain the node with the specified id.
	 * 
	 * @param source
	 *            the source hierarchy
	 * @param nodeId
	 *            the node id to remove
	 * @return the hierarchy without the specified node (deep copy)
	 */
	public static Hierarchy remove( Hierarchy source, String nodeId )
	{
		final boolean useSubtree = false;
		return clone( source, useSubtree, n -> !n.getId().startsWith( nodeId ) );
	}

	/**
	 * Creates a thin wrapper hierarchy around the specified node.
	 * Includes child nodes of this node.
	 * 
	 * @param source
	 *            the source hierarchy the node belongs to
	 * @param node
	 *            the node to wrap
	 * @return the wrapper hierarchy
	 */
	public static Hierarchy wrapNode( Hierarchy source, Node node )
	{
		List<BasicNode> nodes = new LinkedList<>();

		Arrays.stream( source.getGroups() )
			.filter( n -> n.getId().startsWith( node.getId() ) )
			.forEach( n -> nodes.add( (BasicNode)n ) );

		return new BasicHierarchy( nodes, source.getDataNames() );
	}

	/**
	 * Checks whether the specified hierarchy contains the specified node (by reference).
	 * 
	 * @param h
	 *            the hierarchy to look in
	 * @param node
	 *            the node to check
	 * @return true if the hierarchy contains the specified node, false otherwise.
	 */
	public static boolean contains( Hierarchy h, Node node )
	{
		return Arrays.stream( h.getGroups() ).anyMatch( n -> n == node );
	}

	/**
	 * Creates a sub-hierarchy of the specified {@link Hierarchy}, which contains the specified node as root,
	 * and all its child nodes.
	 * The newly created hierarchy is a deep copy of the source hierarchy.
	 * 
	 * @param source
	 *            the hierarchy to create the sub-hierarchy from
	 * @param nodeId
	 *            id of the node within the source hierarchy, which will be the root in the sub-hierarchy.
	 *            Defaults to {@link Constants#ROOT_ID} if null.
	 * @param destNodeId
	 *            the new id that the copied nodes will receive.
	 *            Defaults to {@link Constants#ROOT_ID} if null.
	 * @return the sub-hierarchy
	 * @throws NoSuchElementException
	 *             if the hierarchy doesn't contain a node with the specified name
	 */
	public static Hierarchy subHierarchy( Hierarchy source, String nodeId, String destNodeId )
	{
		final boolean useSubtree = false;

		if ( nodeId == null )
			nodeId = Constants.ROOT_ID;
		if ( destNodeId == null )
			destNodeId = Constants.ROOT_ID;

		List<BasicNode> nodes = new LinkedList<>();
		rebase( Arrays.stream( source.getGroups() ), nodeId, destNodeId, nodes, useSubtree );
		return buildHierarchy( nodes, source.getDataNames(), useSubtree );
	}

	/**
	 * Rebases all nodes and instances in the specified stream from the specified old id to the new id.
	 * This method creates copies of all objects (nodes, instances) with the same properties, but changed ids.
	 * 
	 * The rebased nodes have no parent-child relationships set, requiring a follow-up call to a method that
	 * will build them.
	 * 
	 * @param nodes
	 *            stream of nodes to rebase
	 * @param oldId
	 *            the old id to rebase. Only nodes that contain this substring will be rebased.
	 * @param newId
	 *            the new id to rebase the nodes as
	 * @param destNodes
	 *            the list to put the rebased nodes in
	 * @param useSubtree
	 *            whether the centroid calculation should also include child nodes' instances
	 */
	public static void rebase(
		Stream<Node> nodes, String oldId, String newId,
		List<BasicNode> destNodes, boolean useSubtree )
	{
		int prefixSub = oldId.length();

		nodes.filter( n -> n.getId().equals( oldId ) || HierarchyBuilder.areIdsAncestorAndDescendant( oldId, n.getId() ) )
			.forEach(
				n -> {
					String id = newId + n.getId().substring( prefixSub );
					BasicNode rebasedNode = new BasicNode( id, null, useSubtree );

					n.getNodeInstances().forEach(
						in -> rebasedNode.addInstance(
							new BasicInstance(
								in.getInstanceName(),
								id,
								// NOTE: Technically we should copy the array for a true deep copy,
								// however in the context of this application it makes no sense, since
								// we never modify feature values of instances, and duplication of these
								// arrays ends up being a considerable memory hog.
								// Arrays.copyOf( in.getData(), in.getData().length ),
								in.getData(),
								in.getTrueClass()
							)
						)
					);

					destNodes.add( rebasedNode );
				}
			);
	}

	/**
	 * Creates a deep copy of the specified hierarchy, creating copies of {@link Node}s and {@link Instance}s
	 * that comprise the {@link Hierarchy}.
	 * 
	 * One exception is that {@link Instance}s are not cloned 100% deeply -- the feature values array is copied
	 * by reference, since it is never modified, at least for now.
	 * 
	 * @param source
	 *            the hierarchy to clone.
	 * @param useSubtree
	 *            whether the centroid calculation should also include child nodes' instances
	 * @param nodeInclusionPredicate
	 *            predicate allowing to select nodes that should be included in the cloned hierarchy.
	 *            Can be null to include all nodes.
	 * @return the cloned hierarchy
	 */
	public static Hierarchy clone( Hierarchy source, boolean useSubtree, Predicate<Node> nodeInclusionPredicate )
	{
		List<BasicNode> nodes = new LinkedList<>();

		Arrays.stream( source.getGroups() ).forEach(
			n -> {
				if ( nodeInclusionPredicate != null && !nodeInclusionPredicate.test( n ) ) {
					return;
				}

				nodes.add( clone( n, useSubtree ) );
			}
		);

		return buildHierarchy( nodes, source.getDataNames(), useSubtree );
	}

	/**
	 * Creates a deep copy of the specified node, creating copies of {@link Instance}s that have
	 * been assigned to this {@link Node}.
	 * 
	 * @param source
	 *            the node to clone
	 * @param useSubtree
	 *            whether the centroid calculation should also include child nodes' instances
	 * @return
	 */
	public static BasicNode clone( Node source, boolean useSubtree )
	{
		// Can't clone the parent reference, leave it as null.
		BasicNode clonedNode = new BasicNode( source.getId(), null, useSubtree );
		source.getNodeInstances().forEach( in -> clonedNode.addInstance( clone( in ) ) );
		return clonedNode;
	}

	public static Instance clone( Instance source )
	{
		return new BasicInstance(
			source.getInstanceName(),
			source.getNodeId(),
			// NOTE: Technically we should copy the array for a true deep copy,
			// however in the context of this application it makes no sense, since
			// we never modify feature values of instances, and duplication of these
			// arrays ends up being a considerable memory hog.
			// Arrays.copyOf( in.getData(), in.getData().length ),
			source.getData(),
			source.getTrueClass()
		);
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
	public static Node findGroup( LoadedHierarchy h, int row )
	{
		Node group = h.getMainHierarchy().getRoot();

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

	/**
	 * Searches the specified hierarchy for a group with the specified name.
	 * 
	 * @param lh
	 *            the hierarchy to look in
	 * @param nodeId
	 *            name of the hierarchy group
	 * @return the hierarchy group with the specified name, or null if not found.
	 */
	public static Node findGroup( LoadedHierarchy lh, String nodeId )
	{
		for ( Node n : lh.getMainHierarchy().getGroups() ) {
			if ( n.getId().equals( nodeId ) )
				return n;
		}

		return null;
	}

	public static Hierarchy buildHierarchy( List<BasicNode> nodes, String[] dataNames, boolean useSubtree )
	{
		nodes.sort( new NodeIdComparator() );
		BasicNode root = nodes.get( 0 );
		HierarchyBuilder.createParentChildRelations( nodes, null );
		nodes.addAll( HierarchyBuilder.fixDepthGaps( nodes, root.getId(), useSubtree, null ) );
		HierarchyBuilder.recalculateCentroids( nodes, useSubtree, null );
		HierarchyBuilder.sortAllChildren( root );

		return new BasicHierarchy( nodes, dataNames );
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

		List<Node> nodes = new ArrayList<>();
		for ( Node n : h.getGroups() )
			nodes.add( n );
		nodes.sort( new NodeIdComparator() );

		for ( Node node : nodes ) {
			for ( Instance instance : node.getNodeInstances() ) {
				appendInstance( buf, instance, withAssignClass, withTrueClass, withInstanceNames );
				buf.append( '\n' );
			}
		}

		return buf.toString();
	}

	/**
	 * @param h
	 *            the hierarchy to get the instance from
	 * @return the first instance in the hierarchy
	 */
	public static Instance getFirstInstance( Hierarchy h )
	{
		return h.getRoot().getSubtreeInstances().get( 0 );
	}

	/**
	 * @param h
	 *            the hierarchy to get the feature count from
	 * @return number of features in the specified hierarchy
	 */
	public static int getFeatureCount( Hierarchy h )
	{
		return getFirstInstance( h ).getData().length;
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
		int dims = getFeatureCount( h );

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
			int dims = getFeatureCount( h );

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

