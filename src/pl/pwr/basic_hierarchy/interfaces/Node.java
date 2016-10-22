package pl.pwr.basic_hierarchy.interfaces;

import java.util.LinkedList;


public interface Node
{
	public String getId();

	public Node getParent();

	public String getParentId();

	public LinkedList<Node> getChildren();

	/*
	 * Should returns instances belonging to this particular node
	 */
	public LinkedList<Instance> getNodeInstances();

	/*
	 * Should returns instances belonging to this node and its predecessors
	 */
	public LinkedList<Instance> getSubtreeInstances();

	/*
	 * Could be node centroid or medoid
	 */
	public Instance getNodeRepresentation();

	public void printSubtree();
}
