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
		m_predicate = null;
		if ( m_controls != null )
			m_controls.clear();
		m_controls = null;
		if ( m_painters != null )
			m_painters.clear();
		m_painters = null;
		if ( m_bounders != null )
			m_bounders.clear();
		m_bounders = null;
		m_offscreen = null;
		m_clip = null;
		m_screen = null;
		m_bounds = null;
		m_rclip = null;
		m_bgpainter = null;
		if ( m_queue != null ) {
			m_queue.clean();
			m_queue.clear();
		}
		m_queue = null;
		m_transform = null;
		m_itransform = null;
		m_transact = null;
		m_tmpPoint = null;
		m_customToolTip = null;
	}
}
