package pl.pwr.hiervis.measures;

import java.util.function.Function;

import basic_hierarchy.interfaces.Hierarchy;
import interfaces.QualityMeasure;


/**
 * A pair that associates a measure calculation function with a user-friendly unique identifier.
 * 
 * @author Tomasz Bachmiński
 *
 */
public final class MeasureTask implements Comparable<MeasureTask>
{
	// All fields are immutable, so it should be safe to expose them.
	public final Object measureObject;
	public final String identifier;
	public final boolean autoCompute;
	public final Function<Hierarchy, Boolean> applicabilityFunction;
	public final Function<Hierarchy, Object> computeFunction;


	/**
	 * 
	 * @param measure
	 *            instance of the measure object itself
	 * @param identifier
	 *            Name of the computed measure. This will be displayed in the interface for the user to see.
	 * @param autoCompute
	 *            Whether this measure should be computed automatically as soon as the hierarchy is loaded,
	 *            if the measure is applicable to the hierarchy
	 * @param applicabilityFunction
	 *            The function that returns a boolean value indicating whether the measure is applicable for
	 *            the currently loaded hierarchy. Null if the measure is always applicable.
	 * @param computeFunction
	 *            The function that will compute the measure
	 */
	public MeasureTask(
		Object measure,
		String identifier, boolean autoCompute,
		Function<Hierarchy, Boolean> applicabilityFunction, Function<Hierarchy, Object> computeFunction )
	{
		if ( measure == null ) {
			throw new IllegalArgumentException( "Measure must not be null!" );
		}
		if ( identifier == null || identifier.isEmpty() ) {
			throw new IllegalArgumentException( "Identifier is null or an empty string!" );
		}
		if ( computeFunction == null ) {
			throw new IllegalArgumentException( "Compute function must not be null!" );
		}

		this.measureObject = measure;
		this.identifier = identifier;
		this.autoCompute = autoCompute;
		this.applicabilityFunction = applicabilityFunction;
		this.computeFunction = computeFunction;
	}

	public boolean isQualityMeasure()
	{
		return measureObject instanceof QualityMeasure;
	}

	/**
	 * Calling this method when {@link #isQualityMeasure()} returns false will result in
	 * a {@link ClassCastException}
	 * 
	 * @return the desired value for this measure
	 */
	public double getDesiredValue()
	{
		return ( (QualityMeasure)measureObject ).getDesiredValue();
	}

	/**
	 * Calling this method when {@link #isQualityMeasure()} returns false will result in
	 * a {@link ClassCastException}
	 * 
	 * @return the undesired value for this measure
	 */
	public double getNotDesiredValue()
	{
		return ( (QualityMeasure)measureObject ).getNotDesiredValue();
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

	@Override
	public int compareTo( MeasureTask mt )
	{
		return identifier.compareTo( mt.identifier );
	}
}
