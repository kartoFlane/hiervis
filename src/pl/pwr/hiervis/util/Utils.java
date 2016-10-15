package pl.pwr.hiervis.util;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import pl.pwr.hiervis.core.HVConstants;
import prefuse.activity.ActivityManager;


public class Utils {
	
	/**
	 * Returns a rotated copy of the image passed in argument.
	 * 
	 * @param img
	 *            The image to be rotated
	 * @param angle
	 *            The angle in degrees
	 * @return The rotated image
	 */
	public static BufferedImage rotate( BufferedImage img, double angle ) {
		double sin = Math.abs( Math.sin( Math.toRadians( angle ) ) );
		double cos = Math.abs( Math.cos( Math.toRadians( angle ) ) );

		int w = img.getWidth( null );
		int h = img.getHeight( null );

		int neww = (int)Math.floor( w * cos + h * sin );
		int newh = (int)Math.floor( h * cos + w * sin );

		BufferedImage bimg = new BufferedImage( neww, newh, BufferedImage.TYPE_INT_ARGB );// toBufferedImage(getEmptyImage(neww, newh));
		Graphics2D g = bimg.createGraphics();

		g.translate( ( neww - w ) / 2, ( newh - h ) / 2 );
		g.rotate( Math.toRadians( angle ), w / 2, h / 2 );
		g.drawRenderedImage( img, null );
		g.dispose();

		return bimg;
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
