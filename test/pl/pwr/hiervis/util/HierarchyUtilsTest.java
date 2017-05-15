package pl.pwr.hiervis.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import basic_hierarchy.common.Constants;
import basic_hierarchy.common.HierarchyBuilder;
import basic_hierarchy.common.NodeIdComparator;
import basic_hierarchy.implementation.BasicHierarchy;
import basic_hierarchy.implementation.BasicInstance;
import basic_hierarchy.implementation.BasicNode;
import basic_hierarchy.interfaces.Hierarchy;
import basic_hierarchy.interfaces.Instance;
import basic_hierarchy.interfaces.Node;


public class HierarchyUtilsTest
{
	/** Feature values' array is copied by reference when cloning Instances (memory optimization) */
	private static final boolean _featureArrayOptimization = true;

	Hierarchy alpha = null;


	@Before
	public void setup()
	{
		alpha = generateHierarchy(
			3000, 2,
			"gen.0", "gen.0.0", "gen.0.1",
			"gen.0.2", "gen.0.3", "gen.0.1.1"
		);
	}

	@Test
	public void testClone()
	{
		Hierarchy test = HierarchyUtils.clone( alpha, false, null );
		Assert.assertFalse( test == alpha );

		compareHierarchies( alpha, test );
	}

	@Test
	public void testSubHierarchy()
	{
		testSubHierarchy( alpha, "gen.0.2", Constants.ROOT_ID );
		testSubHierarchy( alpha, "gen.0.1.1", Constants.ROOT_ID );
	}

	@Test
	public void testMerge()
	{
		Hierarchy test = HierarchyUtils.subHierarchy( alpha, "gen.0.2", Constants.ROOT_ID );
		testMerge( alpha, test, "gen.0.2" );

		test = HierarchyUtils.subHierarchy( alpha, "gen.0.1.1", "gen.0.8.4.3" );
		testMerge( alpha, test, "gen.0.1.1" );
	}

	// -------------------------------------------------------------

	public static void testSubHierarchy( Hierarchy alphaH, String srcId, String destId )
	{
		Hierarchy testH = HierarchyUtils.subHierarchy( alphaH, srcId, destId );

		List<Node> alphaNs = new ArrayList<>( Arrays.asList( alphaH.getGroups() ) );
		alphaNs.removeIf( n -> !n.getId().contains( srcId ) );
		List<Node> testNs = Arrays.asList( testH.getGroups() );

		Assert.assertEquals( alphaNs.size(), testNs.size() );

		final int _endi = alphaNs.size();
		for ( int i = 0; i < _endi; ++i ) {
			Node alphaN = alphaNs.get( i );
			Node testN = testNs.get( i );

			Assert.assertFalse( alphaN == testN );
			String alphaId = alphaN.getId().replace( srcId, "" );
			String testId = testN.getId().replace( destId, "" );
			Assert.assertEquals( alphaId, testId );

			List<Instance> alphaIs = alphaN.getNodeInstances();
			List<Instance> testIs = testN.getNodeInstances();
			Assert.assertEquals( alphaIs.size(), testIs.size() );

			final int _endj = alphaIs.size();
			for ( int j = 0; j < _endj; ++j ) {
				Instance alphaI = alphaIs.get( j );
				Instance testI = testIs.get( j );

				alphaId = alphaI.getNodeId().replace( srcId, "" );
				testId = testI.getNodeId().replace( destId, "" );
				Assert.assertEquals( alphaId, testId );

				Assert.assertFalse( alphaI == testI );
				Assert.assertEquals( alphaI.getTrueClass(), testI.getTrueClass() );
				Assert.assertEquals( alphaI.getInstanceName(), testI.getInstanceName() );
				if ( _featureArrayOptimization ) {
					Assert.assertEquals( alphaI.getData(), testI.getData() );
				}
				else {
					Assert.assertFalse( alphaI.getData() == testI.getData() );
					Assert.assertArrayEquals( alphaI.getData(), testI.getData(), 0 );
				}
			}
		}
	}

	public static void testMerge( Hierarchy alphaH, Hierarchy testH, String mergeNodeId )
	{
		testH = HierarchyUtils.merge( testH, alphaH, mergeNodeId );
		compareHierarchies( alphaH, testH );
	}

