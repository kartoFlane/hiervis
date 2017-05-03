package pl.pwr.hiervis.prefuse;

import prefuse.Display;
import prefuse.Visualization;


public class DisplayEx extends Display
{
	public DisplayEx( Visualization v )
	{
		super( v );
	}

	public void dispose()
	{
		if ( m_vis != null ) {
			m_vis.reset();
		}
		setVisualization( null );
		m_predicate.clear();
		if ( m_controls != null )
			m_controls.clear();
		if ( m_painters != null )
			m_painters.clear();
		if ( m_bounders != null )
			m_bounders.clear();
		if ( m_queue != null ) {
			m_queue.clean();
			m_queue.clear();
		}
	}
}
