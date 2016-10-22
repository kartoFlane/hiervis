package pl.pwr.basic_hierarchy.interfaces;

public interface Hierarchy
{
	public Node getRoot();

	public Node[] getGroups();

	public int getNumberOfGroups();

	public String[] getClasses();

	public int getNumberOfClasses();

	public int getClassCount( String className, boolean withClassHierarchy );

	public int getNumberOfInstances();

	public void printTree();
}
