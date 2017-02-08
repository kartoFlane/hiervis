package pl.pwr.hiervis.util;

import java.awt.GridBagConstraints;
import java.awt.Insets;


public class GridBagConstraintsBuilder
{
	private GridBagConstraints constraints;


	public GridBagConstraintsBuilder()
	{
		constraints = new GridBagConstraints();
	}

	/**
	 * Replaces the underlying {@link GridBagConstraints} with a new instance, and returns the old one.
	 * 
	 * @return the {@link GridBagConstraints} with all the options that have been set thus far.
	 */
	public GridBagConstraints build()
	{
		GridBagConstraints result = constraints;
		constraints = new GridBagConstraints();
		return result;
	}

	/**
	 * Specifies how many cells should be occupied by the component.
	 */
	public GridBagConstraintsBuilder span( int gridCellsWidth, int gridCellsHeight )
	{
		constraints.gridwidth = gridCellsWidth;
		constraints.gridheight = gridCellsHeight;
		return this;
	}

	/**
	 * Specifies how many cells should be occupied by the component.
	 */
	public GridBagConstraintsBuilder spanHorizontal( int gridCells )
	{
		constraints.gridwidth = gridCells;
		return this;
	}

	/**
	 * Specifies how many cells should be occupied by the component.
	 */
	public GridBagConstraintsBuilder spanVertical( int gridCells )
	{
		constraints.gridheight = gridCells;
		return this;
	}

	/**
	 * Sets the position of the component in the layout.
	 */
	public GridBagConstraintsBuilder position( int x, int y )
	{
		constraints.gridx = x;
		constraints.gridy = y;
		return this;
	}

	/**
	 * Sets the X position of the component in the layout.
	 */
	public GridBagConstraintsBuilder positionX( int x )
	{
		constraints.gridx = x;
		return this;
	}

	/**
	 * Sets the Y position of the component in the layout.
	 */
	public GridBagConstraintsBuilder positionY( int y )
	{
		constraints.gridy = y;
		return this;
	}

	/**
	 * Makes the component fill leftover space in the layout.
	 */
	public GridBagConstraintsBuilder fill()
	{
		constraints.fill = GridBagConstraints.BOTH;
		return this;
	}

	/**
	 * Makes the component fill leftover horizontal space in the layout.
	 */
	public GridBagConstraintsBuilder fillHorizontal()
	{
		constraints.fill = GridBagConstraints.HORIZONTAL;
		return this;
	}

	/**
	 * Makes the component fill leftover vertical space in the layout.
	 */
	public GridBagConstraintsBuilder fillVertical()
	{
		constraints.fill = GridBagConstraints.VERTICAL;
		return this;
	}

	/**
	 * Makes the component maintain constant size, and not take up any leftover space.
	 */
	public GridBagConstraintsBuilder fillNone()
	{
		constraints.fill = GridBagConstraints.NONE;
		return this;
	}

	/**
	 * Specifies how to distribute extra space.
	 * 
	 * @see GridBagConstraints#weightx
	 * @see GridBagConstraints#weighty
	 */
	public GridBagConstraintsBuilder weight( double weightX, double weightY )
	{
		constraints.weightx = weightX;
		constraints.weighty = weightY;
		return this;
	}

	/**
	 * @see GridBagConstraints#weightx
	 */
	public GridBagConstraintsBuilder weightX( double weightX )
	{
		constraints.weightx = weightX;
		return this;
	}

	/**
	 * @see GridBagConstraints#weighty
	 */
	public GridBagConstraintsBuilder weightY( double weightY )
	{
		constraints.weighty = weightY;
		return this;
	}

	/**
	 * @see GridBagConstraints#ipadx
	 * @see GridBagConstraints#ipady
	 */
	public GridBagConstraintsBuilder padding( int padX, int padY )
	{
		constraints.ipadx = padX;
		constraints.ipady = padY;
		return this;
	}

	/**
	 * @see GridBagConstraints#ipadx
	 */
	public GridBagConstraintsBuilder paddingX( int padX )
	{
		constraints.ipadx = padX;
		return this;
	}

	/**
	 * @see GridBagConstraints#ipady
	 */
	public GridBagConstraintsBuilder paddingY( int padY )
	{
		constraints.ipady = padY;
		return this;
	}

