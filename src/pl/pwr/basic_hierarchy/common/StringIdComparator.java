package pl.pwr.basic_hierarchy.common;

import java.util.Comparator;


/**
 * Compares two strings representing node ids.
 * <p>
 * Ids must begin with '{@code gen}', followed by any number of groups of digits.
 * Each segment is separated by a dot ('.').<br/>
 * For example: '{@code gen.0.15.1}'
 * </p>
 */
public class StringIdComparator implements Comparator<String>
{
	@Override
	public int compare( String o1, String o2 )
	{
		String[] id1 = o1.split( Constants.HIERARCHY_BRANCH_SEPARATOR_REGEX );
		String[] id2 = o2.split( Constants.HIERARCHY_BRANCH_SEPARATOR_REGEX );

		// Ignore the first segment - 'gen'
		for ( int i = 1; i < Math.min( id1.length, id2.length ); ++i ) {
			int n1 = Integer.parseInt( id1[i] );
			int n2 = Integer.parseInt( id2[i] );

			if ( n1 != n2 ) {
				// Id with smaller generation index is 'smaller'.
				return n1 - n2;
			}
			else {
				// Both numbers are equal -- proceed to the next segment.
			}
		}

		// Both ids have equal segments, so they're effectively equal.
		// In this case, consider the shorter id as 'smaller'.
		return id1.length - id2.length;
	}
}
