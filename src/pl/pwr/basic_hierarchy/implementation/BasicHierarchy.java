package pl.pwr.basic_hierarchy.implementation;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;

import pl.pwr.basic_hierarchy.common.Constants;
import pl.pwr.basic_hierarchy.common.StringIdComparator;
import pl.pwr.basic_hierarchy.interfaces.Hierarchy;
import pl.pwr.basic_hierarchy.interfaces.Node;


public class BasicHierarchy implements Hierarchy
{
	private Node root;
	private BasicNode[] groups;
	private String[] classes;
	private int[] classesCount;
	private int numberOfInstances;


	public BasicHierarchy(
		Node root, LinkedList<Node> groups,
		HashMap<String, Integer> eachClassWithCount,
		int numberOfInstances
	)
	{
		this.root = root;
		this.groups = groups.toArray( new BasicNode[groups.size()] );
		this.numberOfInstances = numberOfInstances;

		classes = new String[eachClassWithCount.size()];
		classesCount = new int[eachClassWithCount.size()];
		LinkedList<String> sortedKeyes = new LinkedList<String>( eachClassWithCount.keySet() );
		Collections.sort( sortedKeyes, new StringIdComparator() );
		int arrayIndex = 0;
		for ( String key : sortedKeyes ) {
			classes[arrayIndex] = key;
			classesCount[arrayIndex] = eachClassWithCount.get( key );
			arrayIndex++;
		}
	}

	@Override
	public Node getRoot()
	{
		return root;
	}

	@Override
	public int getNumberOfGroups()
	{
		return groups.length;
	}

	@Override
	public int getNumberOfClasses()
	{
		return classes.length;
	}

	@Override
	public Node[] getGroups()
	{
		return groups;
	}

	@Override
	public String[] getClasses()
	{
		return classes;
	}

	@Override
	public int getClassCount( String className, boolean withInstancesInheritance )
	{
		int index = Arrays.binarySearch( classes, className, new StringIdComparator() );
		if ( index < 0 )
			return index;
		else {
			// REFACTOR below code could be faster, by moving the computations into the constructor in a smart way
			// e.g. by using the partial results (from other classes) to compute results for other classes
			if ( withInstancesInheritance ) {
				int returnValue = classesCount[index];
				for ( int i = index; i < classesCount.length; i++ ) {
					if ( className.length() < classes[i].length() && classes[i].startsWith( className + Constants.HIERARCHY_BRANCH_SEPARATOR ) ) {
						returnValue += classesCount[i];
					}
				}
				return returnValue;
			}
			else
				return classesCount[index];
		}
	}

	@Override
	public int getNumberOfInstances()
	{
		return numberOfInstances;
	}

	@Override
	public void printTree()
	{
		if ( root != null )
			root.printSubtree();
	}
}
