package pl.pwr.hiervis.prefuse;

import prefuse.data.Table;


public class TableEx extends Table
{
	public void dispose()
	{
		if ( m_listeners != null )
			m_listeners.clear();
		m_listeners = null;
		if ( m_columns != null )
			m_columns.clear();
		m_columns = null;
		if ( m_names != null )
			m_names.clear();
		m_names = null;
		if ( m_entries != null )
			m_entries.clear();
		m_entries = null;
		if ( m_rows != null )
			m_rows.clear();
		m_rows = null;
		m_tuples = null;
		m_schema = null;
	}
}
