package pl.pwr.hiervis.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import basic_hierarchy.common.HierarchyBuilder;
import basic_hierarchy.interfaces.Hierarchy;


/**
 * Container class that associates a {@link Hierarchy} with {@link HierarchyLoadOptions} that was
 * used to load it.
 * 
 * @author Tomasz Bachmiński
 *
 */
public class LoadedHierarchy
{
	public final Hierarchy data;
	public final LoadedHierarchy.Options options;

	private final Map<String, Object> computedMeasureMap;


	public LoadedHierarchy( Hierarchy h, LoadedHierarchy.Options o )
	{
		if ( h == null )
			throw new IllegalArgumentException( "Hierarchy must not be null!" );
		if ( o == null )
			throw new IllegalArgumentException( "Options must not be null!" );

		this.data = h;
		this.options = o;

		this.computedMeasureMap = new HashMap<>();
	}

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


	/**
	 * Container class for load options for each {@link Hierarchy} object loaded in the program.
	 * 
	 * @author Tomasz Bachmiński
	 *
	 */
	public static class Options
	{
		public static final Options DEFAULT = new Options( false, false, false, false, false );

		public final boolean hasTnstanceNameAttribute;
		public final boolean hasTrueClassAttribute;
		public final boolean hasColumnHeader;
		public final boolean isFillBreadthGaps;
		public final boolean isUseSubtree;


		/**
		 * @param withInstanceNameAttribute
		 *            if true, the reader will assume that the file includes a column containing instance names
		 * @param withTrueClassAttribute
		 *            if true, the reader will assume that the file includes a column containing true class
		 * @param withHeader
		 *            if true, the reader will assume that the first row contains column headers, specifying the name for each column
		 * @param fillBreadthGaps
		 *            if true, the {@link HierarchyBuilder} will attempt to fix the raw hierarchy built from the file.
		 * @param useSubtree
		 *            whether the centroid calculation should also include child groups' instances.
		 */
		public Options(
			boolean withInstanceNameAttribute,
			boolean withTrueClassAttribute,
			boolean withHeader,
			boolean fillBreadthGaps,
			boolean useSubtree )
		{
			this.hasTnstanceNameAttribute = withInstanceNameAttribute;
			this.hasTrueClassAttribute = withTrueClassAttribute;
			this.hasColumnHeader = withHeader;
			this.isFillBreadthGaps = fillBreadthGaps;
			this.isUseSubtree = useSubtree;
		}
	}
}
