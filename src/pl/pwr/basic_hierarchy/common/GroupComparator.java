package pl.pwr.basic_hierarchy.common;

import java.util.Comparator;

import pl.pwr.basic_hierarchy.interfaces.Group;


/**
 * Compares two groups.
 * <p>
 * Implementation compares the two groups' IDs using {@linkplain StringIdComparator}.
 * </p>
 */
public class GroupComparator implements Comparator<Group>
{
	private StringIdComparator idComparator = new StringIdComparator();


	/**
	 * Note: this comparator imposes orderings that are <b>inconsistent with {@code equals}</b>.
	 * 
	 * @see Comparator#compare(Object, Object)
	 */
	@Override
	public int compare( Group o1, Group o2 )
	{
		String o1Id = o1.getId();
		String o2Id = o2.getId();

		return idComparator.compare( o1Id, o2Id );
	}
}
