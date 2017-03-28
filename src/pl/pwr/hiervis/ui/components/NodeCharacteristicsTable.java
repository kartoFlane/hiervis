package pl.pwr.hiervis.ui.components;

import java.util.LinkedList;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import basic_hierarchy.interfaces.Instance;
import basic_hierarchy.interfaces.Node;


@SuppressWarnings("serial")
public class NodeCharacteristicsTable extends JTable
{
	private Node node;
	private String[] dimNames;
	private boolean useSubtree = false;


	public NodeCharacteristicsTable( String[] dimNames, Node node )
	{
		super();

		this.node = node;
		this.dimNames = dimNames;

		String headerNames[] = { "Feature", "Min", "Max", "Avg", "Stdev" };
		this.setModel(
			new DefaultTableModel( headerNames, 0 ) {
				@Override
				public boolean isCellEditable( int row, int col )
				{
					return false;
				}
			}
		);
	}

	public void setUseSubtree( boolean useSubtree )
	{
		this.useSubtree = useSubtree;
	}

	public boolean isUseSubtree()
	{
		return useSubtree;
	}

	public void updateTable()
	{
		DefaultTableModel model = (DefaultTableModel)getModel();
		while ( model.getRowCount() > 0 )
			model.removeRow( 0 );

		LinkedList<Instance> instances = useSubtree
			? node.getSubtreeInstances()
			: node.getNodeInstances();

		for ( int j = 0; j < dimNames.length; ++j ) {
			final int index = j;
			double[] values = instances.stream().mapToDouble( in -> in.getData()[index] ).toArray();

			double[] minMaxAvg = computeMinMaxAvg( values );
			double stdev = computeStdev( values, minMaxAvg[2] );

			model.addRow( new Object[] { dimNames[j], minMaxAvg[0], minMaxAvg[1], minMaxAvg[2], stdev } );
		}

		revalidate();
		repaint();
	}

	private static double[] computeMinMaxAvg( double[] values )
	{
		double[] result = new double[3];
		result[0] = Double.MAX_VALUE;
		result[1] = Double.MIN_VALUE;

		double sum = 0;
		for ( double value : values ) {
			result[0] = Math.min( result[0], value );
			result[1] = Math.max( result[1], value );
			sum += value;
		}

		result[2] = sum / values.length;

		return result;
	}

	private static double computeVariance( double[] values, double mean )
	{
		double variance = 0;
		for ( double value : values ) {
			variance += Math.pow( value - mean, 2 );
		}
		return variance / values.length;
	}

	private static double computeStdev( double[] values, double mean )
	{
		return Math.sqrt( computeVariance( values, mean ) );
	}
}
