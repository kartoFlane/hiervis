package pl.pwr.hiervis.util;

import java.awt.Color;


/**
 * A container class used to represent colors using the HSV color model.
 * 
 * @author Tomasz Bachmiñski
 *
 */
public class HSV {

	private float h;
	private float s;
	private float v;


	public HSV( float h, float s, float v ) {
		set( h, s, v );
	}

	public HSV( HSV hsv ) {
		this.h = hsv.h;
		this.s = hsv.s;
		this.v = hsv.v;
	}

	public HSV( Color color ) {
		float[] hsv = Color.RGBtoHSB( color.getRed(), color.getGreen(), color.getBlue(), null );
		h = hsv[0];
		s = hsv[1];
		v = hsv[2];
	}

	public void set( float h, float s, float v ) {
		setHue( h );
		setSaturation( s );
		setValue( v );
	}

	public void setHue( float hue ) {
		if ( hue < 0 || hue > 1.0f )
			throw new IllegalArgumentException( "Hue: 0 < " + hue + " < 1.0" );
		h = hue;
	}

	public void setSaturation( float saturation ) {
		if ( saturation < 0 || saturation > 1.0f )
			throw new IllegalArgumentException( "Saturation: 0 < " + saturation + " < 1.0" );
		s = saturation;
	}

	public void setValue( float value ) {
		if ( value < 0 || value > 1.0f )
			throw new IllegalArgumentException( "Value: 0 < " + value + " < 1.0" );
		v = value;
	}

	/** Same as {@code setValue()}. */
	public void setBrightness( float brightness ) {
		setValue( brightness );
	}

	public float getHue() {
		return h;
	}

	public float getSaturation() {
		return s;
	}

	public float getValue() {
		return v;
	}

	/** Same as {@code getValue()}. */
	public float getBrightness() {
		return v;
	}

	/**
	 * Creates a Color instance out of this HSV container.
	 * 
	 * @return a new Color instance
	 */
	public Color toColor() {
		return new Color( Color.HSBtoRGB( h, s, v ) );
	}

	@Override
	public String toString() {
		return String.format( "HSV { %s, %s, %s }", h, s, v );
	}
}
