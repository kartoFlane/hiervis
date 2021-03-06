package pl.pwr.hiervis.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.logging.log4j.Logger;

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

	/**
	 * Zooms in the view for the specified display so that the view encompasses all items
	 * in the specified group, with the given margin from the display's edges. Animated over
	 * the specified duration.
	 * 
	 * @param display
	 *            the display to zoom
	 * @param group
	 *            the group of items to zoom in to
	 * @param margin
	 *            the bounds that should be visible in the Display view
	 * @param duration
	 *            the duration of an animated transition. A value of zero will result in an instantaneous change.
	 */
	public static void fitToBounds( Display display, String group, int margin, int duration )
	{
		Visualization vis = display.getVisualization();
		Rectangle2D bounds = vis.getBounds( group );
		GraphicsLib.expand( bounds, margin + (int)( 1 / display.getScale() ) );
		DisplayLib.fitViewToBounds( display, bounds, duration );
	}

	/**
	 * Resets zoom of the specified display. Animated over the specified duration.
	 * 
	 * @param display
	 *            the display to reset
	 * @param duration
	 *            the duration of animated transition
	 */
	public static void unzoom( Display display, int duration )
	{
		DisplayLib.fitViewToBounds( display, display.getBounds(), duration );
	}

	/**
	 * Convenience method to set transform of a Display.
	 */
	public static void setTransform( Display display, AffineTransform transform )
	{
		try {
			display.setTransform( transform );
		}
		catch ( NoninvertibleTransformException e ) {
			throw new RuntimeException( "Implementation error: this should never happen.", e );
		}
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

	/**
	 * Executes the specified runnable in a timed block, and prints the measured
	 * time to the specified logger, with the specified prefix.
	 * 
	 * @param log
	 *            the logger to print the result to
	 * @param prefix
	 *            the prefix in the printed message
	 * @param f
	 *            the runnable to time
	 */
	public static void timed( Logger log, String prefix, Runnable f )
	{
		StopWatch sw = new StopWatch();

		sw.start();
		f.run();
		sw.stop();

		log.trace(
			String.format(
				"%s: %sms (%sns)",
				prefix, sw.getTime(), sw.getNanoTime()
			)
		);
	}

	public static int rgba( Color c )
	{
		return ColorLib.rgba( c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha() );
	}

	/**
	 * Returns a rectangle containing the smallest and largest values for each dimension in the specified node.
	 * 
	 * @param node
	 *            the node for which the extents are to be computed
	 * @param dimX
	 *            index of the data dimension imaged on the X axis
	 * @param dimY
	 *            index of the data dimension imaged on the Y axis
	 * @return the smallest bounding rectangle
	 */
	public static Rectangle2D calculateBoundingRectForCluster( Node node, int dimX, int dimY )
	{
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;

		for ( Instance i : node.getSubtreeInstances() ) {
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
	public static <E> E[] createArray( Class<E> clazz, int size )
	{
		return (E[])Array.newInstance( clazz, size );
	}

	/**
	 * Merges the two arrays passed in argument into a single one,
	 * in the order specified.
	 * 
	 * @param arrayA
	 *            the first array
	 * @param arrayB
	 *            the second array
	 * @return new array
	 */
	public static <E> E[] merge( E[] arrayA, E[] arrayB )
	{
		E[] result = Arrays.copyOf( arrayA, arrayA.length + arrayB.length );
		System.arraycopy( arrayB, 0, result, arrayA.length, arrayB.length );
		return result;
	}

	/**
	 * Runs the main method in the specified class in a subprocess. This method does not wait for the
	 * subprocess to terminate.
	 * 
	 * Source:
	 * https://stackoverflow.com/questions/636367/executing-a-java-application-in-a-separate-process
	 * 
	 * @param clazz
	 *            the class to start
	 * @return handle to the started subprocess
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static Process start( Class<?> clazz, String... args ) throws IOException
	{
		String javaHome = System.getProperty( "java.home" );
		String javaBin = javaHome +
			File.separator + "bin" +
			File.separator + "java";
		String classpath = System.getProperty( "java.class.path" );
		String className = clazz.getCanonicalName();

		String[] commandArgs = { javaBin, "-cp", classpath, className };
		return new ProcessBuilder( merge( commandArgs, args ) ).start();
	}
}
