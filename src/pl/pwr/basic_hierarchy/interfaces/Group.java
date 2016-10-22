package pl.pwr.basic_hierarchy.interfaces;

import java.util.LinkedList;


public interface Group
{
	public String getId();

	public Group getParent();

	public String getParentId();

	public LinkedList<Group> getChildren();

	/**
	 * Returns instances which belong to this particular group
	 */
	public LinkedList<Instance> getInstances();

	/**
	 * Returns instances which belong to this group and its subgroups.
	 */
	public LinkedList<Instance> getSubgroupInstances();

	/**
	 * Returns a node centroid or medoid.
	 */
	public Instance getGroupRepresentation();
}
