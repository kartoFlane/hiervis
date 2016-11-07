package pl.pwr.hiervis.core;

import java.io.PrintWriter;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;

import basic_hierarchy.interfaces.Group;
import basic_hierarchy.interfaces.Hierarchy;


public class HierarchyStatistics
{
	private int[] eachLevelNumberOfInstances;

	private LinkedList<Double> numberOfChildPerNode;
	private double avgNumberOfChildPerNode;
	private double stdevNumberOfChildPerNode;

	private LinkedList<Double> numberOfChildPerInternalNode;
	private double avgNumberOfChildPerInternalNode;
	private double stdevNumberOfChildPerInternalNode;

	private LinkedList<Double> numberOfInstancesPerNode;
	private double stdevNumberOfInstancesPerNode;
	private double avgNumberOfInstancesPerNode;

	private LinkedList<Double> numberOfChildPerNodeWithSpecifiedBranchingFactor;
	private double avgNumberOfChildPerNodeWithSpecifiedBranchingFactor;
	private double stdevNumberOfChildPerNodeWithSpecifiedBranchingFactor;

	private LinkedList<GroupWithEmpiricalParameters> nodesEstimatedParameters;
	private int overallNumberOfInstances;
	private String statsToVisualiseOnImages, statsToWriteOutInFile;
	private HashMap<Integer, Integer> nodeBranchFactorAndCountOfNodesWithThatFactor;

	private double[] avgNumberOfChildrenPerNodeOnEachHeight;
	private double[] stdevNumberOfChildrenPerNodeOnEachHeight;

	private double[] hierarchyWidthOnEachHeight;
	private double avgHierarchyWidth;
	private double stdevHierarchyWidth;

	private double[] numberOfLeavesOnEachHeight;
	private int numberOfLeaves;

	private LinkedList<Double> pathLength;
	private double avgPathLength;
	private double stdevPathLength;


	public HierarchyStatistics( Hierarchy h, String statisticsFilePath )
	{
		calculate( h, statisticsFilePath );
	}

	public void calculate( Hierarchy h, String statisticsFilePath )
	{
		int minimumBranchingFactor = 2;
		traverseHierarchyAndCalculateMeasures( h, minimumBranchingFactor );
		calculateEmpiricalMeanAndVariancesOfEachGroup( h );
		createSummaryStatsBuffered();
		saveStatistics( statisticsFilePath, minimumBranchingFactor );
	}

