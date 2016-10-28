package pl.pwr.basic_hierarchy.implementation;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import pl.pwr.basic_hierarchy.common.Constants;
import pl.pwr.basic_hierarchy.common.StringIdComparator;
import pl.pwr.basic_hierarchy.interfaces.Group;
import pl.pwr.basic_hierarchy.interfaces.Hierarchy;


public class BasicHierarchy implements Hierarchy
{
	private Group root;
	private BasicGroup[] groups;
	private String[] classes;
	private int[] classCounts;
	private int instanceCount;


	public BasicHierarchy(
		Group root, List<? extends Group> groups,
		Map<String, Integer> eachClassWithCount
	)
	{
		if ( root == null ) {
			throw new IllegalArgumentException( "Root node must not be null." );
		}

		this.root = root;
		this.groups = groups.toArray( new BasicGroup[groups.size()] );

		for ( Group g : groups ) {
			this.instanceCount += g.getInstances().size();
		}

		classes = new String[eachClassWithCount.size()];
		classCounts = new int[eachClassWithCount.size()];

		LinkedList<String> sortedKeys = new LinkedList<String>( eachClassWithCount.keySet() );
		sortedKeys.sort( new StringIdComparator() );

		for ( int index = 0; index < sortedKeys.size(); ++index ) {
			String key = sortedKeys.get( index );
			classes[index] = key;
			classCounts[index] = eachClassWithCount.get( key );
		}
	}

	@Override
	public Group getRoot()
	{
		return root;
	}

	@Override
	public int getGroupCount()
	{
		return groups.length;
	}

	@Override
	public int getClassCount()
	{
		return classes.length;
	}

	@Override
	public Group[] getGroups()
	{
		return groups;
	}

	@Override
	public String[] getClasses()
	{
		return classes;
	}

	@Override
	public int getInstanceCount()
	{
		return instanceCount;
	}

	@Override
	public int getClassCount( String className, boolean withInstanceInheritance )
	{
		int index = Arrays.binarySearch( classes, className, new StringIdComparator() );

		if ( index < 0 ) {
			// Not found.
			return index;
		}
		else {
			// REFACTOR below code could be faster, by moving the computations into the constructor in a smart way
			// e.g. by using the partial results (from other classes) to compute results for other classes
			if ( withInstanceInheritance ) {
				String prefix = className + Constants.HIERARCHY_BRANCH_SEPARATOR;
				int result = classCounts[index];
				for ( int i = index; i < classCounts.length; ++i ) {
					if ( className.length() < classes[i].length() && classes[i].startsWith( prefix ) ) {
						result += classCounts[i];
					}
				}

				return result;
			}
			else {
				return classCounts[index];
			}
		}
	}

	@Override
	public String toString()
	{
		if ( root != null ) {
			return root.toString();
		}

		throw new RuntimeException( "Implementation error: this hierarchy has no root node." );
	}
}
