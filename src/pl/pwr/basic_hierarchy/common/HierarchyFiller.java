package pl.pwr.basic_hierarchy.common;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import pl.pwr.basic_hierarchy.implementation.BasicGroup;
import pl.pwr.basic_hierarchy.interfaces.Group;


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
	public static List<? extends Group> buildCompleteGroupHierarchy( BasicGroup root, List<BasicGroup> groups, boolean fixBreadthGaps )
	{
		buildGroupHierarchy( groups );
		groups = fixDepthGaps( root, groups );

		if ( fixBreadthGaps ) {
			groups = fixBreadthGaps( root, groups );
		}

		return groups;
	}

	/**
	 * Builds a hierarchy of groups, based solely on the groups present in the specified collection.
	 * <p>
	 * If a group's ID implies it has a parent, but that parent is not present in the collection, then
	 * that group will not have its parent set.
	 * </p>
	 * <p>
	 * This means that this method DOES NOT GUARANTEE that the hierarchy it creates will be contiguous.
	 * To fix this, follow-up this method with {@link #fixDepthGaps(BasicGroup, List)} and/or
	 * {@link #fixBreadthGaps(BasicGroup, List)}
	 * </p>
	 * 
	 * @param groups
	 *            collection of all groups to build the hierarchy from
	 */
	private static void buildGroupHierarchy( List<BasicGroup> groups )
	{
		for ( int i = 0; i < groups.size(); ++i ) {
			BasicGroup parentGroup = groups.get( i );
			String[] parentBranchIds = getGroupBranchIds( parentGroup );

			for ( int j = 0; j < groups.size(); ++j ) {
				if ( i == j ) {
					// Can't become a parent unto itself.
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
	 * Fixed gaps in depth (missing ancestors) by creating empty groups where needed.
	 * <p>
	 * Such gaps appear when the source file did not list these groups (since they were empty),
	 * but their existence can be inferred from IDs of existing groups.
	 * </p>
	 * 
	 * @param root
	 *            the root group
	 * @param groups
	 *            the original collection of groups
	 * @return the complete 'fixed' collection of groups, filled with artificial groups
	 */
	private static List<BasicGroup> fixDepthGaps( BasicGroup root, List<BasicGroup> groups )
	{
		List<BasicGroup> artificialGroups = new ArrayList<BasicGroup>();

		StringBuilder buf = new StringBuilder();

		for ( int i = 0; i < groups.size(); ++i ) {
			BasicGroup group = groups.get( i );

			if ( group == root ) {
				// Don't consider the root group.
				continue;
			}

			if ( group.getParent() == null ) {
				String[] groupBranchIds = getGroupBranchIds( group );

				BasicGroup nearestParent = null;
				int nearestParentHeight = -1;

				// Try to find nearest parent in 'real' groups
				BasicGroup candidateGroup = findNearestAncestor( groups, groupBranchIds, -1, i );
				if ( candidateGroup != null ) {
					nearestParent = candidateGroup;
					nearestParentHeight = getGroupBranchIds( candidateGroup ).length;
				}

				// Try to find nearest parent in artificial groups
				candidateGroup = findNearestAncestor( groups, groupBranchIds, nearestParentHeight );
				if ( candidateGroup != null ) {
					nearestParent = candidateGroup;
					nearestParentHeight = getGroupBranchIds( candidateGroup ).length;
				}

				if ( nearestParent != null ) {
					BasicGroup newParent = nearestParent;

					for ( int j = nearestParentHeight; j < groupBranchIds.length - 1; ++j ) {
						buf.setLength( 0 );

						String newGroupId = buf
							.append( newParent.getId() )
							.append( Constants.HIERARCHY_BRANCH_SEPARATOR )
							.append( groupBranchIds[j] )
							.toString();

						// Add an empty group
						BasicGroup newGroup = new BasicGroup(
							newGroupId, newParent
						);

						// Create proper parent-child relations
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

		List<BasicGroup> allGroups = new ArrayList<BasicGroup>( artificialGroups );
		allGroups.addAll( groups );

		return allGroups;
	}

	/**
	 * Fixed gaps in breadth (missing siblings) by creating empty groups where needed.
	 * <p>
	 * Such gaps appear when the source file did not list these groups (since they were empty),
	 * but their existence can be inferred from IDs of existing groups.
	 * </p>
	 * 
	 * @param root
	 *            the root group
	 * @param groups
	 *            the original collection of groups
	 * @return the complete 'fixed' collection of groups, filled with artificial groups
	 */
	private static List<BasicGroup> fixBreadthGaps( BasicGroup root, List<BasicGroup> groups )
	{
		List<BasicGroup> artificialGroups = new ArrayList<BasicGroup>();

		Comparator<Group> groupComparator = new GroupComparator();
		StringBuilder buf = new StringBuilder();

		Queue<BasicGroup> pendingGroups = new LinkedList<BasicGroup>();
		pendingGroups.add( root );

		while ( !pendingGroups.isEmpty() ) {
			BasicGroup currentGroup = pendingGroups.remove();

			List<Group> children = currentGroup.getChildren();
			List<Group> newChildren = new LinkedList<Group>();

			for ( int i = 0; i <= children.size(); ++i ) {
				Group childGroup = children.get( i );

				if ( childGroup == null ) {
					// If i-th child doesn't exist, then there's a gap. Fix it.
					buf.setLength( 0 );
					buf.append( currentGroup.getId() ).append( Constants.HIERARCHY_BRANCH_SEPARATOR ).append( i );

					BasicGroup newGroup = new BasicGroup( buf.toString(), currentGroup );
					newGroup.setParent( currentGroup );

					newChildren.add( newGroup );
					artificialGroups.add( newGroup );
				}
				else {
					if ( areGroupsRelated( currentGroup, childGroup ) ) {
						pendingGroups.add( (BasicGroup)childGroup );
					}
					else {
						throw new RuntimeException(
							String.format(
								"Fatal error while filling breadth gaps! '%s' IS NOT an ancestor of '%s', " +
									"but '%s' IS a child of '%s'!",
								currentGroup.getId(), childGroup.getId(), childGroup.getId(), currentGroup.getId()
							)
						);
					}
				}
			}

			for ( Group g : newChildren ) {
				currentGroup.addChild( g );
			}
			children.sort( groupComparator );
		}

		List<BasicGroup> allGroups = new ArrayList<BasicGroup>( artificialGroups );
		allGroups.addAll( groups );

		return allGroups;
	}

	/**
	 * {@link #findNearestAncestor(List, String[], int, int)}
	 */
	private static BasicGroup findNearestAncestor( List<BasicGroup> groups, String[] childBranchIds, int nearestHeight )
	{
		return findNearestAncestor( groups, childBranchIds, nearestHeight, -1 );
	}

	/**
	 * Attempts to find the nearest existing group that can act as an ancestor to the group specified in argument. IF no such
	 * group could be found, this method returns null.
	 * <p>
	 * This method relies on the groups' IDs being correctly formatted and allowing us to infer the parent-child relations.
	 * </p>
	 * 
	 * @param groups
	 *            list of groups to search in
	 * @param childBranchIds
	 *            ID segments of the group for which we're trying to find an ancestor
	 * @param nearestHeight
	 *            number of segments of the best candidate we have available currently (can be negative to mean 'none')
	 * @param maxIndex
	 *            max index to search to in the list of groups, for bounding purposes (can be negative to perform an unbounded search)
	 * @return the nearest group that can act as an ancestor, or null if not found
	 */
	private static BasicGroup findNearestAncestor( List<BasicGroup> groups, String[] childBranchIds, int nearestHeight, int maxIndex )
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

	/**
	 * {@link #areGroupsDirectlyRelated(String[], String[])}
	 */
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

	/**
	 * {@link #areGroupsRelated(String[], String[])}
	 */
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
			for ( int i = 0; i < parentIds.length; ++i ) {
				if ( !parentIds[i].equals( childIds[i] ) ) {
					return false;
				}
			}

			return true;
		}
		else {
			return false;
		}
	}
}
