package pl.pwr.basic_hierarchy.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;

import pl.pwr.basic_hierarchy.implementation.BasicGroup;
import pl.pwr.basic_hierarchy.interfaces.Instance;
import pl.pwr.basic_hierarchy.interfaces.Group;


public class HierarchyFiller
{
	@SuppressWarnings("unchecked")
	public static LinkedList<Group> addMissingEmptyNodes(
		BasicGroup root,
		ArrayList<BasicGroup> nodes,
		int rootIndexInNodes,
		boolean fillBreadthGaps
	)
	{
		LinkedList<Group> allNodes;
		// hierarchy build

		// search for parent
		boolean[] nodeHasParent = new boolean[nodes.size()];
		for ( int i = 0; i < nodes.size(); i++ ) {
			BasicGroup parentNode = nodes.get( i );
			String[] parentBranchId = parentNode.getId().split( Constants.HIERARCHY_BRANCH_SEPARATOR_REGEX );
			int parentHeightNum = parentBranchId.length;
			// REFACTOR if we have a map of nodes then below search could be quicker
			for ( int j = 0/* i+1 */; j < nodes.size(); j++ )// assume that nodes are in DFS order in input file
			{
				BasicGroup elem = nodes.get( j );
				String[] elemBranchId = elem.getId().split( Constants.HIERARCHY_BRANCH_SEPARATOR_REGEX );
				int elemHeightNum = elemBranchId.length;
				if ( elemHeightNum == parentHeightNum + 1 )// if elem is a DIRECT child to parent
				{
					boolean equalBranchIds = true;
					for ( int k = 0; k < parentHeightNum && equalBranchIds; k++ ) {
						if ( !parentBranchId[k].equals( elemBranchId[k] ) ) {
							equalBranchIds = false;
						}
					}

					if ( equalBranchIds )// there is DIRECT link parent->child
					{
						nodeHasParent[j] = true;
						elem.setParent( parentNode );
						parentNode.addChild( elem );
					}
				}
			}
		}

		// add additional nodes which weren't in input file, but are needed
		// first consider HEIGHT gaps between nodes
		LinkedList<BasicGroup> additionalNodes = new LinkedList<BasicGroup>();
		if ( rootIndexInNodes >= 0 ) {
			nodeHasParent[0] = nodes.get( rootIndexInNodes ).equals( root );// skip root from consideration
		}
		for ( int i = 0; i < nodeHasParent.length; i++ ) {
			if ( !nodeHasParent[i] )// no direct parent
			{
				BasicGroup elem = nodes.get( i );
				String[] elemBranchId = elem.getId().split( Constants.HIERARCHY_BRANCH_SEPARATOR_REGEX );
				int elemHeightNum = elemBranchId.length;
				// REFACTOR below defined two loops have duplicated code
				// find nearest parent
				BasicGroup nearestParent = null;
				int nearestParentHeightNum = Integer.MIN_VALUE;
				// REFACTOR below defined loop could be quicker when there will be a flag of nodes
				for ( int j = 0; j < i; j++ ) {
					BasicGroup parentNode = nodes.get( j );
					String[] parentBranchId = parentNode.getId().split( Constants.HIERARCHY_BRANCH_SEPARATOR_REGEX );
					int parentHeightNum = parentBranchId.length;

					if ( parentHeightNum > nearestParentHeightNum && elemHeightNum > parentHeightNum ) {
						boolean equalBranchIds = true;
						for ( int k = 0; k < parentHeightNum && equalBranchIds; k++ )// we need to search until the end and choose the longest prefix, not
						{// the first that was found
							if ( !parentBranchId[k].equals( elemBranchId[k] ) ) {
								equalBranchIds = false;
							}
						}

						if ( equalBranchIds ) {
							nearestParent = parentNode;
							nearestParentHeightNum = parentHeightNum;
						}
					}
				}
				// maybe, we recently added useful node
				// REFACTOR below loop is almost the same as the upper one
				for ( int j = 0; j < additionalNodes.size(); j++ ) {
					BasicGroup potentialParentNode = additionalNodes.get( j );
					String[] potentialParentBranchId = potentialParentNode.getId().split( Constants.HIERARCHY_BRANCH_SEPARATOR_REGEX );
					int potentialParentHeightNum = potentialParentBranchId.length;

					if ( potentialParentHeightNum > nearestParentHeightNum && elemHeightNum > potentialParentHeightNum ) {
						boolean equalBranchIds = true;
						for ( int k = 0; k < potentialParentHeightNum && equalBranchIds; k++ )// look for the longest prefix
						{
							if ( !potentialParentBranchId[k].equals( elemBranchId[k] ) ) {
								equalBranchIds = false;
							}
						}

						if ( equalBranchIds ) {
							nearestParent = potentialParentNode;
							nearestParentHeightNum = potentialParentHeightNum;
						}
					}
				}

				if ( nearestParent != null )// nearest parent found
				{
					BasicGroup newParent = nearestParent;
					for ( int k = nearestParentHeightNum; k < elemHeightNum - 1; k++ ) {
						String nodeToAddIdPostfix = elemBranchId[k];
						String nodeToAddId = newParent.getId().concat( Constants.HIERARCHY_BRANCH_SEPARATOR ).concat( nodeToAddIdPostfix );

						// add empty node
						BasicGroup nodeToAdd = new BasicGroup(
							nodeToAddId, newParent,
							new LinkedList<Group>(), new LinkedList<Instance>()
						);
						additionalNodes.add( nodeToAdd );
						// create proper parent relation
						newParent.addChild( nodeToAdd );
						newParent = nodeToAdd;
					}
					// Add missing links
					newParent.addChild( elem );
					elem.setParent( newParent );
					nodeHasParent[i] = true;
				}
				else {
					System.out.println( "There is no nearest paret for " + elem.getId() + " it means that something goes really bad." );
					System.exit( 1 );
				}
			}
		}

		// then consider BREADTH gaps between nodes
		if ( fillBreadthGaps ) {
			// consider gaps for each node's children
			LinkedList<BasicGroup> nodesToCheck = new LinkedList<BasicGroup>();
			nodesToCheck.add( root );
			while ( !nodesToCheck.isEmpty() ) {
				BasicGroup currentNode = nodesToCheck.removeFirst();
				String currentNodeId = currentNode.getId();
				int lengthOfCurrentId = currentNode.getId().length();
				int maxId = Integer.MIN_VALUE;
				HashSet<Integer> existingIds = new HashSet<Integer>();
				for ( Group child : currentNode.getChildren() ) // collect existing ids
				{
					if ( child.getId().startsWith( currentNodeId + Constants.HIERARCHY_BRANCH_SEPARATOR ) ) {
						String childNumberStr = child.getId().substring( lengthOfCurrentId + 1 );
						int childNumber = Integer.parseInt( childNumberStr );
						maxId = Math.max( maxId, childNumber );
						existingIds.add( childNumber );
						nodesToCheck.add( (BasicGroup)child );
					}
					else {
						System.err.println(
							"Fatal error! in filling breth gaps. " + currentNodeId + " is NOT a prepend of " +
								child.getId() + " but " + child.getId() + " is a CHILD of " + currentNodeId
						);
					}
				}

				for ( int i = 0; i <= maxId; i++ ) // fill gaps
				{
					if ( !existingIds.contains( i ) ) {
						BasicGroup nodeToAdd = new BasicGroup(
							currentNode.getId().concat( Constants.HIERARCHY_BRANCH_SEPARATOR + i ),
							currentNode, new LinkedList<Group>(), new LinkedList<Instance>()
						);
						additionalNodes.add( nodeToAdd );
						currentNode.addChild( nodeToAdd );
					}
				}
				Collections.sort( currentNode.getChildren(), new NodeComparator() );
			}
		}

		allNodes = (LinkedList<Group>)additionalNodes.clone();
		allNodes.addAll( nodes );

		return allNodes;
	}
}
