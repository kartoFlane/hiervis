package pl.pwr.hiervis.util;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import pl.pwr.hiervis.core.HVConfig;


public class ImageUtils
{
	private ImageUtils()
	{
		// Static class -- disallow instantiation.
		throw new RuntimeException( "Attempted to instantiate a static class: " + getClass().getName() );
	}

	/**
	 * Returns a rotated copy of the image passed in argument.
	 * 
	 * @param img
	 *            The image to be rotated
	 * @param angle
	 *            The angle in degrees
	 * @return The rotated image
	 */
	public static BufferedImage rotate( BufferedImage img, double angle )
	{
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

	public static BufferedImage trimImg( BufferedImage img, HVConfig config )
	{
		// TODO instead of iterating through the columns of image, we can use BINARY SEARCH through columns
		// and check if column doesn't contain at least 1 non-background colour pixel
		int imgHeight = img.getHeight();
		int imgWidth = img.getWidth();

		// TRIM WIDTH - LEFT
		int startWidth = 0;
		for ( int x = 0; x < imgWidth; x++ ) {
			if ( startWidth == 0 ) {
				for ( int y = 0; y < imgHeight; y++ ) {
					if ( img.getRGB( x, y ) != config.getBackgroundColor().getRGB() ) {
						startWidth = x;
						break;
					}
				}
			}
			else
				break;
		}

		// TRIM WIDTH - RIGHT
		int endWidth = 0;
		for ( int x = imgWidth - 1; x >= 0; x-- ) {
			if ( endWidth == 0 ) {
				for ( int y = 0; y < imgHeight; y++ ) {
					if ( img.getRGB( x, y ) != config.getBackgroundColor().getRGB() ) {
						endWidth = x;
						break;
					}
				}
			}
			else
				break;
		}

		int newWidth = endWidth - startWidth;

		BufferedImage newImg = new BufferedImage(
			newWidth,
			imgHeight,
			BufferedImage.TYPE_INT_RGB
		);
		Graphics g = newImg.createGraphics();
		g.drawImage( img, 0, 0, newImg.getWidth(), newImg.getHeight(), startWidth, 0, endWidth, imgHeight, null );
		img = newImg;

		return img;
	}

	public static BufferedImage addBorder(
		BufferedImage img,
		int leftWidth, int rightWidth,
		int topHeight, int bottomHeight,
		Color color )
	{
		BufferedImage borderedImg = new BufferedImage(
			img.getWidth() + leftWidth + rightWidth,
			img.getHeight() + topHeight + bottomHeight,
			img.getType()
		);
		Graphics2D g = borderedImg.createGraphics();

		g.setColor( color );
		g.fillRect( 0, 0, borderedImg.getWidth(), borderedImg.getHeight() );
		g.drawImage( img, leftWidth, topHeight, img.getWidth(), img.getHeight(), null );

		return borderedImg;
	}

	public static BufferedImage setBackgroud( BufferedImage image, Color backgroundColor )
	{
		Graphics2D g2d = image.createGraphics();
		g2d.setPaint( backgroundColor );
		g2d.fillRect( 0, 0, image.getWidth(), image.getHeight() );
		g2d.dispose();
		return image;
	}
}
