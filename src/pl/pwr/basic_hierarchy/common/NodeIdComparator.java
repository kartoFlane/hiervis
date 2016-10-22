package pl.pwr.basic_hierarchy.common;

import java.util.Comparator;

import pl.pwr.basic_hierarchy.interfaces.Node;


public class NodeIdComparator implements Comparator<Node>
{
	/*
	 * Note: this comparator imposes orderings that are inconsistent with equals.
	 * 
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare( Node o1, Node o2 )
	{
		String o1Id = o1.getId();
		String o2Id = o2.getId();

		return new StringIdComparator().compare( o1Id, o2Id );
	}
}
