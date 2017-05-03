package pl.pwr.hiervis.prefuse;

import java.lang.reflect.Field;

import prefuse.data.CascadedTable;
import prefuse.data.Table;
import prefuse.data.event.TableListener;


public class TableEx extends Table
{
	public void dispose()
	{
		if ( m_listeners != null )
			m_listeners.clear();
		if ( m_columns != null )
			m_columns.clear();
		if ( m_names != null )
			m_names.clear();
		if ( m_entries != null )
			m_entries.clear();
		if ( m_rows != null )
			m_rows.clear();
		if ( m_tuples != null )
			m_tuples.invalidateAll();
	}

	public static void disposeCascadedTable( CascadedTable vt )
	{
		// Need to use reflection to access protected fields in order to
		// unregister the internal listener...
		// Easier than creating a dozen of *Ex classes just to add cleanup functionality.
		try {
			Field f = CascadedTable.class.getDeclaredField( "m_listener" );
			f.setAccessible( true );
			TableListener listener = (TableListener)f.get( vt );
			vt.getParentTable().removeTableListener( listener );
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
	}
}
