package pl.pwr.hiervis.util.ui;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import pl.pwr.hiervis.util.HSV;
import pl.pwr.hiervis.util.Utils;


/**
 * A component which allows the user to select a color's shade.
 * 
 * @author Tomasz Bachmiński
 *
 */
public class ShadePicker extends JComponent
{
	public static final String SELECTED_HUE = ShadePicker.class.getCanonicalName() + ":SELECTED_HUE";
	public static final String SELECTED_SATURATION = ShadePicker.class.getCanonicalName() + ":SELECTED_SATURATION";
	public static final String SELECTED_VALUE = ShadePicker.class.getCanonicalName() + ":SELECTED_VALUE";

	private static final long serialVersionUID = 3299771386071172028L;
	private static final Color gradientMaskTranslucent = new Color( 0, 0, 0, 0 );

	private int selectionRadius = 5;

	private HSV hsv = new HSV( 1f, 0f, 1f );


	public ShadePicker()
	{
		super();

		MouseAdapter ml = new MouseAdapter() {
			@Override
			public void mousePressed( MouseEvent e )
			{
				setSelection( e );
			}

			@Override
			public void mouseDragged( MouseEvent e )
			{
				setSelection( e );
			}
		};

		this.addMouseListener( ml );
		this.addMouseMotionListener( ml );
	}

	public void setSelectionRadius( int radius )
	{
		if ( radius <= 0 )
			throw new IllegalArgumentException( "Radius must be greater than 0." );
		selectionRadius = radius;
		repaint();
	}

	public int getSelectionRadius()
	{
		return selectionRadius;
	}

	public void setHue( float hue )
	{
		float o = hsv.getHue();
		hsv.setHue( hue );
		repaint();
		firePropertyChange( SELECTED_HUE, o, hue );
	}

	public void setHue( Color color )
	{
		if ( color == null )
			throw new IllegalArgumentException( "Argument must not be null." );
		setHue( new HSV( color ).getHue() );
	}

	public void setSaturation( float saturation )
	{
		float o = hsv.getSaturation();
		hsv.setSaturation( saturation );
		repaint();
		firePropertyChange( SELECTED_SATURATION, o, saturation );
	}

	public void setSaturation( Color color )
	{
		if ( color == null )
			throw new IllegalArgumentException( "Argument must not be null." );
		setSaturation( new HSV( color ).getSaturation() );
	}

	public void setValue( float value )
	{
		float o = hsv.getValue();
		hsv.setValue( value );
		repaint();
		firePropertyChange( SELECTED_VALUE, o, value );
	}

	public void setValue( Color color )
	{
		if ( color == null )
			throw new IllegalArgumentException( "Argument must not be null." );
		setValue( new HSV( color ).getValue() );
	}

	public void setSelection( float saturation, float value )
	{
		float os = hsv.getSaturation();
		float ov = hsv.getValue();

		hsv.setSaturation( saturation );
		hsv.setValue( value );

		repaint();

		firePropertyChange( SELECTED_SATURATION, os, saturation );
		firePropertyChange( SELECTED_VALUE, ov, value );
	}

	public void setSelection( Color color )
	{
		if ( color == null )
			throw new IllegalArgumentException( "Argument must not be null." );

		HSV hsv = new HSV( color );
		setHue( hsv.getHue() );
		setSelection( hsv.getSaturation(), hsv.getValue() );
	}

	private void setSelection( MouseEvent e )
	{
		if ( SwingUtilities.isLeftMouseButton( e ) ) {
			float width = getWidth();
			float height = getHeight();

			float x = Utils.clamp( 0, e.getX(), width );
			float y = Utils.clamp( 0, e.getY(), height );

			setSelection( x / width, 1f - ( y / height ) );
		}
	}

	public HSV getSelection()
	{
		return new HSV( hsv );
	}

	@Override
	public void paintComponent( Graphics g )
	{
		super.paintComponent( g );

		Graphics2D g2 = (Graphics2D)g;
		g2.setRenderingHint(
			RenderingHints.KEY_ANTIALIASING,
			RenderingHints.VALUE_ANTIALIAS_ON
		);

		paintShades( g2 );
		paintSelectionIndicator( g2 );
	}

	protected void paintShades( Graphics g )
	{
		int w = getWidth();
		int h = getHeight();

		Graphics2D g2 = (Graphics2D)g;

		g2.setColor( Color.lightGray );
		g2.fillRect( 0, 0, w, h );

		Paint p = g2.getPaint();

		GradientPaint primary = new GradientPaint(
			0, 0, Color.white,
			w, 0, new HSV( hsv.getHue(), 1.0f, 1.0f ).toColor()
		);
		GradientPaint secondary = new GradientPaint(
			0, 0, gradientMaskTranslucent,
			0, h, Color.black
		);

		g2.setPaint( primary );
		g2.fillRect( 0, 0, w, h );
		g2.setPaint( secondary );
		g2.fillRect( 0, 0, w, h );

		g2.setPaint( p );
	}

	protected void paintSelectionIndicator( Graphics g )
	{
		final int s = (int)( hsv.getSaturation() * getWidth() );
		final int v = (int)( ( 1 - hsv.getValue() ) * getHeight() );

		// Color the selection indicator depending on its distance from top-left corner,
		// so that it remains visible.
		float d = (float)( Math.sqrt( Math.pow( 1 - hsv.getValue(), 2 ) + Math.pow( hsv.getSaturation(), 2 ) ) );
		// Cap it at some point to prevent it from melding with gray background.
		d = d > 0.4 ? 1 : 0;

		g.setColor( new HSV( 0, 0, d ).toColor() );
		g.drawOval( s - selectionRadius, v - selectionRadius, selectionRadius * 2, selectionRadius * 2 );
	}
}