	/**
	 * Sets the specified insets.
	 * 
	 * @see GridBagConstraints#insets
	 */
	public GridBagConstraintsBuilder insets( Insets i )
	{
		constraints.insets = i;
		return this;
	}

	/**
	 * Creates and sets new insets with the specified amount of pixels in each direction.
	 * 
	 * @see GridBagConstraints#insets
	 */
	public GridBagConstraintsBuilder insets( int i )
	{
		constraints.insets = new Insets( i, i, i, i );
		return this;
	}

	/**
	 * Creates and sets new insets with the specified values.
	 * 
	 * @see GridBagConstraints#insets
	 */
	public GridBagConstraintsBuilder insets( int top, int left, int bottom, int right )
	{
		constraints.insets = new Insets( top, left, bottom, right );
		return this;
	}

	/**
	 * Creates and sets new insets with the specified amount of pixels each horizontal direction (left and right).
	 * 
	 * @see GridBagConstraints#insets
	 */
	public GridBagConstraintsBuilder insetsHorizontal( int i )
	{
		constraints.insets = new Insets( 0, i, 0, i );
		return this;
	}

	/**
	 * Creates and sets new insets with the specified values.
	 * 
	 * @see GridBagConstraints#insets
	 */
	public GridBagConstraintsBuilder insetsHorizontal( int left, int right )
	{
		constraints.insets = new Insets( 0, left, 0, right );
		return this;
	}

	/**
	 * Creates and sets new insets with the specified amount of pixels each vertical direction (top and bottom).
	 * 
	 * @see GridBagConstraints#insets
	 */
	public GridBagConstraintsBuilder insetsVertical( int i )
	{
		constraints.insets = new Insets( i, 0, i, 0 );
		return this;
	}

	/**
	 * Creates and sets new insets with the specified values.
	 * 
	 * @see GridBagConstraints#insets
	 */
	public GridBagConstraintsBuilder insetsVertical( int top, int bottom )
	{
		constraints.insets = new Insets( top, 0, bottom, 0 );
		return this;
	}

	/**
	 * Sets the component alignment to the specified value.
	 * 
	 * @see GridBagConstraints#anchor
	 */
	public GridBagConstraintsBuilder anchor( int anchor )
	{
		constraints.anchor = anchor;
		return this;
	}

	/**
	 * Centers the component in its cell.
	 */
	public GridBagConstraintsBuilder anchorCenter()
	{
		constraints.anchor = GridBagConstraints.CENTER;
		return this;
	}

	/**
	 * Aligns the component at the top edge of its cell, and centers it horizontally.
	 */
	public GridBagConstraintsBuilder anchorNorth()
	{
		constraints.anchor = GridBagConstraints.NORTH;
		return this;
	}

	/**
	 * Aligns the component at the bottom edge of its cell, and centers it horizontally.
	 */
	public GridBagConstraintsBuilder anchorSouth()
	{
		constraints.anchor = GridBagConstraints.SOUTH;
		return this;
	}

	/**
	 * Aligns the component at the left edge of its cell, and centers it vertically.
	 */
	public GridBagConstraintsBuilder anchorWest()
	{
		constraints.anchor = GridBagConstraints.WEST;
		return this;
	}

	/**
	 * Aligns the component at the right edge of its cell, and centers it vertically.
	 */
	public GridBagConstraintsBuilder anchorEast()
	{
		constraints.anchor = GridBagConstraints.EAST;
		return this;
	}

	/**
	 * Aligns the component at the top-left corner of its cell.
	 */
	public GridBagConstraintsBuilder anchorNorthWest()
	{
		constraints.anchor = GridBagConstraints.NORTHWEST;
		return this;
	}

	/**
	 * Aligns the component at the top-right corner of its cell.
	 */
	public GridBagConstraintsBuilder anchorNorthEast()
	{
		constraints.anchor = GridBagConstraints.NORTHEAST;
		return this;
	}

	/**
	 * Aligns the component at the bottom-left corner of its cell.
	 */
	public GridBagConstraintsBuilder anchorSouthWest()
	{
		constraints.anchor = GridBagConstraints.SOUTHWEST;
		return this;
	}

	/**
	 * Aligns the component at the bottom-right corner of its cell.
	 */
	public GridBagConstraintsBuilder anchorSouthEast()
	{
		constraints.anchor = GridBagConstraints.SOUTHEAST;
		return this;
	}
}
