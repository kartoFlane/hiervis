package pl.pwr.hiervis.ui.control;

import java.awt.event.MouseEvent;
import java.util.function.Function;

import prefuse.Display;
import prefuse.controls.ControlAdapter;
import prefuse.visual.VisualItem;


public class CustomToolTipControl extends ControlAdapter
{
	private Function<VisualItem, String> toolTipGenerator;


	public CustomToolTipControl( Function<VisualItem, String> toolTipGenerator )
	{
		if ( toolTipGenerator == null )
			throw new IllegalArgumentException( "Argument must not be null." );
		this.toolTipGenerator = toolTipGenerator;
	}

	/**
	 * @see prefuse.controls.Control#itemEntered(prefuse.visual.VisualItem, java.awt.event.MouseEvent)
	 */
	public void itemEntered( VisualItem item, MouseEvent e )
	{
		Display d = (Display)e.getSource();
		d.setToolTipText( toolTipGenerator.apply( item ) );
	}

	/**
	 * @see prefuse.controls.Control#itemExited(prefuse.visual.VisualItem, java.awt.event.MouseEvent)
	 */
	public void itemExited( VisualItem item, MouseEvent e )
	{
		Display d = (Display)e.getSource();
		d.setToolTipText( null );
	}
}