	private void traverseHierarchyAndCalculateMeasures( Hierarchy h, int minBranchingFactor )
	{
		int hierarchyHeight = getHierarchyHeight( h );
		eachLevelNumberOfInstances = new int[hierarchyHeight + 1];
		overallNumberOfInstances = 0;
		nodeBranchFactorAndCountOfNodesWithThatFactor = new HashMap<Integer, Integer>();
		avgNumberOfChildrenPerNodeOnEachHeight = new double[hierarchyHeight + 1];
		stdevNumberOfChildrenPerNodeOnEachHeight = new double[hierarchyHeight + 1];
		LinkedList<AbstractMap.SimpleEntry<Integer, Integer>> nodeHeightWithItsChildrenCount = new LinkedList<>();
		hierarchyWidthOnEachHeight = new double[hierarchyHeight + 1];
		numberOfLeavesOnEachHeight = new double[hierarchyHeight + 1];
		numberOfChildPerNode = new LinkedList<>();
		numberOfChildPerInternalNode = new LinkedList<>();
		numberOfChildPerNodeWithSpecifiedBranchingFactor = new LinkedList<>();
		numberOfInstancesPerNode = new LinkedList<>();
		pathLength = new LinkedList<>();


		Stack<AbstractMap.SimpleEntry<Group, Integer>> s = new Stack<>();// node and lvl number
		s.push( new AbstractMap.SimpleEntry<Group, Integer>( h.getRoot(), 0 ) );

		while ( !s.isEmpty() ) {
			AbstractMap.SimpleEntry<Group, Integer> curr = s.pop();
			Group currentNode = curr.getKey();
			int height = curr.getValue();

			for ( Group ch : currentNode.getChildren() ) {
				s.push( new AbstractMap.SimpleEntry<Group, Integer>( ch, curr.getValue() + 1 ) );
			}

			numberOfInstancesPerNode.add( (double)currentNode.getInstances().size() );

			numberOfChildPerNode.add( (double)currentNode.getChildren().size() );

			if ( !currentNode.getChildren().isEmpty() ) {
				numberOfChildPerInternalNode.add( (double)currentNode.getChildren().size() );
			}
			else {
				numberOfLeavesOnEachHeight[height] += 1;
				numberOfLeaves += 1;
				pathLength.add( (double)height );
			}

			if ( currentNode.getChildren().size() >= minBranchingFactor ) {
				numberOfChildPerNodeWithSpecifiedBranchingFactor.add( (double)currentNode.getChildren().size() );
			}

			hierarchyWidthOnEachHeight[height] += 1;
			eachLevelNumberOfInstances[height] += currentNode.getInstances().size();
			overallNumberOfInstances += currentNode.getInstances().size();

			Integer numOfChildren = currentNode.getChildren().size();
			nodeHeightWithItsChildrenCount.add( new AbstractMap.SimpleEntry<Integer, Integer>( height, numOfChildren ) );
			avgNumberOfChildrenPerNodeOnEachHeight[height] += numOfChildren;

			if ( nodeBranchFactorAndCountOfNodesWithThatFactor.containsKey( numOfChildren ) ) {
				Integer incrementedCount = nodeBranchFactorAndCountOfNodesWithThatFactor.get( numOfChildren ) + 1;
				nodeBranchFactorAndCountOfNodesWithThatFactor.put( numOfChildren, incrementedCount );
			}
			else {
				nodeBranchFactorAndCountOfNodesWithThatFactor.put( numOfChildren, 1 );
			}
		}

		postprocessObtainedData( nodeHeightWithItsChildrenCount );
	}

	private void postprocessObtainedData( LinkedList<SimpleEntry<Integer, Integer>> nodeHeightWithItsChildrenCount )
	{
		StandardDeviation stdev = new StandardDeviation( true );
		Mean mean = new Mean();
		double[] primitives;

		primitives = ArrayUtils.toPrimitive( numberOfInstancesPerNode.toArray( new Double[numberOfInstancesPerNode.size()] ) );
		avgNumberOfInstancesPerNode = mean.evaluate( primitives );
		stdevNumberOfInstancesPerNode = stdev.evaluate( primitives );

		primitives = ArrayUtils.toPrimitive( numberOfChildPerNode.toArray( new Double[numberOfChildPerNode.size()] ) );
		avgNumberOfChildPerNode = mean.evaluate( primitives );
		stdevNumberOfChildPerNode = stdev.evaluate( primitives );

		primitives = ArrayUtils.toPrimitive( numberOfChildPerInternalNode.toArray( new Double[numberOfChildPerInternalNode.size()] ) );
		avgNumberOfChildPerInternalNode = mean.evaluate( primitives );
		stdevNumberOfChildPerInternalNode = stdev.evaluate( primitives );

		primitives = ArrayUtils.toPrimitive(
			numberOfChildPerNodeWithSpecifiedBranchingFactor
				.toArray( new Double[numberOfChildPerNodeWithSpecifiedBranchingFactor.size()] )
		);
		avgNumberOfChildPerNodeWithSpecifiedBranchingFactor = mean.evaluate( primitives );
		stdevNumberOfChildPerNodeWithSpecifiedBranchingFactor = stdev.evaluate( primitives );

		avgHierarchyWidth = mean.evaluate( hierarchyWidthOnEachHeight );
		stdevHierarchyWidth = stdev.evaluate( hierarchyWidthOnEachHeight );

		primitives = ArrayUtils.toPrimitive( pathLength.toArray( new Double[pathLength.size()] ) );
		avgPathLength = mean.evaluate( primitives );
		stdevPathLength = stdev.evaluate( primitives );

		// filling gaps in histogram
		Set<Integer> keyes = nodeBranchFactorAndCountOfNodesWithThatFactor.keySet();
		int maxKey = Collections.max( keyes );
		int minKey = Collections.min( keyes );

		for ( int i = minKey + 1; i < maxKey; i++ ) {
			if ( !nodeBranchFactorAndCountOfNodesWithThatFactor.containsKey( i ) ) {
				nodeBranchFactorAndCountOfNodesWithThatFactor.put( i, 0 );
			}
		}

		for ( int i = 0; i < avgNumberOfChildrenPerNodeOnEachHeight.length; i++ ) {
			avgNumberOfChildrenPerNodeOnEachHeight[i] /= hierarchyWidthOnEachHeight[i];
		}

		for ( AbstractMap.SimpleEntry<Integer, Integer> elem : nodeHeightWithItsChildrenCount ) {
			int height = elem.getKey();
			int count = elem.getValue();
			stdevNumberOfChildrenPerNodeOnEachHeight[height] += ( count - avgNumberOfChildrenPerNodeOnEachHeight[height] )
				* ( count - avgNumberOfChildrenPerNodeOnEachHeight[height] );
		}

		for ( int i = 0; i < avgNumberOfChildrenPerNodeOnEachHeight.length; i++ ) {
			stdevNumberOfChildrenPerNodeOnEachHeight[i] /= hierarchyWidthOnEachHeight[i];
		}

	}

