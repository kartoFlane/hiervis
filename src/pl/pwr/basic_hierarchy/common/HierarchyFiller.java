package pl.pwr.basic_hierarchy.common;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import pl.pwr.basic_hierarchy.implementation.BasicGroup;
import pl.pwr.basic_hierarchy.interfaces.Group;
import pl.pwr.basic_hierarchy.interfaces.Instance;


/**
 * This class exposes methods which fix gaps in raw hierarchies.
 */
public class HierarchyFiller
{
	private HierarchyFiller()
	{
		// Static class -- disallow instantiation.
		throw new RuntimeException( "Attempted to instantiate a static class: " + getClass().getName() );
	}

	/**
	 * Builds a complete hierarchy of groups, while also patching up holes in the original hierarchy by inserting empty groups
	 * for missing IDs.
	 * 
	 * @param root
	 *            the root group
	 * @param groups
	 *            the original collection of groups
	 * @param fixBreadthGaps
	 *            whether the hierarchy fixing algorithm should also fix gaps in breadth, not just depth.
	 * @return the complete 'fixed' collection of groups, filled with artificial groups
	 */
	public static LinkedList<Group> buildCompleteGroupHierarchy( BasicGroup root, ArrayList<BasicGroup> groups, boolean fixBreadthGaps )
	{
		buildGroupHierarchy( groups );
		return addArtificialGroups( root, groups, fixBreadthGaps );
	}

	/**
	 * Builds a hierarchy of groups, based solely on the groups present in the specified collection.
	 * <p>
	 * If a group's ID implies it has a parent, but that parent is not present in the collection, then
	 * that group will not have its parent set.
	 * </p>
	 * <p>
	 * This means that this method DOES NOT GUARANTEE that the hierarchy it creates will be contiguous.
	 * To fix this, follow-up this method with {@link #addArtificialGroups(BasicGroup, ArrayList, boolean)}
	 * </p>
	 * 
	 * @param groups
	 *            collection of all groups to build the hierarchy from
	 */
	private static void buildGroupHierarchy( ArrayList<BasicGroup> groups )
	{
		for ( int i = 0; i < groups.size(); ++i ) {
			BasicGroup parentGroup = groups.get( i );
			String[] parentBranchIds = getGroupBranchIds( parentGroup );

			for ( int j = 0; j < groups.size(); ++j ) {
				if ( i == j ) { // Can't become a parent unto itself.
					continue;
				}

				BasicGroup childGroup = groups.get( j );
				String[] childBranchIds = getGroupBranchIds( childGroup );

				if ( areGroupsDirectlyRelated( parentBranchIds, childBranchIds ) ) {
					childGroup.setParent( parentGroup );
					parentGroup.addChild( childGroup );
				}
			}
		}
	}

	/**
	 * Add artificial groups which weren't present in the input file, but are needed in order to create a contiguous hierarchy.
	 * 
	 * @param root
	 *            the root group
	 * @param groups
	 *            the original collection of groups
	 * @param fixBreadthGaps
	 *            whether the hierarchy fixing algorithm should also fix gaps in breadth, not just depth.
	 * @return the complete 'fixed' collection of groups, filled with artificial groups
	 */
	private static LinkedList<Group> addArtificialGroups( BasicGroup root, ArrayList<BasicGroup> groups, boolean fixBreadthGaps )
	{
		LinkedList<BasicGroup> artificialGroups = new LinkedList<BasicGroup>();

		// Fix gaps in depth -- missing ancestors.
		for ( int i = 0; i < groups.size(); ++i ) {
			BasicGroup group = groups.get( i );

			if ( group == root ) {
				continue; // Don't consider the root group.
			}

			if ( group.getParent() == null ) {
				String[] groupBranchIds = getGroupBranchIds( group );

				BasicGroup nearestParent = null;
				int nearestParentHeight = -1;

				// Try to find nearest parent in 'real' groups
				BasicGroup tempGroup = findNearestParent( groups, groupBranchIds, -1, i );
				if ( tempGroup != null ) {
					nearestParent = tempGroup;
					nearestParentHeight = getGroupBranchIds( tempGroup ).length;
				}

				// Try to find nearest parent in artificial groups
				tempGroup = findNearestParent( groups, groupBranchIds, nearestParentHeight );
				if ( tempGroup != null ) {
					nearestParent = tempGroup;
					nearestParentHeight = getGroupBranchIds( tempGroup ).length;
				}

				if ( nearestParent != null ) {
					BasicGroup newParent = nearestParent;

					for ( int j = nearestParentHeight; j < groupBranchIds.length - 1; ++j ) {
						String newGroupIdPostfix = groupBranchIds[j];
						String newGroupId = newParent.getId().concat( Constants.HIERARCHY_BRANCH_SEPARATOR ).concat( newGroupIdPostfix );

						// Add empty group
						BasicGroup newGroup = new BasicGroup(
							newGroupId, newParent,
							new LinkedList<Group>(), new LinkedList<Instance>()
						);

						// Create proper parent relation
						newParent.addChild( newGroup );
						newGroup.setParent( newParent );

						artificialGroups.add( newGroup );

						newParent = newGroup;
					}

					// Add missing links
					newParent.addChild( group );
					group.setParent( newParent );
				}
				else {
					throw new RuntimeException(
						String.format(
							"Could not find nearest parent for '%s'. This means that something went seriously wrong.",
							group.getId()
						)
					);
				}
			}
		}

		// Fix gaps in breadth -- missing siblings.
		if ( fixBreadthGaps ) {
			// TODO: This seems to be a somewhat roundabout way of filling the gaps. Rework this?

			LinkedList<BasicGroup> groupsToCheck = new LinkedList<BasicGroup>();
			groupsToCheck.add( root );

			while ( !groupsToCheck.isEmpty() ) {
				BasicGroup currentGroup = groupsToCheck.removeFirst();
				String currentGroupId = currentGroup.getId();

				int lengthOfCurrentId = currentGroupId.length();
				int maxId = Integer.MIN_VALUE;

				// Collect existing IDs
				HashSet<Integer> existingIds = new HashSet<Integer>();
				for ( Group childGroup : currentGroup.getChildren() ) {
					if ( areGroupsRelated( currentGroup, childGroup ) ) {
						int childNumber = Integer.parseInt( childGroup.getId().substring( lengthOfCurrentId + 1 ) );
						maxId = Math.max( maxId, childNumber );
						existingIds.add( childNumber );
						groupsToCheck.add( (BasicGroup)childGroup );
					}
					else {
						throw new RuntimeException(
							String.format(
								"Fatal error while filling breadth gaps! '%s' IS NOT an ancestor of '%s', " +
									"but '%s' IS a child of '%s'!",
								currentGroupId, childGroup.getId(), childGroup.getId(), currentGroupId
							)
						);
					}
				}

				// Fill gaps between IDs
				for ( int i = 0; i <= maxId; i++ ) {
					if ( !existingIds.contains( i ) ) {
						BasicGroup newGroup = new BasicGroup(
							currentGroup.getId().concat( Constants.HIERARCHY_BRANCH_SEPARATOR + i ),
							currentGroup, new LinkedList<Group>(), new LinkedList<Instance>()
						);

						currentGroup.addChild( newGroup );
						newGroup.setParent( currentGroup );

						artificialGroups.add( newGroup );
					}
				}

				currentGroup.getChildren().sort( new NodeComparator() );
			}
		}

		LinkedList<Group> allGroups = new LinkedList<Group>( artificialGroups );
		allGroups.addAll( groups );

		return allGroups;
	}

