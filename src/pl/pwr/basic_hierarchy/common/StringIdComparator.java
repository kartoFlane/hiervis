package pl.pwr.basic_hierarchy.common;

import java.util.Comparator;


public class StringIdComparator implements Comparator<String>
{
	@Override
	public int compare( String o1, String o2 )
	{
		String[] o1Id = o1.split( Constants.HIERARCHY_BRANCH_SEPARATOR_REGEX );
		String[] o2Id = o2.split( Constants.HIERARCHY_BRANCH_SEPARATOR_REGEX );

		for ( int i = 1; i < Math.min( o1Id.length, o2Id.length ); i++ ) {
			if ( Integer.parseInt( o1Id[i] ) < Integer.parseInt( o2Id[i] ) ) {
				return -1;
			}
			else if ( Integer.parseInt( o1Id[i] ) > Integer.parseInt( o2Id[i] ) ) {
				return 1;
			}
		}
		// ids have equal part, so shorter id should be first
		if ( o1Id.length < o2Id.length ) {
			return -1;
		}
		else if ( o1Id.length > o2Id.length ) {
			return 1;
		}
		return 0;
	}
}
