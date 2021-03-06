package pl.pwr.hiervis.prefuse.action;

import java.awt.Color;

import pl.pwr.hiervis.core.HVConfig;
import pl.pwr.hiervis.core.HVConstants;
import pl.pwr.hiervis.core.HVContext;
import pl.pwr.hiervis.hierarchy.ElementRole;
import prefuse.action.assignment.ColorAction;
import prefuse.data.Schema;
import prefuse.data.Tuple;
import prefuse.data.expression.AbstractExpression;
import prefuse.data.expression.ComparisonPredicate;
import prefuse.data.expression.Literal;
import prefuse.visual.VisualItem;


public class InstanceColorAction extends ColorAction
{
	private static final ComparisonPredicate pCurrent = getPredicateFor( ElementRole.CURRENT );
	private static final ComparisonPredicate pParent = getPredicateFor( ElementRole.DIRECT_PARENT );
	private static final ComparisonPredicate pAncestor = getPredicateFor( ElementRole.INDIRECT_PARENT );
	private static final ComparisonPredicate pChild = getPredicateFor( ElementRole.CHILD );
	private static final ComparisonPredicate pOther = getPredicateFor( ElementRole.OTHER );

	private HVContext context;
	private transient HVConfig tmp = null;


	public InstanceColorAction( HVContext context, String group, String field )
	{
		super( group, field );
		this.context = context;
	}

	@Override
	public void run( double frac )
	{
		tmp = context.getConfig();
		super.run( frac );
	}

	@Override
	public int getColor( VisualItem item )
	{
		if ( pCurrent.getBoolean( item ) ) {
			return tmp.getCurrentGroupColor().getRGB();
		}
		else if ( pParent.getBoolean( item ) ) {
			return tmp.getParentGroupColor().getRGB();
		}
		else if ( pAncestor.getBoolean( item ) ) {
			return tmp.getAncestorGroupColor().getRGB();
		}
		else if ( pChild.getBoolean( item ) ) {
			return tmp.getChildGroupColor().getRGB();
		}
		else if ( pOther.getBoolean( item ) ) {
			return tmp.getOtherGroupColor().getRGB();
		}
		else {
			return Color.magenta.getRGB();
		}
	}

	/**
	 * @param elementRole
	 *            the {@link ElementRole} to test for
	 * @return creates and returns a predicate which returns true for instances whose node's
	 *         {@link ElementRole} is the same as the one passed in argument.
	 */
	public static ComparisonPredicate getPredicateFor( ElementRole elementRole )
	{
		return new ComparisonPredicate(
			ComparisonPredicate.EQ,
			new InstanceNodeExpression(),
			Literal.getLiteral( elementRole.getNumber() )
		);
	}


	/**
	 * Given a row from the instance data table, extracts the node to which that instance belongs and returns
	 * its {@link ElementRole}.
	 */
	@SuppressWarnings("rawtypes")
	private static class InstanceNodeExpression extends AbstractExpression
	{
		public Class getType( Schema s )
		{
			return int.class;
		}

		public Object get( Tuple t )
		{
			return getInt( t );
		}

		public int getInt( Tuple t )
		{
			prefuse.data.Node node = (prefuse.data.Node)t.get( HVConstants.PREFUSE_INSTANCE_NODE_COLUMN_NAME );
			return node.getInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME );
		}
	}
}
