package pl.pwr.basic_hierarchy.interfaces;

public interface Hierarchy
{
	/**
	 * Returns the root node of this hierarchy.
	 */
	public Group getRoot();

	/**
	 * Returns an array of groups of instances.
	 */
	public Group[] getGroups();

	/**
	 * Returns the number of groups in this hierarchy. Equivalent to {@code getGroups().length}.
	 */
	public int getGroupCount();

	/**
	 * Returns an array of ground-truth classes present in this hierarchy.
	 */
	public String[] getClasses();

	/**
	 * Returns the number of ground-truth classes. Equivalent to {@code getClasses().length}.
	 */
	public int getClassCount();

	/**
	 * Returns the total number of instances in this hierarchy, ie. sum of instances in all groups.
	 */
	public int getInstanceCount();

	/**
	 * Returns the number of groups within the specified class.
	 * 
	 * @param className
	 *            Class name to look for.
	 * @param withInstanceInheritance
	 *            if true, the result will also include child classes of the specified class. (?)
	 */
	public int getClassCount( String className, boolean withInstanceInheritance );
}
