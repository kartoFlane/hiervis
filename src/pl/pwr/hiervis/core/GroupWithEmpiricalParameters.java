package pl.pwr.hiervis.core;

import pl.pwr.basic_hierarchy.interfaces.Instance;
import pl.pwr.basic_hierarchy.interfaces.Node;


public class GroupWithEmpiricalParameters
{
	private Node node;
	private double[][] empiricalCovariance;
	private double[] empiricalMean;
	private int numOfDimensions;


	public GroupWithEmpiricalParameters( Node node )
	{
		this.node = node;
		this.numOfDimensions = getNumberOfDimensions( node );
		this.empiricalMean = calculateMean( node, numOfDimensions );
		this.empiricalCovariance = calculateCovariance( node, empiricalMean, numOfDimensions );
	}

	private int getNumberOfDimensions( Node node )
	{
		int numOfDimensions = -1;
		Node curr = node;
		do {
			if ( !curr.getSubtreeInstances().isEmpty() ) {
				numOfDimensions = curr.getSubtreeInstances().getFirst().getData().length;
			}
		} while ( numOfDimensions == -1 && ( curr = curr.getParent() ) != null );

		if ( numOfDimensions == -1 ) {
			System.err.print( "GroupWithParameters.getNumberOfDimensions()" );
			System.err.println(
				" cannot determine the number of dimensions! It means that "
					+ "there exists a tree branch without any instance in it. It should not happened."
			);
			System.exit( 1 );
		}

		return numOfDimensions;
	}

	private double[] calculateMean( Node node, int numOfDimensions )
	{
		double[] empiricalMean = new double[numOfDimensions];

		for ( Instance i : node.getSubtreeInstances() ) {
			for ( int dim = 0; dim < numOfDimensions; dim++ ) {
				empiricalMean[dim] += i.getData()[dim];
			}
		}

		for ( int dim = 0; dim < numOfDimensions; dim++ ) {
			empiricalMean[dim] = empiricalMean[dim] / node.getSubtreeInstances().size();
		}

		return empiricalMean;
	}

	private double[][] calculateCovariance( Node node, double[] empiricalMean, int numOfDimensions )
	{
		double[][] empiricalCovariance = new double[numOfDimensions][numOfDimensions];

		for ( Instance inst : node.getSubtreeInstances() ) {
			for ( int i = 0; i < numOfDimensions; i++ ) {
				for ( int j = 0; j <= i; j++ ) {
					empiricalCovariance[i][j] += ( inst.getData()[i] - empiricalMean[i] ) * ( inst.getData()[j] - empiricalMean[j] );
				}
			}
		}

		for ( int i = 0; i < numOfDimensions; i++ ) {
			for ( int j = 0; j <= i; j++ ) {
				empiricalCovariance[i][j] /= Math.max( node.getSubtreeInstances().size() - 1, 1 );// sample variance
				empiricalCovariance[j][i] = empiricalCovariance[i][j];
			}
		}

		return empiricalCovariance;
	}

	public String getId()
	{
		return node.getId();
	}

	public double[][] getEmpiricalCovariance()
	{
		return empiricalCovariance;
	}

	public double[] getEmpiricalMean()
	{
		return empiricalMean;
	}
}
