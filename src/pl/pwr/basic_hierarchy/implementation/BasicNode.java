package pl.pwr.basic_hierarchy.implementation;

import java.util.LinkedList;

import pl.pwr.basic_hierarchy.interfaces.Instance;
import pl.pwr.basic_hierarchy.interfaces.Node;


public class BasicNode implements Node
{
	private String id;
	private Node parent;
	private LinkedList<Node> children;
	private LinkedList<Instance> instances;


	public void setId( String id )
	{
		this.id = id;
	}

	public BasicNode( String id, Node parent, LinkedList<Node> children, LinkedList<Instance> instances )
	{
		this.id = id;
		this.parent = parent;
		this.children = children;
		this.instances = instances;
	}

	public void setParent( Node parent )
	{
		this.parent = parent;
	}

	public void setChildren( LinkedList<Node> children )
	{
		this.children = children;
	}

	public void addChild( Node child )
	{
		this.children.add( child );
	}

	public void addInstance( Instance instance )
	{
		this.instances.add( instance );
	}

	public void setInstances( LinkedList<Instance> instances )
	{
		this.instances = instances;
	}

	@Override
	public String getId()
	{
		return id;
	}

	@Override
	public Node getParent()
	{
		return parent;
	}

	@Override
	public String getParentId()
	{
		return parent.getId();
	}

	@Override
	public LinkedList<Node> getChildren()
	{
		return children;
	}

	@Override
	public LinkedList<Instance> getNodeInstances()
	{
		return instances;
	}

	@Override
	public LinkedList<Instance> getSubtreeInstances()
	{
		LinkedList<Instance> subtreeInstances = new LinkedList<Instance>( instances );

		for ( Node child : children ) {
			subtreeInstances.addAll( child.getSubtreeInstances() );
		}
		return subtreeInstances;
	}

	@Override
	public Instance getNodeRepresentation()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void printSubtree()
	{
		print( "", true );
	}

	private void print( String prefix, boolean isTail )
	{
		System.out.println( prefix + ( isTail ? "L-- " : "|-- " ) + id + "(" + instances.size() + ")" );
		for ( int i = 0; i < children.size() - 1; i++ ) {
			( (BasicNode)children.get( i ) ).print( prefix + ( isTail ? "    " : "|   " ), false );
		}
		if ( children.size() > 0 ) {
			( (BasicNode)children.get( children.size() - 1 ) ).print( prefix + ( isTail ? "    " : "|   " ), true );
		}
	}
}
