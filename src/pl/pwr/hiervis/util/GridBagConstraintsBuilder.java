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

	public GridBagConstraints build()
	{
		GridBagConstraints result = constraints;
		constraints = new GridBagConstraints();
		return result;
	}

	public GridBagConstraintsBuilder span( int gridCellsWidth, int gridCellsHeight )
	{
		constraints.gridwidth = gridCellsWidth;
		constraints.gridheight = gridCellsHeight;
		return this;
	}

	public GridBagConstraintsBuilder spanHorizontal( int gridCells )
	{
		constraints.gridwidth = gridCells;
		return this;
	}

	public GridBagConstraintsBuilder spanVertical( int gridCells )
	{
		constraints.gridheight = gridCells;
		return this;
	}

	public GridBagConstraintsBuilder position( int x, int y )
	{
		constraints.gridx = x;
		constraints.gridy = y;
		return this;
	}

	public GridBagConstraintsBuilder positionX( int x )
	{
		constraints.gridx = x;
		return this;
	}

	public GridBagConstraintsBuilder positionY( int y )
	{
		constraints.gridy = y;
		return this;
	}

	public GridBagConstraintsBuilder fill()
	{
		constraints.fill = GridBagConstraints.BOTH;
		return this;
	}

	public GridBagConstraintsBuilder fillHorizontal()
	{
		constraints.fill = GridBagConstraints.HORIZONTAL;
		return this;
	}

	public GridBagConstraintsBuilder fillVertical()
	{
		constraints.fill = GridBagConstraints.VERTICAL;
		return this;
	}

	public GridBagConstraintsBuilder fillNone()
	{
		constraints.fill = GridBagConstraints.NONE;
		return this;
	}

	public GridBagConstraintsBuilder weight( double weightX, double weightY )
	{
		constraints.weightx = weightX;
		constraints.weighty = weightY;
		return this;
	}

	public GridBagConstraintsBuilder weightX( double weightX )
	{
		constraints.weightx = weightX;
		return this;
	}

	public GridBagConstraintsBuilder weightY( double weightY )
	{
		constraints.weighty = weightY;
		return this;
	}

	public GridBagConstraintsBuilder padding( int padX, int padY )
	{
		constraints.ipadx = padX;
		constraints.ipady = padY;
		return this;
	}

	public GridBagConstraintsBuilder paddingX( int padX )
	{
		constraints.ipadx = padX;
		return this;
	}

	public GridBagConstraintsBuilder paddingY( int padY )
	{
		constraints.ipady = padY;
		return this;
	}

	public GridBagConstraintsBuilder insets( Insets i )
	{
		constraints.insets = i;
		return this;
	}

	public GridBagConstraintsBuilder insets( int i )
	{
		constraints.insets = new Insets( i, i, i, i );
		return this;
	}

	public GridBagConstraintsBuilder insets( int top, int left, int bottom, int right )
	{
		constraints.insets = new Insets( top, left, bottom, right );
		return this;
	}

	public GridBagConstraintsBuilder insetsHorizontal( int i )
	{
		constraints.insets = new Insets( 0, i, 0, i );
		return this;
	}

	public GridBagConstraintsBuilder insetsVertical( int i )
	{
		constraints.insets = new Insets( i, 0, i, 0 );
		return this;
	}

	/**
	 * @see GridBagConstraints#anchor
	 */
	public GridBagConstraintsBuilder anchor( int anchor )
	{
		constraints.anchor = anchor;
		return this;
	}

	public GridBagConstraintsBuilder anchorCenter()
	{
		constraints.anchor = GridBagConstraints.CENTER;
		return this;
	}

	public GridBagConstraintsBuilder anchorNorth()
	{
		constraints.anchor = GridBagConstraints.NORTH;
		return this;
	}

	public GridBagConstraintsBuilder anchorSouth()
	{
		constraints.anchor = GridBagConstraints.SOUTH;
		return this;
	}

	public GridBagConstraintsBuilder anchorWest()
	{
		constraints.anchor = GridBagConstraints.WEST;
		return this;
	}

	public GridBagConstraintsBuilder anchorEast()
	{
		constraints.anchor = GridBagConstraints.EAST;
		return this;
	}

	public GridBagConstraintsBuilder anchorNorthWest()
	{
		constraints.anchor = GridBagConstraints.NORTHWEST;
		return this;
	}

	public GridBagConstraintsBuilder anchorNorthEast()
	{
		constraints.anchor = GridBagConstraints.NORTHEAST;
		return this;
	}

	public GridBagConstraintsBuilder anchorSouthWest()
	{
		constraints.anchor = GridBagConstraints.SOUTHWEST;
		return this;
	}

	public GridBagConstraintsBuilder anchorSouthEast()
	{
		constraints.anchor = GridBagConstraints.SOUTHEAST;
		return this;
	}
}
