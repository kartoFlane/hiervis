package pl.pwr.hiervis.util.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import pl.pwr.hiervis.util.HSV;
import pl.pwr.hiervis.util.Utils;


/**
 * A component which allows the user to select a color's hue.
 * 
 * @author Tomasz Bachmiński
 *
 */
public class HuePicker extends JComponent
{
	public static final String SELECTED_HUE = HuePicker.class.getCanonicalName() + ":SELECTED_HUE";

	private static final long serialVersionUID = -614405052134068408L;

	private int indicatorSize = 8;
	private Color selectionFillColor = Color.white;
	private Color selectionBorderColor = Color.gray;
	private Color hueBorderColor = Color.lightGray;

	private float selectedHue = 1.0f;

	private boolean paintHuesMaximized = false;
	private boolean paintHueBorder = true;


	public HuePicker()
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

	public void setIndicatorSize( int size )
	{
		if ( size <= 0 )
			throw new IllegalArgumentException( "Size must be greater than 0." );
		indicatorSize = size;
		repaint();
	}

	public int getIndicatorSize()
	{
		return indicatorSize;
	}

	/**
	 * If set to true, the hue band will be painted to occupy the entire width
	 * of this component, without trying to fit it in between the selection
	 * indicators.
	 * This is useful for when the component has very small width compared
	 * to the selection indicators.
	 */
	public void setPaintHuesMaximized( boolean value )
	{
		paintHuesMaximized = value;
		repaint();
	}

	public boolean getPaintHuesMaximized()
	{
		return paintHuesMaximized;
	}

	/**
	 * If set to true, the hue band will be painted with a 1px border around it.
	 */
	public void setPaintHueBorder( boolean value )
	{
		paintHueBorder = value;
		repaint();
	}

	public boolean getPaintHueBorder()
	{
		return paintHueBorder;
	}

	public void setSelection( float hue )
	{
		if ( hue < 0 || hue > 1.0f )
			throw new IllegalArgumentException( "0 < " + hue + " < 1.0" );
		float o = selectedHue;
		selectedHue = hue;
		repaint();
		firePropertyChange( SELECTED_HUE, o, hue );
	}

	public void setSelection( Color color )
	{
		setSelection( new HSV( color ).getHue() );
	}

	public float getSelection()
	{
		return selectedHue;
	}

	private void setSelection( MouseEvent e )
	{
		if ( SwingUtilities.isLeftMouseButton( e ) ) {
			final int minY = indicatorSize / 2;
			final int maxY = getHeight() - indicatorSize / 2;

			float y = Utils.clamp( minY, e.getY(), maxY ) - minY;
			setSelection( 1f - ( y / ( maxY - minY ) ) );
		}
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

		paintHues( g2 );
		paintSelectionIndicator( g2 );
	}

	protected void paintHues( Graphics g )
	{
		final int x;
		final int w;
		final int h = getHeight() - indicatorSize;
		final float stepY = 1f / h;

		if ( paintHuesMaximized ) {
			if ( paintHueBorder ) {
				x = 1;
				w = getWidth() - 2;
			}
			else {
				x = 0;
				w = getWidth();
			}
		}
		else {
			x = indicatorSize;
			w = getWidth() - 2 * indicatorSize;
		}

		if ( paintHueBorder ) {
			g.setColor( hueBorderColor );
			g.fillRect( x - 1, indicatorSize / 2 - 1, w + 2, h + 2 );
		}

		for ( int y = 0; y < h; ++y ) {
			Color c = new Color( Color.HSBtoRGB( 1f - y * stepY, 1, 1 ) );
			g.setColor( c );
			g.fillRect( x, y + indicatorSize / 2, w, 1 );
		}
	}

	protected void paintSelectionIndicator( Graphics g )
	{
		final int w = getWidth();
		final int h = getHeight() - indicatorSize;

		int hOffset = Math.round( ( 1f - selectedHue ) * h );
		if ( hOffset == h )
			hOffset -= 1;
		hOffset += indicatorSize / 2;

		Polygon leftTri = new Polygon();
		leftTri.addPoint( 0, hOffset - indicatorSize / 2 );
		leftTri.addPoint( 0, hOffset + indicatorSize / 2 );
		leftTri.addPoint( indicatorSize, hOffset );

		Polygon rightTri = new Polygon();
		rightTri.addPoint( w - 1, hOffset - indicatorSize / 2 );
		rightTri.addPoint( w - 1, hOffset + indicatorSize / 2 );
		rightTri.addPoint( w - indicatorSize - 1, hOffset );

		g.setColor( selectionFillColor );
		g.fillPolygon( leftTri );
		g.fillPolygon( rightTri );
		g.setColor( selectionBorderColor );
		g.drawPolygon( leftTri );
		g.drawPolygon( rightTri );
	}
}
