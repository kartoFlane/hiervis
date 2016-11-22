package pl.pwr.hiervis.core;

import java.util.function.Function;

import basic_hierarchy.interfaces.Hierarchy;
import internal_measures.statistics.AvgPathLength;
import internal_measures.statistics.Height;
import internal_measures.statistics.NumberOfLeaves;
import internal_measures.statistics.NumberOfNodes;
import internal_measures.statistics.histogram.ChildPerNodePerLevel;


/**
 * A pair that associates a measure calculation function with a user-friendly unique identifier.
 * 
 * @author Tomasz Bachmiñski
 *
 */
public final class MeasureTask
{
	/** {@link AvgPathLength} */
	public static final MeasureTask averagePathLength = new MeasureTask( "Average Path Length", new AvgPathLength()::calculate );
	/** {@link Height} */
	public static final MeasureTask height = new MeasureTask( "Height", new Height()::calculate );
	/** {@link NumberOfLeaves} */
	public static final MeasureTask numberOfLeaves = new MeasureTask( "Number of Leaves", new NumberOfLeaves()::calculate );
	/** {@link NumberOfNodes} */
	public static final MeasureTask numberOfNodes = new MeasureTask( "Number of Nodes", new NumberOfNodes()::calculate );
	/** {@link ChildPerNodePerLevel} */
	public static final MeasureTask childrenPerNodePerLevel = new MeasureTask( "Children Per Node Per Level", new ChildPerNodePerLevel()::calculate );

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