	private static BasicGroup findNearestParent( List<BasicGroup> groups, String[] childBranchIds, int nearestHeight )
	{
		return findNearestParent( groups, childBranchIds, nearestHeight, -1 );
	}

	private static BasicGroup findNearestParent( List<BasicGroup> groups, String[] childBranchIds, int nearestHeight, int maxIndex )
	{
		if ( maxIndex < 0 ) {
			maxIndex = groups.size();
		}

		BasicGroup result = null;

		for ( int i = 0; i < maxIndex; ++i ) {
			BasicGroup parentGroup = groups.get( i );
			String[] parentBranchIds = getGroupBranchIds( parentGroup );

			if ( parentBranchIds.length > nearestHeight ) {
				if ( areGroupsRelated( parentBranchIds, childBranchIds ) ) {
					result = parentGroup;
					nearestHeight = parentBranchIds.length;
				}
			}
		}

		return result;
	}

	/**
	 * Convenience method to split a group's IDs into segments for easier processing.
	 */
	private static String[] getGroupBranchIds( Group g )
	{
		return g.getId().split( Constants.HIERARCHY_BRANCH_SEPARATOR_REGEX );
	}

	private static boolean areGroupsDirectlyRelated( Group parent, Group child )
	{
		return areGroupsDirectlyRelated( getGroupBranchIds( parent ), getGroupBranchIds( child ) );
	}

	/**
	 * Checks whether the two groups are directly related (parent-child).
	 * 
	 * @param parentIds
	 *            ID segments of the group acting as parent
	 * @param childIds
	 *            ID segments of the group as child
	 * @return whether the two groups are in fact in a direct parent-child relationship.
	 */
	private static boolean areGroupsDirectlyRelated( String[] parentIds, String[] childIds )
	{
		// Check that the child is exactly one level 'deeper' than the parent.

		if ( parentIds.length + 1 == childIds.length ) {
			// Compare the group IDs to verify that they are related.
			return areGroupsRelated( parentIds, childIds );
		}
		else {
			return false;
		}
	}

	private static boolean areGroupsRelated( Group parent, Group child )
	{
		return areGroupsRelated( getGroupBranchIds( parent ), getGroupBranchIds( child ) );
	}

	/**
	 * Checks whether the two groups are indirectly related (ancestor-descendant).
	 * 
	 * @param parentIds
	 *            ID segments of the group acting as parent
	 * @param childIds
	 *            ID segments of the group as child
	 * @return whether the two groups are in fact in a ancestor-descendant relationship.
	 */
	private static boolean areGroupsRelated( String[] parentIds, String[] childIds )
	{
		if ( parentIds.length <= childIds.length ) {
			boolean result = true;
			for ( int i = 0; i < parentIds.length && result; ++i ) {
				if ( !parentIds[i].equals( childIds[i] ) ) {
					result = false;
				}
			}

			return result;
		}
		else {
			return false;
		}
	}
}
