package pl.pwr.hiervis.util;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import basic_hierarchy.interfaces.Instance;
import basic_hierarchy.interfaces.Node;
import pl.pwr.hiervis.core.HVConstants;
import prefuse.Display;
import prefuse.activity.ActivityManager;


public class Utils {

	private Utils() {
		// Static class -- disallow instantiation.
		throw new RuntimeException( "Attempted to instantiate a static class: " + getClass().getName() );
	}

	public static void waitUntilActivitiesAreFinished() {
		while ( ActivityManager.activityCount() > 0 ) {
			try {
				Thread.sleep( HVConstants.SLEEP_TIME );
			}
			catch ( InterruptedException e ) {
				e.printStackTrace();
			}
		}
	}

	public static Point2D getDisplayCenter( Display display ) {
		Rectangle2D contentRect = display.getVisibleRect();

		return new Point2D.Double(
				contentRect.getCenterX(),
				contentRect.getCenterY() );
	}

	public static void resetDisplayZoom( Display display ) {
		display.zoomAbs( getDisplayCenter( display ), 1 / display.getScale() );
	}

	public static BufferedImage getDisplaySnapshot( Display dis ) {
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
						BufferedImage.TYPE_INT_RGB );
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
	 * Calculates the smallest rectangle containing all points within
	 * the specified node's cluster.
	 * 
	 * @param node
	 *            the node for which the extents are to be computed
	 * @return the smallest bounding rectangle
	 */
	public static Rectangle2D calculateBoundingRectForCluster( Node node ) {
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double maxX = Double.MIN_VALUE;
		double maxY = Double.MIN_VALUE;

		for ( Instance i : node.getSubtreeInstances() ) {
			double x = i.getData()[0];
			double y = i.getData()[1];

			minX = Math.min( minX, x );
			minY = Math.min( minY, y );
			maxX = Math.max( maxX, x );
			maxY = Math.max( maxY, y );
		}

		return new Rectangle2D.Double( minX, minY, maxX - minX, maxY - minY );
	}

	public static int clamp( int min, int value, int max ) {
		return Math.min( max, Math.max( min, value ) );
	}

	public static long clamp( long min, long value, long max ) {
		return Math.min( max, Math.max( min, value ) );
	}

	public static float clamp( float min, float value, float max ) {
		return Math.min( max, Math.max( min, value ) );
	}

	public static double clamp( double min, double value, double max ) {
		return Math.min( max, Math.max( min, value ) );
	}
}
