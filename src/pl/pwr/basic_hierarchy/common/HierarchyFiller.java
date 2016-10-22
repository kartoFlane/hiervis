package pl.pwr.basic_hierarchy.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;

import pl.pwr.basic_hierarchy.implementation.BasicNode;
import pl.pwr.basic_hierarchy.interfaces.Instance;
import pl.pwr.basic_hierarchy.interfaces.Node;


public class HierarchyFiller
{
	@SuppressWarnings("unchecked")
	public static LinkedList<Node> addMissingEmptyNodes(
		BasicNode root,
		ArrayList<BasicNode> nodes,
		int rootIndexInNodes,
		boolean fillBreathGaps
	)
	{
		LinkedList<Node> allNodes;
		// hierarchy build

		// search for parent
		boolean[] nodeHasParent = new boolean[nodes.size()];
		for ( int i = 0; i < nodes.size(); i++ ) {
			BasicNode parentNode = nodes.get( i );
			String[] parentBranchId = parentNode.getId().split( Constants.HIERARCHY_BRANCH_SEPARATOR_REGEX );
			int parentHeightNum = parentBranchId.length;
			// REFACTOR if we have a map of nodes then below search could be quicker
			for ( int j = 0/* i+1 */; j < nodes.size(); j++ )// assume that nodes are in DFS order in input file
			{
				BasicNode elem = nodes.get( j );
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
		LinkedList<BasicNode> additionalNodes = new LinkedList<BasicNode>();
		if ( rootIndexInNodes >= 0 ) {
			nodeHasParent[0] = nodes.get( rootIndexInNodes ).equals( root );// skip root from consideration
		}
		for ( int i = 0; i < nodeHasParent.length; i++ ) {
			if ( !nodeHasParent[i] )// no direct parent
			{
				BasicNode elem = nodes.get( i );
				String[] elemBranchId = elem.getId().split( Constants.HIERARCHY_BRANCH_SEPARATOR_REGEX );
				int elemHeightNum = elemBranchId.length;
				// REFACTOR below defined two loops have duplicated code
				// find nearest parent
				BasicNode nearestParent = null;
				int nearestParentHeightNum = Integer.MIN_VALUE;
				// REFACTOR below defined loop could be quicker when there will be a flag of nodes
				for ( int j = 0; j < i; j++ ) {
					BasicNode parentNode = nodes.get( j );
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
					BasicNode potentialParentNode = additionalNodes.get( j );
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
					BasicNode newParent = nearestParent;
					for ( int k = nearestParentHeightNum; k < elemHeightNum - 1; k++ ) {
						String nodeToAddIdPostfix = elemBranchId[k];
						String nodeToAddId = newParent.getId().concat( Constants.HIERARCHY_BRANCH_SEPARATOR ).concat( nodeToAddIdPostfix );

						// add empty node
						BasicNode nodeToAdd = new BasicNode(
							nodeToAddId, newParent,
							new LinkedList<Node>(), new LinkedList<Instance>()
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

		// then consider BREATH gaps between nodes
		if ( fillBreathGaps ) {
			// consider gaps for each node's children
			LinkedList<BasicNode> nodesToCheck = new LinkedList<BasicNode>();
			nodesToCheck.add( root );
			while ( !nodesToCheck.isEmpty() ) {
				BasicNode currentNode = nodesToCheck.removeFirst();
				String currentNodeId = currentNode.getId();
				int lengthOfCurrentId = currentNode.getId().length();
				int maxId = Integer.MIN_VALUE;
				HashSet<Integer> existingIds = new HashSet<Integer>();
				for ( Node child : currentNode.getChildren() )// collect existing ids
				{
					if ( child.getId().startsWith( currentNodeId + Constants.HIERARCHY_BRANCH_SEPARATOR ) ) {
						String childNumberStr = child.getId().substring( lengthOfCurrentId + 1 );
						int childNumber = Integer.parseInt( childNumberStr );
						maxId = Math.max( maxId, childNumber );
						existingIds.add( childNumber );
						nodesToCheck.add( (BasicNode)child );
					}
					else {
						System.err.println(
							"Fatal error! in filling breth gaps. " + currentNodeId + " is NOT a prepend of " +
								child.getId() + " but " + child.getId() + " is a CHILD of " + currentNodeId
						);
					}
				}

				for ( int i = 0; i <= maxId; i++ )// fill gaps
				{
					if ( !existingIds.contains( i ) ) {
						BasicNode nodeToAdd = new BasicNode(
							currentNode.getId().concat( Constants.HIERARCHY_BRANCH_SEPARATOR + i ),
							currentNode, new LinkedList<Node>(), new LinkedList<Instance>()
						);
						additionalNodes.add( nodeToAdd );
						currentNode.addChild( nodeToAdd );
					}
				}
				Collections.sort( currentNode.getChildren(), new NodeIdComparator() );
			}
		}

		allNodes = (LinkedList<Node>)additionalNodes.clone();
		allNodes.addAll( nodes );
		return allNodes;
	}
}
