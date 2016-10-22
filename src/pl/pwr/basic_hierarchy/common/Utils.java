package pl.pwr.basic_hierarchy.common;

import java.util.Stack;

import pl.pwr.basic_hierarchy.interfaces.Node;


public class Utils
{
	/*
	 * Applicable only to positive numbers
	 */
	public static boolean isPositiveNumeric( String str )
	{
		for ( char c : str.toCharArray() ) {
			if ( !Character.isDigit( c ) ) return false;
		}
		return true;
	}

	public int getNumberOfSubtreeGroups( Node subtreeRoot )
	{
		Stack<Node> s = new Stack<Node>();
		s.push( subtreeRoot );
		int numberOfGroups = 0;
		while ( !s.empty() ) {
			Node n = s.pop();
			numberOfGroups++;
			for ( Node child : n.getChildren() ) {
				s.push( child );
			}
		}
		return numberOfGroups;
	}
}
