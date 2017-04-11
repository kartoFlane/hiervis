package pl.pwr.hiervis.core;

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


	public LoadedHierarchy( Hierarchy h, LoadedHierarchy.Options o )
	{
		if ( h == null )
			throw new IllegalArgumentException( "Hierarchy must not be null!" );
		if ( o == null )
			throw new IllegalArgumentException( "Options must not be null!" );

		this.data = h;
		this.options = o;
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
