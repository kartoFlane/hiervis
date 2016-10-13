package pl.pwr.hierarchyvis.util.prefuse.histogram;

//TODO @@@ How can I prevent people from modifying the table?  Can I mark 
//the columns as read-only?

//I'm afraid of doing too much to prevent setting, as the constructor needs
//to set things.  Can I set things by doing super.set() etc?

//TODO @@@ do I want to allow using predicates on this table?  Probably...

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;

import prefuse.data.Table;
import prefuse.data.Tuple;
import prefuse.data.column.Column;
import prefuse.data.event.ColumnListener;
import prefuse.data.parser.DoubleParser;
import prefuse.data.parser.IntParser;
import prefuse.data.parser.StringParser;
import prefuse.data.tuple.TableTuple;
import prefuse.data.tuple.TupleManager;
import prefuse.data.util.RowManager;
import prefuse.util.collections.CopyOnWriteArrayList;


/**
 * <p>
 * A HistogramTable is a subclass of (@link prefuse.data.Table), but
 * that has been histogramized: one column of the original
 * (@link prefuse.data.Table) gets counted and slotted into a Table.
 * The first (@link prefuse.data.column.Column) is the ranges of the data,
 * with the value of the cell corresponding to the minimum value of the range.
 * The second column holds the number of each element in the original Table
 * that falls within the data range represented by the cell in column 1.
 * 
 * NOTE: This only works with numeric and string fields.
 * It has not been tested with booleans or derived fields.
 * Booleans will probably be treated as strings.
 * 
 * Known bug: See the HistogramFrame class comments about an axis bug.
 * 
 * @author <a href="http://webfoot.com/ducky.home.html">Kaitlin Duck Sherwood</a>
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class HistogramTable extends Table implements ColumnListener {

	// m_bin{Min, Max} are the min and max of the data column BUT for Strings, min is 1
	// and max is the number of unique strings.
	protected Hashtable<String, Double> m_binMin = new Hashtable<String, Double>();
	protected Hashtable<String, Double> m_binMax = new Hashtable<String, Double>();

	protected Hashtable<String, Integer> m_countMins = new Hashtable<String, Integer>();
	protected Hashtable<String, Integer> m_countMaxes = new Hashtable<String, Integer>();
	protected double m_binWidth;
	protected int m_binCount;
	static final int DEFAULT_BIN_COUNT = 15;
	static final String ROLE_COLUMN_NAME = "role";


	public HistogramTable( Table aTable )
			throws Exception {
		this( aTable, DEFAULT_BIN_COUNT, -1 );
	}

	/**
	 * @param aTable
	 *            a Prefuse Table with data values (i.e. non-histogrammized) in it
	 * @param aBinCount
	 *            how many bins the data's range should be split into
	 * @throws Exception
	 */
	public HistogramTable( Table aTable, int aBinCount, int roleNumber ) {
		super();

		String[] fieldNames = getFieldNames( aTable );

		m_binCount = aBinCount;
		initializeHistogramTable( fieldNames, m_binCount );

		for ( int fieldIndex = 0; fieldIndex < fieldNames.length; fieldIndex++ ) {
			String field = fieldNames[fieldIndex];
			Column dataColumn = aTable.getColumn( field );

			if ( dataColumn == null ) {
				throw new RuntimeException( String.format( "No column could be found for field '%s'", field ) );
			}

			if ( aBinCount <= 0 ) {
				throw new RuntimeException( "Bin count must not be negative: " + aBinCount );
			}

			if ( dataColumn.canGetDouble() ) {
				initializeNumericColumn( field, dataColumn, false );
			}
			else if ( dataColumn.canGetString() ) {
				initializeStringColumn( field, dataColumn );
			}
			else {
				throw new RuntimeException(
						String.format( "Field '%s' is neither a number nor a string. What to do with this?", field ) );
			}
		}

		// One deletion (one pass of this loop) allows to delete all empty bins,
		// because emptiness of OX bins indicates that on OY is also empty
		for ( int fieldIndex = 0; fieldIndex < fieldNames.length; fieldIndex++ ) {
			String field = fieldNames[fieldIndex];

			Column dataColumn = aTable.getColumn( field );
			removeRightEmptyBins( field, dataColumn );
		}

		addColumn( ROLE_COLUMN_NAME, int.class );
		for ( int i = 0; i < getRowCount(); i++ ) {
			set( i, ROLE_COLUMN_NAME, roleNumber );
		}
	}

	// REFACTOR: there is a duplication of code with above defined constructor
	public HistogramTable( Table aTable, /* int aBinCount, */ HistogramTable computedBinsToUse, int roleNumber ) {
		super();

		String[] fieldNames = getFieldNames( aTable );

		m_binCount = computedBinsToUse.m_binCount;// aBinCount;
		initializeHistogramTable( fieldNames, m_binCount );

		for ( int fieldIndex = 0; fieldIndex < fieldNames.length; fieldIndex++ ) {
			String field = fieldNames[fieldIndex];
			Column dataColumn = aTable.getColumn( field );

			if ( dataColumn == null ) {
				throw new RuntimeException( String.format( "No column could be found for field '%s'", field ) );
			}

			if ( m_binCount <= 0 ) {
				throw new RuntimeException( "Bin count must not be negative: " + m_binCount );
			}

			if ( dataColumn.canGetDouble() ) {
				m_binMax = computedBinsToUse.m_binMax;// .put(field, maxValue);
				m_binMin = computedBinsToUse.m_binMin;// .put(field,minValue);

				double minValue = m_binMin.get( field );
				double maxValue = m_binMax.get( field );

				// m_binWidth = (1+ maxValue - minValue) / m_binCount;
				m_binWidth = ( maxValue - minValue ) / m_binCount;

				assert m_binWidth >= 0.0 : "m_binWidth < 0!";

				initializeNumericColumn( field, dataColumn, true );
			}
			else if ( dataColumn.canGetString() ) {
				initializeStringColumn( field, dataColumn );
			}
			else {
				throw new RuntimeException(
						String.format( "Field '%s' is neither a number nor a string. What to do with this?", field ) );
			}
		}

		addColumn( ROLE_COLUMN_NAME, int.class );
		for ( int i = 0; i < getRowCount(); i++ ) {
			set( i, ROLE_COLUMN_NAME, roleNumber );
		}
	}

	/**
	 * @param aTable
	 *            a HistogramTable or Prefuse Table
	 * @return fieldNames a list of the names of the columns in the table.
	 *         Note that a HistogramTable will have all the same column names as
	 *         are in its (Prefuse Table) data table's, but will also have an additional
	 *         set of columns that have the counts. See getCountField().
	 *         TODO It might be interesting to have a method getNonCountFieldNames which
	 *         strips out the count fields.
	 */
	public static String[] getFieldNames( Table aTable ) {
		int columnCount = aTable.getColumnCount();
		String[] fieldNames = new String[columnCount];

		for ( int columnIndex = 0; columnIndex < columnCount; columnIndex++ ) {
			fieldNames[columnIndex] = aTable.getColumnName( columnIndex );
		}

		return fieldNames;
	}

	/**
	 * @param field
	 *            the name of the dataColumn to histogrammize
	 * @param dataColumn
	 *            the dataColumn to histogrammize
	 *            Note: I was unable to figure out how to get the name of a column
	 *            from a Column object. That seemed odd. -- KDS
	 *            Note that I don't actually have to pass in the dataColumn --
	 *            I can get that from the field name -- but the dataColumn happens
	 *            to be handy, so why not.
	 */
	private void initializeStringColumn( String field, Column dataColumn ) {
		// create the two columns, one for bins and one for counts
		addColumn( field, String.class );
		Column binColumn = getColumn( field );
		binColumn.setParser( new StringParser() );

		String countField = getCountField( field );
		addColumn( countField, int.class );
		Column countColumn = getColumn( countField );
		countColumn.setParser( new IntParser() );

		// min/max are kept around (i.e. are instance variables) because
		// they get used a lot when recalculating axes.
		m_countMins.put( field, 0 );
		m_countMaxes.put( field, 0 );

		// make a hash with the keys/counts (because that's easy); transfer
		// into the column later
		m_binWidth = 1;
		Hashtable<String, Integer> values = new Hashtable<String, Integer>();
		int count = 0;
		String key;

		for ( int rowIndex = 0; rowIndex < dataColumn.getRowCount(); rowIndex++ ) {
			key = dataColumn.getString( rowIndex );
			if ( values.get( key ) != null ) {
				count = values.get( key );
			}
			else {
				// this is the first one
				count = 0;
			}
			values.put( key, ++count );
		}

		// with all the values (keys) and their counts in a hash, it's easy to
		// figure out how many unique keys there are
		m_binMin.put( field, 0.0 );
		m_binMax.put( field, (double)values.size() );

		// It is quite possible for the number of bins to be different
		// than the number of unique ordinal values. If there are more
		// ordinal values than bins, just show the arbitrarily first
		// m_binCount ones; if there are fewer, pad the ends with null.
		int rowIndex = 0;
		Enumeration<String> keys = values.keys();
		while ( keys.hasMoreElements() && rowIndex < m_binCount ) {
			key = (String)keys.nextElement();
			{
				binColumn.setString( key, rowIndex );
				countColumn.setInt( values.get( key ), rowIndex++ );
			}
		}

		// insert dummy values if there are fewer unique strings than bins
		for ( int i = rowIndex; i < getRowCount(); i++ ) {
			binColumn.setString( "", i );
			countColumn.setInt( 0, i );
		}
	}

	/**
	 * @param field
	 *            the name of the dataColumn to histogrammize
	 * @param dataColumn
	 *            the dataColumn to histogrammize
	 */
	private void initializeNumericColumn( String field, Column dataColumn, boolean binsAlreadyComputed ) {
		addColumn( field, double.class );
		getColumn( field ).setParser( new DoubleParser() );
		String countField = getCountField( field );

		addColumn( countField, int.class );
		getColumn( countField ).setParser( new IntParser() );

		if ( !binsAlreadyComputed ) {
			initializeNumericBinInfo( field, dataColumn );
		}
		initializeNumericBinColumn( field );
		initializeCountColumn( field, dataColumn );
	}

	/**
	 * @param field
	 *            the name of the dataColumn to histogrammize
	 * @param dataColumn
	 *            the dataColumn to histogrammize
	 */
	private void initializeNumericBinInfo( String field, Column dataColumn ) {
		double[] minMax = new double[2];
		minMax = getNumericColumnMinMax( dataColumn );
		double minValue = minMax[0];
		double maxValue = minMax[1];

		m_binMax.put( field, maxValue );
		m_binMin.put( field, minValue );

		// m_binWidth = (1+maxValue - minValue) / m_binCount;
		m_binWidth = ( maxValue - minValue ) / m_binCount;

		assert m_binWidth >= 0.0 : "m_binWidth < 0!";
	}


	/**
	 * Fill in the histogram table.
	 * 
	 * @param fields
	 *            the names of all the fields
	 * @param rowCount
	 *            the number of rows
	 */
	@SuppressWarnings("rawtypes")
	private void initializeHistogramTable( String[] fields, int rowCount ) {
		int columnCount = 2 * fields.length;

		m_listeners = new CopyOnWriteArrayList();
		m_columns = new ArrayList( columnCount );
		m_names = new ArrayList( columnCount );
		m_entries = new HashMap( columnCount + 5 );
		m_rows = new RowManager( this );
		m_tuples = new TupleManager( this, null, TableTuple.class );

		addRows( rowCount );
	}


	/**
	 * Initialize the bin column. The bin columns have information on
	 * the range of values that the count columns have counts for. For
	 * example, you can say "there are 17 elements between the value of
	 * 2 and 14". 17 would be the value in the count field, and 2-14
	 * would be represented by the bin field. Note that the way that
	 * bin fields are represented, the value in the bin field is the low
	 * end of the range. In the example, the bin field would have a 2
	 * in it.
	 * 
	 * @param field
	 *            the name of the dataColumn to histogrammize
	 */
	private void initializeNumericBinColumn( String field ) {
		double dataColumnMin = m_binMin.get( field );
		// System.out.println("FIELD: " + field + " BIN WIDTH: " + m_binWidth);
		for ( int binIndex = 0; binIndex < m_binCount; binIndex++ ) {
			set( binIndex, field, dataColumnMin + binIndex * m_binWidth );
			// System.out.println("Bin: " + binIndex + " Range: " + (dataColumnMin + binIndex*m_binWidth)
			// + " - " + (dataColumnMin + binIndex*m_binWidth + m_binWidth));
		}
	}


	/**
	 * Initialize the column with the counts of elements in them.
	 * 
	 * @param field
	 *            the name of the dataColumn to histogrammize
	 * @param dataColumn
	 *            the column in the original (@link prefuse.data.Table)
	 *            to be histogramized.
	 */
	private void initializeCountColumn( String field, Column dataColumn ) {
		int currentCount;  // separate var just for debugging ease
		String countField = getCountField( field );

		// initialize everything to 0 before starting to count
		for ( int binIndex = 0; binIndex < m_binCount; binIndex++ ) {
			set( binIndex, countField, 0 );
		}

		double dataColumnMin = m_binMin.get( field );
		double cellValue;
		for ( int dataRowIndex = 0; dataRowIndex < dataColumn.getRowCount(); dataRowIndex++ ) {
			int binSlot = -1;
			cellValue = dataColumn.getDouble( dataRowIndex );
			double tmp = ( ( cellValue - dataColumnMin ) / m_binWidth );
			binSlot = (int)tmp;

			if ( binSlot == m_binCount ) {
				binSlot = m_binCount - 1;
			}
			// System.out.println(tmp + " " + binSlot);

			currentCount = getInt( binSlot, countField );
			setInt( binSlot, countField, currentCount + 1 );
		}

		// printWholeTable();
	}

	private void removeRightEmptyBins( String field, Column dataColumn ) {
		String countField = getCountField( field );

		boolean foundNonZeroCount = false;
		for ( int binSlot = m_binCount - 1; binSlot >= 0 && !foundNonZeroCount; binSlot-- ) {
			int currentCount = getInt( binSlot, countField );
			if ( currentCount == 0 ) {
				System.err.println( "On " + field + " histogram, there is additional empty bin on the right or bottom side."
						+ " In odrer to remove it, decrease number of bins. Honestly, this should never happen." );
			}
			else {
				foundNonZeroCount = true;
			}
		}
		// System.out.println("=======================================");
		// printWholeTable();
	}

	// For debugging. It has its uses.
	@SuppressWarnings("unchecked")
	public void printWholeTable() {
		Tuple t;
		for ( Iterator<Tuple> tuplesIterator = tuples(); tuplesIterator.hasNext(); ) {

			t = tuplesIterator.next();
			System.out.println( t.toString() );
		}
	}


	public double getBinMin( String field ) {
		return m_binMin.get( field );
	}

	public double getBinMax( String field ) {
		return m_binMax.get( field );
	}

	public int getBinCount() {
		return m_binCount;
	}

	/**
	 * @param aColumn
	 *            the column to get min/max of
	 * @return min and max (in an array) of aColumn
	 */
	private double[] getNumericColumnMinMax( Column aColumn ) {
		double oldMin = aColumn.getDouble( 0 );
		double oldMax = oldMin;
		double[] minMax = new double[2];

		if ( aColumn.canGetDouble() ) {
			double currentValue;
			for ( int rowIndex = 1; rowIndex < aColumn.getRowCount(); rowIndex++ ) {
				currentValue = aColumn.getDouble( rowIndex );
				oldMin = Math.min( oldMin, currentValue );
				oldMax = Math.max( oldMax, currentValue );
			}
		}

		minMax[0] = oldMin;
		minMax[1] = oldMax;
		return minMax;
	}

	/**
	 * @param field
	 *            the name of the histogramColumn
	 * @return the minimum and maximum values of the associated
	 *         count column in an array.
	 */
	private double[] getNumericColumnMinMax( String field ) {
		Column col = getColumn( field );

		return getNumericColumnMinMax( col );
	}

	/**
	 * @param field
	 *            the name of the histogramColumn
	 * @return the minimum value of the associated count column
	 *         (In other words, if you ask for getCountMin("A"), you
	 *         will get the min of the column "A counts".)
	 */
	public double getCountMin( String field ) {

		String countField = getCountField( field );
		double[] minMax = new double[2];

		if ( m_countMaxes.get( countField ) == null ) {
			minMax = getNumericColumnMinMax( countField );
			m_countMins.put( countField, (int)minMax[0] );
			m_countMaxes.put( countField, (int)minMax[1] );
		}

		return m_countMins.get( countField );
	}

	/**
	 * @param field
	 *            the name of the histogramColumn
	 * @return the max value of the associated count column
	 *         (In other words, if you ask for getCountMin("A"), you
	 *         will get the max of the column "A counts".)
	 */
	public double getCountMax( String field ) {
		String countField = getCountField( field );

		if ( m_countMaxes.get( countField ) == null ) {
			getCountMin( field );  // sets both
		}

		return m_countMaxes.get( countField );
	}


	public static String getCountField( String field ) {
		return field + " count";
	}

	public double getBinWidth() {
		return m_binWidth;
	}
}
