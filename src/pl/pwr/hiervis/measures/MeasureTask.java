package pl.pwr.hiervis.measures;

import java.nio.file.Paths;
import java.util.function.Function;

import basic_hierarchy.interfaces.Hierarchy;


/**
 * A pair that associates a measure calculation function with a user-friendly unique identifier.
 * 
 * @author Tomasz Bachmiński
 *
 */
public final class MeasureTask
{
	private static MeasureTaskFactory factory = new JavascriptMeasureTaskFactory( false );

	// ---------------------------------------------------------------
	// Statistics
	public static final MeasureTask numberOfNodes = factory.getMeasureTask(
		Paths.get( "scripts/measures/internal/statistics/NumberOfNodes.js" )
	);

	public static final MeasureTask numberOfLeaves = factory.getMeasureTask(
		Paths.get( "scripts/measures/internal/statistics/NumberOfLeaves.js" )
	);

	public static final MeasureTask height = factory.getMeasureTask(
		Paths.get( "scripts/measures/internal/statistics/Height.js" )
	);

	public static final MeasureTask averagePathLength = factory.getMeasureTask(
		Paths.get( "scripts/measures/internal/statistics/AveragePathLength.js" )
	);

	// ---------------------------------------------------------------
	// Internal Measures
	public static final MeasureTask varianceDeviation = factory.getMeasureTask(
		Paths.get( "scripts/measures/internal/VarianceDeviation.js" )
	);

	public static final MeasureTask varianceDeviation2 = factory.getMeasureTask(
		Paths.get( "scripts/measures/internal/VarianceDeviation2.js" )
	);

	public static final MeasureTask flatWithinBetween = factory.getMeasureTask(
		Paths.get( "scripts/measures/internal/FlatWithinBetweenIndex-Euclidean.js" )
	);

	public static final MeasureTask flatDunn1 = factory.getMeasureTask(
		Paths.get( "scripts/measures/internal/FlatDunn1-Euclidean.js" )
	);

	public static final MeasureTask flatDunn2 = factory.getMeasureTask(
		Paths.get( "scripts/measures/internal/FlatDunn2-Euclidean.js" )
	);

	public static final MeasureTask flatDunn3 = factory.getMeasureTask(
		Paths.get( "scripts/measures/internal/FlatDunn3-Euclidean.js" )
	);

	public static final MeasureTask flatDunn4 = factory.getMeasureTask(
		Paths.get( "scripts/measures/internal/FlatDunn4-Euclidean.js" )
	);

	public static final MeasureTask flatDaviesBouldin = factory.getMeasureTask(
		Paths.get( "scripts/measures/internal/FlatDaviesBouldin-Euclidean.js" )
	);

	public static final MeasureTask flatCalinskiHarabasz = factory.getMeasureTask(
		Paths.get( "scripts/measures/internal/FlatCalinskiHarabasz-Euclidean.js" )
	);

	// ---------------------------------------------------------------
	// External Measures
	public static final MeasureTask adaptedF = factory.getMeasureTask(
		Paths.get( "scripts/measures/external/AdaptedFMeasure.js" )
	);

	// ---------------------------------------------------------------
	// External Measures - Information Based
	public static final MeasureTask flatEntropy1 = factory.getMeasureTask(
		Paths.get( "scripts/measures/external/information_based/FlatEntropy1.js" )
	);

	public static final MeasureTask flatEntropy2 = factory.getMeasureTask(
		Paths.get( "scripts/measures/external/information_based/FlatEntropy2.js" )
	);

	public static final MeasureTask flatInformationGain = factory.getMeasureTask(
		Paths.get( "scripts/measures/external/information_based/FlatInformationGain-2-Flat1.js" )
	);

	public static final MeasureTask flatMutualInformation = factory.getMeasureTask(
		Paths.get( "scripts/measures/external/information_based/FlatMutualInformation.js" )
	);

	public static final MeasureTask flatMutualInformationNormalized = factory.getMeasureTask(
		Paths.get( "scripts/measures/external/information_based/FlatNormalizedMutualInformation.js" )
	);

	// ---------------------------------------------------------------
	// External Measures - Purity
	public static final MeasureTask flatClusterPurity = factory.getMeasureTask(
		Paths.get( "scripts/measures/external/purity/FlatClusterPurity.js" )
	);

	public static final MeasureTask hierarchicalClassPurity = factory.getMeasureTask(
		Paths.get( "scripts/measures/external/purity/HierarchicalClassPurity.js" )
	);

	// ---------------------------------------------------------------
	// External Measures - Statistical Hypothesis
	public static final MeasureTask fMeasure = factory.getMeasureTask(
		Paths.get( "scripts/measures/external/statistical_hypothesis/FMeasure-1-Flat.js" )
	);

	public static final MeasureTask fowlkesMallowsIndex = factory.getMeasureTask(
		Paths.get( "scripts/measures/external/statistical_hypothesis/FowlkesMallowsIndex-Flat.js" )
	);

	public static final MeasureTask jaccardIndex = factory.getMeasureTask(
		Paths.get( "scripts/measures/external/statistical_hypothesis/JaccardIndex-Flat.js" )
	);

	public static final MeasureTask randIndex = factory.getMeasureTask(
		Paths.get( "scripts/measures/external/statistical_hypothesis/RandIndex-Flat.js" )
	);

	// ---------------------------------------------------------------
	// Histograms
	public static final MeasureTask nodesPerLevel = factory.getMeasureTask(
		Paths.get( "scripts/measures/internal/statistics/histogram/NodesPerLevel.js" )
	);

	public static final MeasureTask leavesPerLevel = factory.getMeasureTask(
		Paths.get( "scripts/measures/internal/statistics/histogram/LeavesPerLevel.js" )
	);

	public static final MeasureTask instancesPerLevel = factory.getMeasureTask(
		Paths.get( "scripts/measures/internal/statistics/histogram/InstancesPerLevel.js" )
	);

	public static final MeasureTask childrenPerNodePerLevel = factory.getMeasureTask(
		Paths.get( "scripts/measures/internal/statistics/histogram/ChildrenPerNodePerLevel.js" )
	);

	public static final MeasureTask numberOfChildren = factory.getMeasureTask(
		Paths.get( "scripts/measures/internal/statistics/histogram/NumberOfChildren.js" )
	);

	// ---------------------------------------------------------------
	// Members
	// Both fields are immutable, so it should be safe to expose them.
	public final String identifier;
	public final Function<Hierarchy, Object> function;


	/**
	 * 
	 * @param identifier
	 *            Name of the computed measure. This will be displayed in the interface for the user to see.
	 * @param function
	 *            The function that will compute the measure
	 */
	public MeasureTask( String identifier, Function<Hierarchy, Object> function )
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
