package pl.pwr.hiervis.measures;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.Pair;

import basic_hierarchy.interfaces.Hierarchy;
import pl.pwr.hiervis.hierarchy.LoadedHierarchy;


/**
 * Holder class for results of measures that have been computed for a {@link LoadedHierarchy}.
 * 
 * @author Tomasz Bachmiński
 *
 */
public class MeasureResultHolder
{
	private final Map<Pair<Hierarchy, MeasureTask>, Object> computedMeasureMap;


	public MeasureResultHolder()
	{
		computedMeasureMap = new HashMap<>();
	}

	// -------------------------------------------------------------------------
	// Hierarchy measures

	/**
	 * Returns a set of measures that have been computed thus far for the currently loaded hierarchy.
	 * <p>
	 * This method is not particularly thread-safe, as the map of measures might be updated with new entries
	 * while you are processing the set, resulting in missed entries.
	 * </p>
	 * <p>
	 * For a thread-safe alternative, see {@link #forComputedMeasures(Consumer)}
	 * </p>
	 * 
	 * @see #forComputedMeasures(Consumer)
	 */
	public Set<Map.Entry<Pair<Hierarchy, MeasureTask>, Object>> getComputedMeasures()
	{
		synchronized ( computedMeasureMap ) {
			return Collections.unmodifiableMap( computedMeasureMap ).entrySet();
		}
	}

	/**
	 * @param hierarchy
	 *            the hierarchy we want to look up the measure result for
	 * @param measure
	 *            task to look for
	 * @return true if the task is already computed, false otherwise
	 */
	public boolean isMeasureComputed( Hierarchy hierarchy, MeasureTask measure )
	{
		return isNodeMeasureComputed( Pair.of( hierarchy, measure ) );
	}

	public boolean isNodeMeasureComputed( Pair<Hierarchy, MeasureTask> pair )
	{
		synchronized ( computedMeasureMap ) {
			return computedMeasureMap.containsKey( pair );
		}
	}

	/**
	 * Performs the specified function on the set of measures that have been computed thus far for
	 * the currently loaded hierarchy.
	 * <p>
	 * This method executes the function inside of a synchronized block, preventing the set from
	 * being updated while this method is executing.
	 * </p>
	 */
	public void forComputedMeasures( Consumer<Set<Map.Entry<Pair<Hierarchy, MeasureTask>, Object>>> function )
	{
		synchronized ( computedMeasureMap ) {
			function.accept( Collections.unmodifiableMap( computedMeasureMap ).entrySet() );
		}
	}

	public Object getMeasureResultOrDefault( Hierarchy hierarchy, MeasureTask measure, Object defaultValue )
	{
		return getMeasureResultOrDefault( Pair.of( hierarchy, measure ), defaultValue );
	}

	public Object getMeasureResultOrDefault( Pair<Hierarchy, MeasureTask> pair, Object defaultValue )
	{
		synchronized ( computedMeasureMap ) {
			return computedMeasureMap.getOrDefault( pair, defaultValue );
		}
	}

	public Object getMeasureResult( Hierarchy hierarchy, MeasureTask measure )
	{
		return getMeasureResult( Pair.of( hierarchy, measure ) );
	}

	public Object getMeasureResult( Pair<Hierarchy, MeasureTask> pair )
	{
		synchronized ( computedMeasureMap ) {
			return computedMeasureMap.get( pair );
		}
	}

	protected void putMeasureResult( Hierarchy hierarchy, MeasureTask measure, Object value )
	{
		putMeasureResult( Pair.of( hierarchy, measure ), value );
	}

	protected void putMeasureResult( Pair<Hierarchy, MeasureTask> pair, Object value )
	{
		synchronized ( computedMeasureMap ) {
			computedMeasureMap.put( pair, value );
		}
	}

	/**
	 * Clear all results stored in this holder.
	 */
	public void clear()
	{
		computedMeasureMap.clear();
	}
}