	/**
	 * Compares the two hierarchies for deep equality, while also asserting that they don't contain
	 * the same objects (by reference).
	 * 
	 * @param a
	 *            the first hierarchy
	 * @param b
	 *            the second hierarchy
	 */
	public static void compareHierarchies( Hierarchy a, Hierarchy b )
	{
		Assert.assertFalse( a == b );
		Assert.assertEquals( a.getRoot().getId(), b.getRoot().getId() );
		Assert.assertEquals( a.getOverallNumberOfInstances(), b.getOverallNumberOfInstances() );
		Assert.assertArrayEquals( a.getClasses(), b.getClasses() );
		Assert.assertArrayEquals( a.getClassesCount(), b.getClassesCount() );
		Assert.assertArrayEquals( a.getDataNames(), b.getDataNames() );

		List<Node> aNodes = Arrays.asList( a.getGroups() );
		List<Node> bNodes = Arrays.asList( b.getGroups() );
		Assert.assertEquals( aNodes.size(), bNodes.size() );

		final int _endi = aNodes.size();
		for ( int i = 0; i < _endi; ++i ) {
			Node aN = aNodes.get( i );
			Node bN = bNodes.get( i );

			compareNodes( aN, bN );
		}
	}

	public static void compareNodes( Node a, Node b )
	{
		Assert.assertFalse( a == b );
		Assert.assertEquals( a.getId(), b.getId() );
		if ( a.getParent() != null ) {
			Assert.assertEquals( a.getParent().getId(), b.getParent().getId() );
		}

		// Node representations are created artificially, therefore they can't
		// reference the same feature values array object.
		compareInstances( a.getNodeRepresentation(), b.getNodeRepresentation(), false );

		List<Instance> aIs = a.getNodeInstances();
		List<Instance> bIs = b.getNodeInstances();
		Assert.assertEquals( aIs.size(), bIs.size() );
		final int _endi = aIs.size();
		for ( int i = 0; i < _endi; ++i ) {
			compareInstances( aIs.get( i ), bIs.get( i ), _featureArrayOptimization );
		}
	}

	/**
	 * @param a
	 *            the first instance to compare
	 * @param b
	 *            the second instance to compare
	 * @param featureArrayOptimization
	 *            whether to assume that when instances are cloned, their feature values'
	 *            array is copied by reference instead of deeply cloned (memory optimization)
	 */
	public static void compareInstances( Instance a, Instance b, boolean featureArrayOptimization )
	{
		if ( a == null && b == null )
			return;

		Assert.assertFalse( a == b );
		Assert.assertEquals( a.getNodeId(), b.getNodeId() );
		Assert.assertEquals( a.getTrueClass(), b.getTrueClass() );
		Assert.assertEquals( a.getInstanceName(), b.getInstanceName() );
		if ( featureArrayOptimization ) {
			Assert.assertTrue( a.getData() == b.getData() );
		}
		else {
			Assert.assertFalse( a.getData() == b.getData() );
			Assert.assertArrayEquals( a.getData(), b.getData(), 0 );
		}
	}

	// -------------------------------------------------------------

	public BasicHierarchy generateHierarchy( int instanceCount, int dimCount, String... ids )
	{
		Random r = new Random();

		int nodeCount = ids.length;
		int currentInstanceCount = 0;
		final int avgInstancePerNode = instanceCount / nodeCount;

		List<BasicNode> nodes = new ArrayList<>();
		for ( int i = 0; i < nodeCount; ++i ) {
			int nodeInstanceCount = Math.max( 1, (int)( avgInstancePerNode * ( r.nextGaussian() + 1 ) ) );
			if ( i == nodeCount - 1 && currentInstanceCount + nodeInstanceCount < instanceCount ) {
				nodeInstanceCount = instanceCount - currentInstanceCount;
			}

			currentInstanceCount += nodeInstanceCount;
			nodes.add( generateNode( ids[i], nodeInstanceCount, dimCount ) );
		}

		nodes.sort( new NodeIdComparator() );
		BasicNode root = nodes.get( 0 );
		if ( !root.getId().equals( Constants.ROOT_ID ) ) {
			root = null;
		}

		HierarchyBuilder hb = new HierarchyBuilder();
		List<? extends Node> allNodes = hb.buildCompleteHierarchy( root, nodes, false, false );

		return new BasicHierarchy( allNodes, null );
	}

	public BasicNode generateNode( String id, int instanceCount, int dimCount )
	{
		BasicNode node = new BasicNode( id, null, false );
		for ( int i = 0; i < instanceCount; ++i ) {
			node.addInstance( generateInstance( id, dimCount ) );
		}
		return node;
	}

	public BasicInstance generateInstance( String id, int dimCount )
	{
		double[] data = new double[dimCount];
		for ( int i = 0; i < dimCount; i++ ) {
			data[i] = Math.random() * 2 - 1;
		}
		return new BasicInstance( null, id, data );
	}
}
