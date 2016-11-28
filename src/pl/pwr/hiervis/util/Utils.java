package pl.pwr.hiervis.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.reflect.Array;

import basic_hierarchy.interfaces.Instance;
import basic_hierarchy.interfaces.Node;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.activity.ActivityManager;
import prefuse.util.ColorLib;
import prefuse.util.GraphicsLib;
import prefuse.util.display.DisplayLib;


public class Utils
{
	private Utils()
	{
		// Static class -- disallow instantiation.
		throw new RuntimeException( "Attempted to instantiate a static class: " + getClass().getName() );
	}

	public static void waitUntilActivitiesAreFinished()
	{
		while ( ActivityManager.activityCount() > 0 ) {
			try {
				// Poll every 100ms
				Thread.sleep( 100 );
			}
			catch ( InterruptedException e ) {
				e.printStackTrace();
			}
		}
	}

	public static Point2D getDisplayCenter( Display display )
	{
		Rectangle2D contentRect = display.getVisibleRect();

		return new Point2D.Double(
			contentRect.getCenterX(),
			contentRect.getCenterY()
		);
	}

	public static void resetDisplayZoom( Display display, int margin, int duration )
	{
		Visualization vis = display.getVisualization();
		Rectangle2D bounds = vis.getBounds( Visualization.ALL_ITEMS );
		GraphicsLib.expand( bounds, margin + (int)( 1 / display.getScale() ) );
		DisplayLib.fitViewToBounds( display, bounds, duration );
	}

	public static BufferedImage getDisplaySnapshot( Display dis )
	{
		BufferedImage img = null;
		try {
			// get an image to draw into
			Dimension d = new Dimension( dis.getWidth(), dis.getHeight() );
			if ( !GraphicsEnvironment.isHeadless() ) {
				try {
					img = (BufferedImage)dis.createImage( dis.getWidth(), dis.getHeight() );
				}
				catch ( Exception e ) {
					img = null;
				}
			}

			if ( img == null ) {
				img = new BufferedImage(
					dis.getWidth(),
					dis.getHeight(),
					BufferedImage.TYPE_INT_RGB
				);
			}
			Graphics2D g = (Graphics2D)img.getGraphics();

			// set up the display, render, then revert to normal settings
			Point2D p = new Point2D.Double( 0, 0 );
			dis.zoom( p, 1.0 ); // also takes care of damage report
			boolean q = dis.isHighQuality();
			dis.setHighQuality( true );
			dis.paintDisplay( g, d );
			dis.setHighQuality( q );
			dis.zoom( p, 1.0 ); // also takes care of damage report
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}

		return img;
	}

	public static int rgba( Color c )
	{
		return ColorLib.rgba( c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha() );
	}

	/**
	 * Calculates the smallest rectangle containing all points within
	 * the specified group.
	 * 
	 * @param group
	 *            the group for which the extents are to be computed
	 * @param dimX
	 *            index of the data dimension imaged on the X axis
	 * @param dimY
	 *            index of the data dimension imaged on the Y axis
	 * @return the smallest bounding rectangle
	 */
	public static Rectangle2D calculateBoundingRectForCluster( Node group, int dimX, int dimY )
	{
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;

		for ( Instance i : group.getSubtreeInstances() ) {
			double x = i.getData()[dimX];
			double y = i.getData()[dimY];

			minX = Math.min( minX, x );
			minY = Math.min( minY, y );
			maxX = Math.max( maxX, x );
			maxY = Math.max( maxY, y );
		}

		return new Rectangle2D.Double( minX, minY, maxX - minX, maxY - minY );
	}

	/**
	 * Normalize the specified source value from its source range into target range.
	 * 
	 * @param sourceValue
	 *            the source value
	 * @param sourceMin
	 *            the minimum source value
	 * @param sourceMax
	 *            the maximum source value
	 * @param targetMin
	 *            the minimum target value
	 * @param targetMax
	 *            the maximum target value
	 * @return
	 * 		the source value mapped to the target range
	 */
	public static double normalize(
		double sourceValue,
		double sourceMin, double sourceMax,
		double targetMin, double targetMax )
	{
		// Use linear interpolation to map from the source range to target range.
		double t = ( sourceValue - sourceMin ) / ( sourceMax - sourceMin );
		return targetMin + t * ( targetMax - targetMin );
	}

	public static int clamp( int min, int value, int max )
	{
		return Math.min( max, Math.max( min, value ) );
	}

	public static long clamp( long min, long value, long max )
	{
		return Math.min( max, Math.max( min, value ) );
	}

	public static float clamp( float min, float value, float max )
	{
		return Math.min( max, Math.max( min, value ) );
	}

	public static double clamp( double min, double value, double max )
	{
		return Math.min( max, Math.max( min, value ) );
	}

	/**
	 * Creates a generic array of the specified type and size.
	 * 
	 * @param clazz
	 *            type of the array
	 * @param size
	 *            size of the array
	 * @return the generic array
	 */
	@SuppressWarnings("unchecked")
	public <E> E[] createArray( Class<E> clazz, int size )
	{
		return (E[])Array.newInstance( clazz, size );
	}
}
