package pl.pwr.hiervis.measures;

import java.util.function.Function;

import basic_hierarchy.interfaces.Hierarchy;
import distance_measures.Euclidean;
import external_measures.AdaptedFmeasure;
import external_measures.information_based.FlatEntropy1;
import external_measures.information_based.FlatEntropy2;
import external_measures.information_based.FlatInformationGain;
import external_measures.information_based.FlatMutualInformation;
import external_measures.information_based.FlatNormalizedMutualInformation;
import external_measures.purity.FlatClusterPurity;
import external_measures.purity.HierarchicalClassPurity;
import external_measures.statistical_hypothesis.FlatHypotheses;
import external_measures.statistical_hypothesis.Fmeasure;
import external_measures.statistical_hypothesis.FowlkesMallowsIndex;
import external_measures.statistical_hypothesis.JaccardIndex;
import external_measures.statistical_hypothesis.RandIndex;
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
	// External Measures
	/** {@link AdaptedFmeasure} */
	public static final MeasureTask adaptedF = new MeasureTask(
		"Adapted F-Measure",
		new AdaptedFmeasure( false )::getMeasure
	);

	// ---------------------------------------------------------------
	// External Measures - Information Based
	/** {@link FlatEntropy1} */
	public static final MeasureTask flatEntropy1 = new MeasureTask(
		"Flat Entropy 1",
		new FlatEntropy1()::getMeasure
	);

	/** {@link FlatEntropy2} */
	public static final MeasureTask flatEntropy2 = new MeasureTask(
		"Flat Entropy 2",
		new FlatEntropy2()::getMeasure
	);

	/** {@link FlatInformationGain} */
	public static final MeasureTask flatInformationGain = new MeasureTask(
		"Flat Information Gain [Flat Entropy 1]",
		new FlatInformationGain( 2, new FlatEntropy1() )::getMeasure
	);

	/** {@link FlatMutualInformation} */
	public static final MeasureTask flatMutualInformation = new MeasureTask(
		"Flat Mutual Information",
		new FlatMutualInformation()::getMeasure
	);

	/** {@link FlatNormalizedMutualInformation} */
	public static final MeasureTask flatMutualInformationNormalized = new MeasureTask(
		"Flat Normalized Mutual Information",
		new FlatNormalizedMutualInformation()::getMeasure
	);

	// ---------------------------------------------------------------
	// External Measures - Purity
	/** {@link FlatClusterPurity} */
	public static final MeasureTask flatClusterPurity = new MeasureTask(
		"Flat Cluster Purity",
		new FlatClusterPurity()::getMeasure
	);

	/** {@link HierarchicalClassPurity} */
	public static final MeasureTask hierarchicalClassPurity = new MeasureTask(
		"Hierarchical Class Purity",
		new HierarchicalClassPurity()::getMeasure
	);

	// ---------------------------------------------------------------
	// External Measures - Statistical Hypothesis
	/** {@link Fmeasure} */
	public static final MeasureTask fMeasure = new MeasureTask(
		"F-Measure",
		new Fmeasure( 1.0f, new FlatHypotheses() )::getMeasure
	);

	/** {@link FowlkesMallowsIndex} */
	public static final MeasureTask fowlkesMallowsIndex = new MeasureTask(
		"Fowlkes-Mallows Index",
		new FowlkesMallowsIndex( new FlatHypotheses() )::getMeasure
	);

	/** {@link JaccardIndex} */
	public static final MeasureTask jaccardIndex = new MeasureTask(
		"Jaccard Index",
		new JaccardIndex( new FlatHypotheses() )::getMeasure
	);

	/** {@link RandIndex} */
	public static final MeasureTask randIndex = new MeasureTask(
		"Rand Index",
		new RandIndex( new FlatHypotheses() )::getMeasure
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
