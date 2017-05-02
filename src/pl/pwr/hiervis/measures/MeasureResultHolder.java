package pl.pwr.hiervis.measures;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.apache.commons.lang3.tuple.Pair;

import basic_hierarchy.interfaces.Hierarchy;
import basic_hierarchy.interfaces.Node;


/**
 * Holder class for results of measures that have been computed for
 * either a {@link Hierarchy} object, or its constituent {@link Node}s
 * 
 * @author Tomasz Bachmiński
 *
 */
public class MeasureResultHolder
{
	private final Map<String, Object> computedMeasureMap;
	private final Map<Pair<Node, String>, Object> computedNodeMeasureMap;


	public MeasureResultHolder()
	{
		computedMeasureMap = new HashMap<>();
		computedNodeMeasureMap = new HashMap<>();
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
	public Set<Map.Entry<String, Object>> getComputedMeasures()
	{
		synchronized ( computedMeasureMap ) {
			return Collections.unmodifiableMap( computedMeasureMap ).entrySet();
		}
	}

	/**
	 * @param identifier
	 *            identifier of the task to look for
	 * @return true if the task is already computed, false otherwise
	 */
	public boolean isMeasureComputed( String identifier )
	{
		synchronized ( computedMeasureMap ) {
			return computedMeasureMap.containsKey( identifier );
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
	public void forComputedMeasures( Consumer<Set<Map.Entry<String, Object>>> function )
	{
		synchronized ( computedMeasureMap ) {
			function.accept( Collections.unmodifiableMap( computedMeasureMap ).entrySet() );
		}
	}

	public Object getMeasureResultOrDefault( String identifier, Object defaultValue )
	{
		synchronized ( computedMeasureMap ) {
			return computedMeasureMap.getOrDefault( identifier, defaultValue );
		}
	}

	public Object getMeasureResult( String identifier )
	{
		synchronized ( computedMeasureMap ) {
			return computedMeasureMap.get( identifier );
		}
	}

	protected void putMeasureResult( String identifier, Object value )
	{
		synchronized ( computedMeasureMap ) {
			computedMeasureMap.put( identifier, value );
		}
	}

	// -------------------------------------------------------------------------
	// Node measures

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
	public Set<Map.Entry<Pair<Node, String>, Object>> getNodeComputedMeasures()
	{
		synchronized ( computedNodeMeasureMap ) {
			return Collections.unmodifiableMap( computedNodeMeasureMap ).entrySet();
		}
	}

	/**
	 * @param node
	 *            the node we want to look up the measure result for
	 * @param identifier
	 *            identifier of the task to look for
	 * @return true if the task is already computed, false otherwise
	 */
	public boolean isNodeMeasureComputed( Node node, String identifier )
	{
		return isNodeMeasureComputed( Pair.of( node, identifier ) );
	}

	public boolean isNodeMeasureComputed( Pair<Node, String> pair )
	{
		synchronized ( computedNodeMeasureMap ) {
			return computedNodeMeasureMap.containsKey( pair );
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
	public void forNodeComputedMeasures( Consumer<Set<Map.Entry<Pair<Node, String>, Object>>> function )
	{
		synchronized ( computedNodeMeasureMap ) {
			function.accept( Collections.unmodifiableMap( computedNodeMeasureMap ).entrySet() );
		}
	}

	public Object getNodeMeasureResultOrDefault( Node node, String identifier, Object defaultValue )
	{
		return getNodeMeasureResultOrDefault( Pair.of( node, identifier ), defaultValue );
	}

	public Object getNodeMeasureResultOrDefault( Pair<Node, String> pair, Object defaultValue )
	{
		synchronized ( computedNodeMeasureMap ) {
			return computedNodeMeasureMap.getOrDefault( pair, defaultValue );
		}
	}

	public Object getNodeMeasureResult( Node node, String identifier )
	{
		return getNodeMeasureResult( Pair.of( node, identifier ) );
	}

	public Object getNodeMeasureResult( Pair<Node, String> pair )
	{
		synchronized ( computedNodeMeasureMap ) {
			return computedNodeMeasureMap.get( pair );
		}
	}

	protected void putNodeMeasureResult( Node node, String identifier, Object value )
	{
		putNodeMeasureResult( Pair.of( node, identifier ), value );
	}

	protected void putNodeMeasureResult( Pair<Node, String> pair, Object value )
	{
		synchronized ( computedNodeMeasureMap ) {
			computedNodeMeasureMap.put( pair, value );
		}
	}

	/**
	 * Clear all results stored in this holder.
	 */
	public void clear()
	{
		computedMeasureMap.clear();
		computedNodeMeasureMap.clear();
	}
}
