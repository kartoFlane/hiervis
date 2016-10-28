package pl.pwr.basic_hierarchy.implementation;

import java.util.LinkedList;

import pl.pwr.basic_hierarchy.interfaces.Instance;
import pl.pwr.basic_hierarchy.interfaces.Group;


public class BasicGroup implements Group
{
	private String id;
	private Group parent;
	private LinkedList<Group> children;
	private LinkedList<Instance> instances;


	public void setId( String id )
	{
		this.id = id;
	}

	public BasicGroup( String id, Group parent, LinkedList<Group> children, LinkedList<Instance> instances )
	{
		this.id = id;
		this.parent = parent;
		this.children = children;
		this.instances = instances;
	}

	public BasicGroup( String id, Group parent )
	{
		this( id, parent, new LinkedList<Group>(), new LinkedList<Instance>() );
	}

	public void setParent( Group parent )
	{
		this.parent = parent;
	}

	public void setChildren( LinkedList<Group> children )
	{
		this.children = children;
	}

	public void addChild( Group child )
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
	public Group getParent()
	{
		return parent;
	}

	@Override
	public String getParentId()
	{
		return parent.getId();
	}

	@Override
	public LinkedList<Group> getChildren()
	{
		return children;
	}

	@Override
	public LinkedList<Instance> getInstances()
	{
		return instances;
	}

	@Override
	public LinkedList<Instance> getSubgroupInstances()
	{
		LinkedList<Instance> result = new LinkedList<Instance>( instances );

		for ( Group child : children ) {
			result.addAll( child.getSubgroupInstances() );
		}

		return result;
	}

	@Override
	public Instance getGroupRepresentation()
	{
		// TODO: find the centroid/medoid node and return it?
		return null;
	}

	@Override
	public String toString()
	{
		return print( "", true );
	}

	private String print( String prefix, boolean isTail )
	{
		StringBuilder buf = new StringBuilder();

		buf.append( prefix )
			.append( isTail ? "L-- " : "|-- " )
			.append( id )
			.append( '(' )
			.append( instances.size() )
			.append( ')' )
			.append( '\n' );

		String childPrefix = prefix + ( isTail ? "    " : "|   " );

		// Print all children except last
		for ( int i = 0; i < children.size() - 1; ++i ) {
			Group n = children.get( i );
			if ( n instanceof BasicGroup ) {
				buf.append( ( (BasicGroup)n ).print( childPrefix, false ) )
					.append( '\n' );
			}
		}

		// Print the last child as tail
		if ( children.size() > 0 ) {
			Group n = children.get( children.size() - 1 );
			if ( n instanceof BasicGroup ) {
				buf.append( ( (BasicGroup)n ).print( childPrefix, true ) )
					.append( '\n' );
			}
		}

		return buf.toString();
	}
}