	private void calculateEmpiricalMeanAndVariancesOfEachGroup( Hierarchy h )
	{
		nodesEstimatedParameters = new LinkedList<GroupWithEmpiricalParameters>();

		LinkedList<Group> s = new LinkedList<>();
		s.add( h.getRoot() );

		while ( !s.isEmpty() ) {
			Group curr = s.removeLast();
			for ( int i = curr.getChildren().size() - 1; i >= 0; --i ) {
				Group ch = curr.getChildren().get( i );
				s.add( ch );
			}

			nodesEstimatedParameters.add( new GroupWithEmpiricalParameters( curr ) );
		}
	}

	private void saveStatistics( String statisticsFilePath, int minimumBranchingFactor )
	{
		PrintWriter pw;
		try {
			pw = new PrintWriter( statisticsFilePath, "UTF-8" );
			pw.print( getStatisticFileContentBuffered( minimumBranchingFactor ) );
			pw.close();
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
	}

	private String getStatisticFileContentBuffered( int minimumBranchingFactor )
	{
		StringBuilder buf = new StringBuilder();

		buf.append( "Total num of instances" )
			.append( HVConstants.CSV_FILE_SEPARATOR )
			.append( overallNumberOfInstances ).append( '\n' );

		buf.append( "Avg num of children per node" )
			.append( HVConstants.CSV_FILE_SEPARATOR )
			.append( avgNumberOfChildPerNode ).append( '\n' );

		buf.append( "Sample stdev num of children per node" )
			.append( HVConstants.CSV_FILE_SEPARATOR )
			.append( stdevNumberOfChildPerNode ).append( '\n' );

		buf.append( "Avg num of children per INTERNAL node" )
			.append( HVConstants.CSV_FILE_SEPARATOR )
			.append( avgNumberOfChildPerInternalNode ).append( '\n' );

		buf.append( "Sample stdev num of children per INTERNAL node" )
			.append( HVConstants.CSV_FILE_SEPARATOR )
			.append( stdevNumberOfChildPerInternalNode ).append( '\n' );

		buf.append( "Avg num of children per INTERNAL node with MIN BRANCHING FACTOR " )
			.append( minimumBranchingFactor )
			.append( HVConstants.CSV_FILE_SEPARATOR )
			.append( avgNumberOfChildPerNodeWithSpecifiedBranchingFactor ).append( '\n' );

		buf.append( "Sample stdev num of children per INTERNAL node with MIN BRANCHING FACTOR " )
			.append( minimumBranchingFactor )
			.append( HVConstants.CSV_FILE_SEPARATOR )
			.append( stdevNumberOfChildPerNodeWithSpecifiedBranchingFactor ).append( '\n' );

		buf.append( "Avg num of instances per node" )
			.append( HVConstants.CSV_FILE_SEPARATOR )
			.append( avgNumberOfInstancesPerNode ).append( '\n' );

		buf.append( "Sample stdev num of instances per node" )
			.append( HVConstants.CSV_FILE_SEPARATOR )
			.append( stdevNumberOfInstancesPerNode ).append( '\n' );

		buf.append( "Hierarchy height" )
			.append( HVConstants.CSV_FILE_SEPARATOR )
			.append( eachLevelNumberOfInstances.length - 1 ).append( '\n' );

		buf.append( "Avg hierarchy width" )
			.append( HVConstants.CSV_FILE_SEPARATOR )
			.append( avgHierarchyWidth ).append( '\n' );

		buf.append( "Sample stdev hierarchy width" )
			.append( HVConstants.CSV_FILE_SEPARATOR )
			.append( stdevHierarchyWidth ).append( '\n' );

		buf.append( "Number of nodes" )
			.append( HVConstants.CSV_FILE_SEPARATOR )
			.append( numberOfChildPerNode.size() ).append( '\n' );

		buf.append( "Number of INTERNAL nodes" )
			.append( HVConstants.CSV_FILE_SEPARATOR )
			.append( numberOfChildPerInternalNode.size() ).append( '\n' );

		buf.append( "Number of INTERNAL nodes with MIN BRANCHING FACTOR " )
			.append( minimumBranchingFactor )
			.append( HVConstants.CSV_FILE_SEPARATOR )
			.append( numberOfChildPerNodeWithSpecifiedBranchingFactor.size() ).append( '\n' );

		buf.append( "Number of leaves" )
			.append( HVConstants.CSV_FILE_SEPARATOR )
			.append( numberOfLeaves ).append( '\n' );

		buf.append( "Avg path length" )
			.append( HVConstants.CSV_FILE_SEPARATOR )
			.append( avgPathLength ).append( '\n' );

		buf.append( "Sample stdev path length" )
			.append( HVConstants.CSV_FILE_SEPARATOR )
			.append( stdevPathLength ).append( '\n' );

		buf.append( '\n' ).append( statsToWriteOutInFile ).append( "\n\n" );

		buf.append( "Node" )
			.append( HVConstants.CSV_FILE_SEPARATOR )
			.append( "Mean vector" );

		for ( int i = 0; i < nodesEstimatedParameters.getFirst().getEmpiricalMean().length; ++i ) {
			buf.append( HVConstants.CSV_FILE_SEPARATOR );
		}

		buf.append( "Covariance matrix" ).append( '\n' );

		for ( GroupWithEmpiricalParameters g : nodesEstimatedParameters ) {
			buf.append( g.getId() )
				.append( HVConstants.CSV_FILE_SEPARATOR );

			for ( int i = 0; i < g.getEmpiricalMean().length; ++i ) {
				buf.append( g.getEmpiricalMean()[i] );

				// Don't place separator after last element
				if ( i < g.getEmpiricalMean().length - 1 ) {
					buf.append( HVConstants.CSV_FILE_SEPARATOR );
				}
			}

			// Add the last separator outside of the loop so that we always have a separator, even
			// if the mean array is empty.
			buf.append( HVConstants.CSV_FILE_SEPARATOR );

			for ( int i = 0; i < g.getEmpiricalCovariance().length; ++i ) {
				for ( int j = 0; j < g.getEmpiricalCovariance()[i].length; ++j ) {
					buf.append( g.getEmpiricalCovariance()[i][j] );

					// Don't place separator after last element
					if ( j < g.getEmpiricalCovariance()[i].length - 1 ) {
						buf.append( HVConstants.CSV_FILE_SEPARATOR );
					}
				}

				buf.append( '\n' );

				// Don't place header after last element
				if ( i < g.getEmpiricalCovariance().length - 1 ) {
					buf.append( HVConstants.CSV_FILE_SEPARATOR ); // node ID column
					for ( int k = 0; k < g.getEmpiricalCovariance().length; ++k ) {
						buf.append( HVConstants.CSV_FILE_SEPARATOR );
					}
				}
			}
		}

		return buf.toString();
	}

	private void createSummaryStatsBuffered()
	{
		StringBuilder bufImg = new StringBuilder();
		StringBuilder bufFile = new StringBuilder();

		bufImg.append( "Total number of points" )
			.append( HVConstants.CSV_FILE_SEPARATOR )
			.append( overallNumberOfInstances ).append( '\n' );

		bufImg.append( "Level" )
			.append( HVConstants.CSV_FILE_SEPARATOR )
			.append( "No Inst" )
			.append( HVConstants.CSV_FILE_SEPARATOR )
			.append( "% Inst" );

		bufFile.append( bufImg ); // TODO: Dodac reszte informacji

		bufFile.append( HVConstants.CSV_FILE_SEPARATOR )
			.append( "Avg num of children per node" )
			.append( HVConstants.CSV_FILE_SEPARATOR )
			.append( "Stdev" )
			.append( HVConstants.CSV_FILE_SEPARATOR )
			.append( "Hierarchy width" )
			.append( HVConstants.CSV_FILE_SEPARATOR )
			.append( "Num of leaves" );

		StringBuilder buf = new StringBuilder();
		for ( int i = 0; i < eachLevelNumberOfInstances.length; ++i ) {
			buf.append( '\n' )
				.append( i )
				.append( HVConstants.CSV_FILE_SEPARATOR )
				.append( eachLevelNumberOfInstances[i] )
				.append( HVConstants.CSV_FILE_SEPARATOR )
				.append( Math.round( ( eachLevelNumberOfInstances[i] / (double)overallNumberOfInstances ) * 10000 ) / 100.0 );

			bufImg.append( buf );
			bufFile.append( buf );
			buf.setLength( 0 );

			bufFile.append( HVConstants.CSV_FILE_SEPARATOR )
				.append( avgNumberOfChildrenPerNodeOnEachHeight[i] )
				.append( HVConstants.CSV_FILE_SEPARATOR )
				.append( stdevNumberOfChildrenPerNodeOnEachHeight[i] )
				.append( HVConstants.CSV_FILE_SEPARATOR )
				.append( hierarchyWidthOnEachHeight[i] )
				.append( HVConstants.CSV_FILE_SEPARATOR )
				.append( numberOfLeavesOnEachHeight[i] );
		}

		int minBranchFactor = Collections.min( nodeBranchFactorAndCountOfNodesWithThatFactor.keySet() );
		int maxBranchFactor = Collections.max( nodeBranchFactorAndCountOfNodesWithThatFactor.keySet() );

		bufFile.append( "\n\n" ).append( "Branching factor histogram" )
			.append( '\n' ).append( "Factor:" );
		for ( int i = minBranchFactor; i <= maxBranchFactor; ++i ) {
			bufFile.append( HVConstants.CSV_FILE_SEPARATOR ).append( i );
		}
		bufFile.append( '\n' ).append( "Count:" );
		for ( int i = minBranchFactor; i <= maxBranchFactor; ++i ) {
			bufFile.append( HVConstants.CSV_FILE_SEPARATOR )
				.append( nodeBranchFactorAndCountOfNodesWithThatFactor.get( i ) );
		}

		statsToVisualiseOnImages = bufImg.toString();
		statsToWriteOutInFile = bufFile.toString();
	}

	private String getStatisticFileContent( int minimumBranchingFactor )
	{
		String content = "";

		content += "Total num of instances" + HVConstants.CSV_FILE_SEPARATOR + overallNumberOfInstances + "\n";
		content += "Avg num of children per node" + HVConstants.CSV_FILE_SEPARATOR + avgNumberOfChildPerNode + "\n";
		content += "Sample stdev num of children per node" + HVConstants.CSV_FILE_SEPARATOR + stdevNumberOfChildPerNode + "\n";
		content += "Avg num of children per INTERNAL node" + HVConstants.CSV_FILE_SEPARATOR + avgNumberOfChildPerInternalNode + "\n";
		content += "Sample stdev num of children per INTERNAL node" + HVConstants.CSV_FILE_SEPARATOR + stdevNumberOfChildPerInternalNode + "\n";
		content += "Avg num of children per INTERNAL node with MIN BRANCHING FACTOR " + minimumBranchingFactor + HVConstants.CSV_FILE_SEPARATOR
			+ avgNumberOfChildPerNodeWithSpecifiedBranchingFactor + "\n";
		content += "Sample stdev num of children per INTERNAL node with MIN BRANCHING FACTOR " + minimumBranchingFactor
			+ HVConstants.CSV_FILE_SEPARATOR
			+ stdevNumberOfChildPerNodeWithSpecifiedBranchingFactor + "\n";
		content += "Avg num of instances per node" + HVConstants.CSV_FILE_SEPARATOR + avgNumberOfInstancesPerNode + "\n";
		content += "Sample stdev num of instances per node" + HVConstants.CSV_FILE_SEPARATOR + stdevNumberOfInstancesPerNode + "\n";
		content += "Hierarchy height" + HVConstants.CSV_FILE_SEPARATOR + ( eachLevelNumberOfInstances.length - 1 ) + "\n";
		content += "Avg hierarchy width" + HVConstants.CSV_FILE_SEPARATOR + avgHierarchyWidth + "\n";
		content += "Sample stdev hierarchy width" + HVConstants.CSV_FILE_SEPARATOR + stdevHierarchyWidth + "\n";
		content += "Number of nodes" + HVConstants.CSV_FILE_SEPARATOR + numberOfChildPerNode.size() + "\n";
		content += "Number of INTERNAL nodes" + HVConstants.CSV_FILE_SEPARATOR + numberOfChildPerInternalNode.size() + "\n";
		content += "Number of INTERNAL nodes with MIN BRANCHING FACTOR " + minimumBranchingFactor + HVConstants.CSV_FILE_SEPARATOR
			+ numberOfChildPerNodeWithSpecifiedBranchingFactor.size() + "\n";
		content += "Number of leaves" + HVConstants.CSV_FILE_SEPARATOR + numberOfLeaves + "\n";
		content += "Avg path length" + HVConstants.CSV_FILE_SEPARATOR + avgPathLength + "\n";
		content += "Sample stdev path length" + HVConstants.CSV_FILE_SEPARATOR + stdevPathLength + "\n";

		content += "\n" + statsToWriteOutInFile + "\n\n";

		content += "Node" + HVConstants.CSV_FILE_SEPARATOR + "Mean vector";
		for ( int i = 0; i < nodesEstimatedParameters.getFirst().getEmpiricalMean().length; i++ ) {
			content += HVConstants.CSV_FILE_SEPARATOR;
		}
		content += "Covariance matrix" + "\n";

		for ( GroupWithEmpiricalParameters g : nodesEstimatedParameters ) {
			content += g.getId() + HVConstants.CSV_FILE_SEPARATOR;

			for ( int i = 0; i < g.getEmpiricalMean().length; i++ ) {
				content += g.getEmpiricalMean()[i] + HVConstants.CSV_FILE_SEPARATOR;
			}
			content = content.substring( 0, content.length() - 1 );// trim last separator
			content += HVConstants.CSV_FILE_SEPARATOR;

			for ( int i = 0; i < g.getEmpiricalCovariance().length; i++ ) {
				for ( int j = 0; j < g.getEmpiricalCovariance()[0].length; j++ ) {
					content += g.getEmpiricalCovariance()[i][j] + HVConstants.CSV_FILE_SEPARATOR;
				}

				content = content.substring( 0, content.length() - 1 );// trim last separator
				content += "\n";
				for ( int k = 0; k < g.getEmpiricalCovariance().length + 1; k++ )// +1 because of node id column
				{
					content += HVConstants.CSV_FILE_SEPARATOR;
				}
			}
			content = content.substring( 0, content.length() - g.getEmpiricalCovariance().length - 1 );// trim last spacings, -1 because of node id column
		}

		return content;
	}

	private void createStatsToVisualiseOnOutputImgsAndSummaryFile()
	{
		statsToVisualiseOnImages = "";

		statsToVisualiseOnImages += "Total number of points" + HVConstants.CSV_FILE_SEPARATOR + overallNumberOfInstances + "\n";
		statsToVisualiseOnImages += "Level" + HVConstants.CSV_FILE_SEPARATOR + "No Inst" + HVConstants.CSV_FILE_SEPARATOR + "% Inst";
		statsToWriteOutInFile = statsToVisualiseOnImages;// TODO: dodac reszte informacji
		statsToWriteOutInFile += HVConstants.CSV_FILE_SEPARATOR + "Avg. No of Children per node" + HVConstants.CSV_FILE_SEPARATOR + "Stdev"
			+ HVConstants.CSV_FILE_SEPARATOR + "Hierarchy width" + HVConstants.CSV_FILE_SEPARATOR + "No of leaves";

		for ( int i = 0; i < eachLevelNumberOfInstances.length; i++ ) {
			String results = "\n" + i + HVConstants.CSV_FILE_SEPARATOR + eachLevelNumberOfInstances[i] + HVConstants.CSV_FILE_SEPARATOR +
				Math.round( ( eachLevelNumberOfInstances[i] / (double)overallNumberOfInstances ) * 10000 ) / 100.0;
			statsToVisualiseOnImages += results;
			statsToWriteOutInFile += results;// TODO: dodac reszte informacji
			statsToWriteOutInFile += HVConstants.CSV_FILE_SEPARATOR + avgNumberOfChildrenPerNodeOnEachHeight[i] + HVConstants.CSV_FILE_SEPARATOR
				+ stdevNumberOfChildrenPerNodeOnEachHeight[i] + HVConstants.CSV_FILE_SEPARATOR + hierarchyWidthOnEachHeight[i]
				+ HVConstants.CSV_FILE_SEPARATOR + numberOfLeavesOnEachHeight[i];
		}

		int minBranchFactor = Collections.min( nodeBranchFactorAndCountOfNodesWithThatFactor.keySet() );
		int maxBranchFactor = Collections.max( nodeBranchFactorAndCountOfNodesWithThatFactor.keySet() );
		statsToWriteOutInFile += "\n\nBranching factor histogram\nFactor:";
		for ( int i = minBranchFactor; i <= maxBranchFactor; i++ ) {
			statsToWriteOutInFile += HVConstants.CSV_FILE_SEPARATOR + i;
		}
		statsToWriteOutInFile += "\nCount:";
		for ( int i = minBranchFactor; i <= maxBranchFactor; i++ ) {
			statsToWriteOutInFile += HVConstants.CSV_FILE_SEPARATOR + nodeBranchFactorAndCountOfNodesWithThatFactor.get( i );
		}
	}

	private int getHierarchyHeight( Hierarchy h )
	{
		int height = 0;
		Group root = h.getRoot();
		Stack<AbstractMap.SimpleEntry<Group, Integer>> s = new Stack<>(); // Node and its height
		s.push( new AbstractMap.SimpleEntry<Group, Integer>( root, 0 ) );

		while ( !s.isEmpty() ) {
			AbstractMap.SimpleEntry<Group, Integer> curr = s.pop();
			for ( Group ch : curr.getKey().getChildren() ) {
				s.push( new AbstractMap.SimpleEntry<Group, Integer>( ch, curr.getValue() + 1 ) );
			}

			height = Math.max( height, curr.getValue() );
		}

		return height;
	}

	public int getHierarchyHeight()
	{
		return eachLevelNumberOfInstances.length - 1;
	}

	public int getNumberOfInstances( int levelNumber )
	{
		return eachLevelNumberOfInstances[levelNumber];
	}

	public int getPercentageNumberOfInstances( int levelNumber )
	{
		return eachLevelNumberOfInstances[levelNumber] * 100;
	}

	public String getSummaryString()
	{
		return statsToVisualiseOnImages;
	}

	public double getAvgNumberOfChildPerNode()
	{
		return avgNumberOfChildPerNode;
	}

	public double getAvgNumberOfInstancesPerNode()
	{
		return avgNumberOfInstancesPerNode;
	}

	public GroupWithEmpiricalParameters getEstimatedParameters( String nodeId )
	{
		for ( GroupWithEmpiricalParameters g : nodesEstimatedParameters ) {
			if ( g.getId().equals( nodeId ) ) {
				return g;
			}
		}
		return null;
	}

	public int getOverallNumberOfInstances()
	{
		return overallNumberOfInstances;
	}
}
