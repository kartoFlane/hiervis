package pl.pwr.hiervis.core;

import java.util.function.Function;

import basic_hierarchy.interfaces.Hierarchy;
import distance_measures.Euclidean;
import internal_measures.FlatCalinskiHarabasz;
import internal_measures.FlatDaviesBouldin;
import internal_measures.FlatDunn1;
import internal_measures.FlatDunn2;
import internal_measures.FlatDunn3;
import internal_measures.FlatDunn4;
import internal_measures.FlatWithinBetweenIndex;
import internal_measures.VarianceDeviation;
import internal_measures.VarianceDeviation2;
import internal_measures.statistics.AvgPathLength;
import internal_measures.statistics.Height;
import internal_measures.statistics.NumberOfLeaves;
import internal_measures.statistics.NumberOfNodes;
import internal_measures.statistics.histogram.ChildPerNodePerLevel;
import internal_measures.statistics.histogram.HistogramOfNumberOfChildren;
import internal_measures.statistics.histogram.InstancesPerLevel;
import internal_measures.statistics.histogram.LeavesPerLevel;
import internal_measures.statistics.histogram.NodesPerLevel;


/**
 * A pair that associates a measure calculation function with a user-friendly unique identifier.
 * 
 * @author Tomasz Bachmiński
 *
 */
public final class MeasureTask
{
	// ---------------------------------------------------------------
	// Statistics
	/** {@link NumberOfNodes} */
	public static final MeasureTask numberOfNodes = new MeasureTask(
		"Number of Nodes",
		new NumberOfNodes()::calculate
	);
	/** {@link NumberOfLeaves} */
	public static final MeasureTask numberOfLeaves = new MeasureTask(
		"Number of Leaves",
		new NumberOfLeaves()::calculate
	);
	/** {@link Height} */
	public static final MeasureTask height = new MeasureTask(
		"Height",
		new Height()::calculate
	);
	/** {@link AvgPathLength} */
	public static final MeasureTask averagePathLength = new MeasureTask(
		"Average Path Length",
		new AvgPathLength()::calculate
	);

	// ---------------------------------------------------------------
	// Internal Measures
	/** {@link VarianceDeviation} */
	public static final MeasureTask varianceDeviation = new MeasureTask(
		"Variance Deviation",
		new VarianceDeviation( 1.0 )::getMeasure
	);
	/** {@link VarianceDeviation2} */
	public static final MeasureTask varianceDeviation2 = new MeasureTask(
		"Variance Deviation 2",
		new VarianceDeviation2()::getMeasure
	);
	/** {@link FlatWithinBetweenIndex} */
	public static final MeasureTask flatWithinBetween = new MeasureTask(
		"Flat Within Between Index",
		new FlatWithinBetweenIndex( new Euclidean() )::getMeasure
	);
	/** {@link FlatDunn1} */
	public static final MeasureTask flatDunn1 = new MeasureTask(
		"Flat Dunn 1",
		new FlatDunn1( new Euclidean() )::getMeasure
	);
	/** {@link FlatDunn2} */
	public static final MeasureTask flatDunn2 = new MeasureTask(
		"Flat Dunn 2",
		new FlatDunn2( new Euclidean() )::getMeasure
	);
	/** {@link FlatDunn3} */
	public static final MeasureTask flatDunn3 = new MeasureTask(
		"Flat Dunn 3",
		new FlatDunn3( new Euclidean() )::getMeasure
	);
	/** {@link FlatDunn4} */
	public static final MeasureTask flatDunn4 = new MeasureTask(
		"Flat Dunn 4",
		new FlatDunn4( new Euclidean() )::getMeasure
	);
	/** {@link FlatDaviesBouldin} */
	public static final MeasureTask flatDaviesBouldin = new MeasureTask(
		"Flat Davies-Bouldin",
		new FlatDaviesBouldin( new Euclidean() )::getMeasure
	);
	/** {@link FlatCalinskiHarabasz} */
	public static final MeasureTask flatCalinskiHarabasz = new MeasureTask(
		"Flat Calinski-Harabasz",
		new FlatCalinskiHarabasz( new Euclidean() )::getMeasure
	);

	// ---------------------------------------------------------------
	// Histograms
	/** {@link NodesPerLevel} */
	public static final MeasureTask nodesPerLevel = new MeasureTask(
		"Nodes Per Level",
		new NodesPerLevel()::calculate
	);
	/** {@link LeavesPerLevel} */
	public static final MeasureTask leavesPerLevel = new MeasureTask(
		"Leaves Per Level",
		new LeavesPerLevel()::calculate
	);
	/** {@link InstancesPerLevel} */
	public static final MeasureTask instancesPerLevel = new MeasureTask(
		"Instances Per Level",
		new InstancesPerLevel()::calculate
	);
	/** {@link ChildPerNodePerLevel} */
	public static final MeasureTask childrenPerNodePerLevel = new MeasureTask(
		"Children Per Node Per Level",
		new ChildPerNodePerLevel()::calculate
	);
	/** {@link HistogramOfNumberOfChildren} */
	public static final MeasureTask numberOfChildren = new MeasureTask(
		"Number of Children",
		new HistogramOfNumberOfChildren()::calculate
	);

	// ---------------------------------------------------------------
	// Members
	// Both fields are immutable, so it should be safe to expose them.
	public final String identifier;
	public final Function<Hierarchy, Object> function;
	// TODO: Maybe also include the result type as a field?


	/**
	 * 
	 * @param identifier
	 *            Name of the computed measure. This will be displayed in the interface for the user to see.
	 * @param function
	 *            The function that will compute the measure
	 */
	private MeasureTask( String identifier, Function<Hierarchy, Object> function )
	{
		if ( identifier == null || identifier.isEmpty() ) {
			throw new IllegalArgumentException( "Identifier is null or an empty string!" );
		}
		if ( function == null ) {
			throw new IllegalArgumentException( "Function is null!" );
		}
		this.identifier = identifier;
		this.function = function;
	}

	@Override
	public int hashCode()
	{
		return identifier.hashCode();
	}

	@Override
	public boolean equals( Object o )
	{
		if ( o instanceof MeasureTask ) {
			return equals( (MeasureTask)o );
		}
		return false;
	}

	public boolean equals( MeasureTask mt )
	{
		return identifier.equals( mt.identifier );
	}
}
