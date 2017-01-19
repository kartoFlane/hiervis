package pl.pwr.hiervis.ui;

import java.awt.Adjustable;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicLabelUI;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import basic_hierarchy.interfaces.Hierarchy;
import basic_hierarchy.interfaces.Node;
import pl.pwr.hiervis.core.HVConstants;
import pl.pwr.hiervis.core.HVContext;
import pl.pwr.hiervis.ui.components.MouseWheelEventBubbler;
import pl.pwr.hiervis.ui.components.VerticalLabelUI;
import pl.pwr.hiervis.ui.control.PanControl;
import pl.pwr.hiervis.ui.control.ZoomScrollControl;
import pl.pwr.hiervis.util.Event;
import pl.pwr.hiervis.util.SwingUIUtils;
import pl.pwr.hiervis.util.Utils;
import pl.pwr.hiervis.visualisation.HierarchyProcessor;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.controls.ToolTipControl;


@SuppressWarnings("serial")
public class InstanceVisualizationsFrame extends JFrame
{
	private static final Logger log = LogManager.getLogger( InstanceVisualizationsFrame.class );

	private static final int defaultLabelHeight = new JLabel( " " ).getPreferredSize().height;
	private static final int visWidthMin = 100;
	private static final int visWidthMax = 1000;
	private static final int visHeightMin = 100;
	private static final int visHeightMax = 1000;
	private static final int visZoomIncrement = 5;

	public final Event<Pair<Integer, Boolean>> dimensionVisibilityToggled = new Event<>();


	private HVContext context;

	// TODO: Move this to config, but make modifiable in the vis frame?
	private int visWidth = 200;
	private int visHeight = 200;
	private int pointSize = 3;
	private Insets displayInsets = new Insets( 5, 5, 5, 5 );

	private HashMap<Pair<Integer, Integer>, Display> displayMap;
	private Map<Integer, JComponent> dimHolderMap;

	private JCheckBox[] cboxesHorizontal;
	private JCheckBox[] cboxesVertical;

	private JPanel cDimsH;
	private JPanel cDimsV;
	private JPanel cCols;
	private JPanel cRows;
	private JPanel cViewport;


	/*
	 * TODO
	 * - dimension headers need to update to reflect display sizes
	 * - checkboxes need to wrap around when there's a large number of them, or scroll
	 * - visualizations *sometimes* have the wrong initial size; need to force max vis area
	 * - seem to recall seeing the scrollview being stretched out by many visualizations
	 * and then not shrinking correctly after they were hidden
	 * - histograms
	 * - double click to open large frame to show only that vis in large mode
	 */

	public InstanceVisualizationsFrame( HVContext context, Frame owner )
	{
		super( "Instance Visualizations" );
		this.context = context;

		displayMap = new HashMap<>();
		dimHolderMap = new HashMap<>();

		setDefaultCloseOperation( HIDE_ON_CLOSE );

		createGUI();

		context.hierarchyChanging.addListener( this::onHierarchyChanging );
		context.hierarchyChanged.addListener( this::onHierarchyChanged );
		context.nodeSelectionChanged.addListener( this::onNodeSelectionChanged );
		dimensionVisibilityToggled.addListener( this::onDimensionVisibilityToggled );
	}

	// ----------------------------------------------------------------------------------------

	private void createGUI()
	{
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gridBagLayout.rowWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		getContentPane().setLayout( gridBagLayout );

		cDimsH = new JPanel();
		GridBagConstraints gbc_cDimsH = new GridBagConstraints();
		gbc_cDimsH.anchor = GridBagConstraints.WEST;
		gbc_cDimsH.fill = GridBagConstraints.VERTICAL;
		gbc_cDimsH.insets = new Insets( 5, 5, 5, 5 );
		gbc_cDimsH.gridx = 1;
		gbc_cDimsH.gridy = 0;
		getContentPane().add( cDimsH, gbc_cDimsH );
		cDimsH.setLayout( new BoxLayout( cDimsH, BoxLayout.X_AXIS ) );

		cDimsV = new JPanel();
		GridBagConstraints gbc_cDimsV = new GridBagConstraints();
		gbc_cDimsV.anchor = GridBagConstraints.NORTH;
		gbc_cDimsV.fill = GridBagConstraints.HORIZONTAL;
		gbc_cDimsV.insets = new Insets( 5, 5, 5, 5 );
		gbc_cDimsV.gridx = 0;
		gbc_cDimsV.gridy = 1;
		getContentPane().add( cDimsV, gbc_cDimsV );
		cDimsV.setLayout( new BoxLayout( cDimsV, BoxLayout.Y_AXIS ) );

		JScrollPane scrollPane = new JScrollPane();
		GridBagConstraints gbc_cScrollVis = new GridBagConstraints();
		gbc_cScrollVis.fill = GridBagConstraints.BOTH;
		gbc_cScrollVis.gridx = 1;
		gbc_cScrollVis.gridy = 1;
		getContentPane().add( scrollPane, gbc_cScrollVis );

		cViewport = new JPanel();
		cViewport.setLayout( new BoxLayout( cViewport, BoxLayout.Y_AXIS ) );

		scrollPane.setViewportView( cViewport );

		cCols = new JPanel();
		cCols.setLayout( new BoxLayout( cCols, BoxLayout.X_AXIS ) );
		cRows = new JPanel();
		cRows.setLayout( new BoxLayout( cRows, BoxLayout.Y_AXIS ) );

		scrollPane.setViewportBorder( UIManager.getBorder( "ScrollPane.border" ) );
		scrollPane.setColumnHeaderView( cCols );
		scrollPane.setRowHeaderView( cRows );

		scrollPane.getColumnHeader().setScrollMode( JViewport.BACKINGSTORE_SCROLL_MODE );
		scrollPane.getRowHeader().setScrollMode( JViewport.BACKINGSTORE_SCROLL_MODE );

		scrollPane.getHorizontalScrollBar().setUnitIncrement( 16 );
		scrollPane.getVerticalScrollBar().setUnitIncrement( 16 );

		// Remove and replace the original listener to prevent the view from scrolling
		// when using control+scroll to resize all visible Displays
		scrollPane.removeMouseWheelListener( scrollPane.getMouseWheelListeners()[0] );
		scrollPane.addMouseWheelListener(
			new MouseWheelListener() {
				@Override
				public void mouseWheelMoved( MouseWheelEvent e )
				{
					if ( e.isControlDown() ) {
						visWidth -= e.getWheelRotation() * visZoomIncrement;
						visHeight -= e.getWheelRotation() * visZoomIncrement;

						visWidth = Utils.clamp( visWidthMin, visWidth, visWidthMax );
						visHeight = Utils.clamp( visHeightMin, visHeight, visHeightMax );

						for ( Display display : displayMap.values() ) {
							display.setPreferredSize( new Dimension( visWidth, visHeight ) );
							display.setSize( visWidth, visHeight );
						}

						int insetsH = displayInsets.left + displayInsets.right;
						int insetsV = displayInsets.top + displayInsets.bottom;

						int[] visDimsH = getVisibleDimensions( true );
						int[] visDimsV = getVisibleDimensions( false );

						Dimension labelSizeH = new Dimension( visWidth + insetsH, defaultLabelHeight );
						Dimension labelSizeV = new Dimension( defaultLabelHeight, visHeight + insetsV );

						cCols.setPreferredSize( new Dimension( ( visWidth + insetsH ) * visDimsH.length, defaultLabelHeight ) );
						cRows.setPreferredSize( new Dimension( defaultLabelHeight, ( visHeight + insetsV ) * visDimsV.length ) );

						for ( Component c : cCols.getComponents() ) {
							JLabel lbl = (JLabel)c;
							lbl.setMaximumSize( labelSizeH );
						}
						for ( Component c : cRows.getComponents() ) {
							JLabel lbl = (JLabel)c;
							lbl.setMaximumSize( labelSizeV );
						}

						cCols.revalidate();
						cRows.revalidate();
						scrollPane.revalidate();
						repaint();
					}

					// Source: http://stackoverflow.com/q/8351143
					else if ( e.isShiftDown() || !scrollPane.getVerticalScrollBar().isVisible() ) {
						// Horizontal scrolling
						Adjustable adj = scrollPane.getHorizontalScrollBar();
						int scroll = e.getUnitsToScroll() * adj.getBlockIncrement();
						adj.setValue( adj.getValue() + scroll );
					}
					else {
						// Vertical scrolling
						Adjustable adj = scrollPane.getVerticalScrollBar();
						int scroll = e.getUnitsToScroll() * adj.getBlockIncrement();
						adj.setValue( adj.getValue() + scroll );
					}
				}
			}
		);

		if ( context.isHierarchyDataLoaded() ) {
			recreateUI();
		}
	}


	/**
	 * Creates a properly configured, interactable display which can be used to show instance visualizations.
	 * 
	 * @param vis
	 *            the visualization to create the display for.
	 * @return the display
	 */
	private Display createInstanceDisplayFor( Visualization vis )
	{
		Display display = new Display( vis );
		display.setHighQuality( true );
		display.setBackground( context.getConfig().getBackgroundColor() );

		display.addControlListener( new PanControl( true ) );
		display.addControlListener( new ToolTipControl( HVConstants.PREFUSE_INSTANCE_LABEL_COLUMN_NAME ) );
		ZoomScrollControl zoomControl = new ZoomScrollControl();
		zoomControl.setModifierControl( true );
		display.addControlListener( zoomControl );

		display.addMouseWheelListener( new MouseWheelEventBubbler( display, e -> !e.isControlDown() ) );

		// display.setMaximumSize( new Dimension( visWidth, visHeight ) );
		display.setPreferredSize( new Dimension( visWidth, visHeight ) );
		display.setSize( visWidth, visHeight );

		Utils.unzoom( display, 0 );

		return display;
	}

	/**
	 * Creates an instance display for the specified dimensions and the specified node
	 * 
	 * @param node
	 *            node that is currently selected in the hierarchy view
	 * @param dimX
	 *            dimension number on the X axis (0 based)
	 * @param dimY
	 *            dimension number on the Y axis (0 based)
	 * @return the display
	 */
	private Display createInstanceDisplayFor( Node node, int dimX, int dimY )
	{
		Visualization vis = null;

		if ( dimX == dimY ) {
			// TODO: Histogram
			vis = new Visualization();
		}
		else {
			vis = HierarchyProcessor.createInstanceVisualization(
				context, node, pointSize, dimX, dimY, false
			);
		}

		Display display = createInstanceDisplayFor( vis );
		display.setAlignmentX( 0 );
		display.setAlignmentY( 0 );

		displayMap.put( ImmutablePair.of( dimX, dimY ), display );

		vis.run( "draw" );

		return display;
	}

	/**
	 * @param dim
	 *            the dimension number to return the index for (0 based)
	 * @param horizontal
	 *            whether to return index for horizontal or vertical axis
	 * @return index of the leftmost / topmost strut separating the compartment from others.
	 *         (add 1 to this in order to get the index of the display)
	 */
	private int dimToIndex( int dim, boolean horizontal )
	{
		JCheckBox[] arr = horizontal
			? cboxesHorizontal
			: cboxesVertical;

		int visiblePrevCount = 0;
		for ( int i = 0; i < dim; ++i ) {
			if ( arr[i].isSelected() )
				++visiblePrevCount;
		}

		return visiblePrevCount * 3;
	}

	/**
	 * @param dimY
	 *            the dimension number to return the holder for (0 based)
	 * @return the holder component for the specified Y dimension
	 */
	private JComponent getOrCreateDisplayHolder( int dimY )
	{
		JComponent holder = dimHolderMap.getOrDefault( dimY, null );

		if ( holder == null ) {
			holder = createDisplayHolder( dimY );

			int iy = dimToIndex( dimY, false ) + 1; // +1 to skip the first strut

			cViewport.add( Box.createVerticalStrut( 5 ), iy - 1 );
			cViewport.add( holder, iy );
			cViewport.add( Box.createVerticalStrut( 5 ), iy + 1 );

			dimHolderMap.put( dimY, holder );
		}

		return holder;
	}

	/**
	 * Sets the visibility of the display for the specified dimensions.
	 * 
	 * @param dimX
	 *            the dimensions number of X axis (0 based)
	 * @param dimY
	 *            the dimensions number of Y axis (0 based)
	 * @param vis
	 *            whether to make the dispay visible or not
	 */
	private void setDisplayVisible( int dimX, int dimY, boolean vis )
	{
		Display display = displayMap.get( ImmutablePair.of( dimX, dimY ) );

		if ( vis && !display.isVisible() ) {
			showDisplay( display, dimX, dimY );
		}
		else if ( !vis && display.isVisible() ) {
			hideDisplay( display, dimX, dimY );
		}
	}

	/**
	 * Inserts the specified display as the specified dimensions
	 * (automatically figures correct indices to physically insert the component at)
	 * 
	 * @param display
	 *            the display to insert
	 * @param dimX
	 *            the dimension number of X axis (0 based)
	 * @param dimY
	 *            the dimension number of Y axis (0 based)
	 */
	private void showDisplay( Display display, int dimX, int dimY )
	{
		JComponent holder = getOrCreateDisplayHolder( dimY );

		int ix = dimToIndex( dimX, true );

		holder.add( Box.createHorizontalStrut( 5 ), ix );
		holder.add( display, ix + 1 );
		holder.add( Box.createHorizontalStrut( 5 ), ix + 2 );

		display.setVisible( true );
	}

	/**
	 * Removes the specified display from the UI.
	 * If no displays are visible in the Y-th row, the holder is also removed.
	 * 
	 * @param display
	 *            the display to remove
	 * @param dimX
	 *            the dimension number of X axis (0 based).
	 *            Currently unused.
	 * @param dimY
	 *            the dimension number of Y axis (0 based).
	 *            Used to figure the number of children below which the holder should be removed.
	 */
	private void hideDisplay( Display display, int dimX, int dimY )
	{
		// TODO: Rework this method, could completely remove dimX/dimY args?

		Container holder = display.getParent();

		// -1 to get the index of the first strut
		int ix = SwingUIUtils.getComponentIndex( display ) - 1;
		int iy = SwingUIUtils.getComponentIndex( holder ) - 1;

		holder.remove( ix ); // left strut
		holder.remove( ix ); // display itself
		holder.remove( ix ); // right strut

		display.setVisible( false );

		holder.revalidate();

		int count = holder.getComponentCount();
		if ( count <= dimY * 3 ) {
			dimHolderMap.remove( dimY );
			cViewport.remove( iy ); // top strut
			cViewport.remove( iy ); // holder itself
			cViewport.remove( iy ); // bottom strut

			cViewport.revalidate();
		}
	}

	/**
	 * @param horizontal
	 *            if true, visibility will be determined by checking the horizontal checkboxes.
	 *            If false, vertical ones will be used instead.
	 * @return array of dimension numbers whose checkboxes are currently checked.
	 *         Ordered from left-to-right (if horizontal is true) / top-to-bottom (if false).
	 */
	private int[] getVisibleDimensions( boolean horizontal )
	{
		JCheckBox[] arr = horizontal
			? cboxesHorizontal
			: cboxesVertical;

		int count = (int)Arrays.stream( arr ).filter( cbox -> cbox.isSelected() ).count();
		int[] result = new int[count];

		int j = 0;
		for ( int i = 0; i < arr.length; ++i ) {
			if ( arr[i].isSelected() )
				result[j++] = i;
		}

		return result;
	}

	private void recreateUI()
	{
		String[] dataNames = HierarchyProcessor.getFeatureNames( context.getHierarchy() );

		recreateCheckboxes( dataNames );

		int[] visDimsH = getVisibleDimensions( true );
		int[] visDimsV = getVisibleDimensions( false );

		recreateLabels( dataNames, visDimsH, visDimsV );
	}

	private void recreateCheckboxes( String[] dataNames )
	{
		cDimsH.removeAll();
		cDimsV.removeAll();
		int dims = dataNames.length;

		cboxesHorizontal = new JCheckBox[dims];
		cboxesVertical = new JCheckBox[dims];

		for ( int i = 0; i < dims; ++i ) {
			JCheckBox cboxH = new JCheckBox( dataNames[i] );
			JCheckBox cboxV = new JCheckBox( dataNames[i] );

			cboxH.setSelected( false );
			cboxV.setSelected( false );

			final int d = i;
			cboxH.addActionListener( e -> dimensionVisibilityToggled.broadcast( ImmutablePair.of( d, true ) ) );
			cboxV.addActionListener( e -> dimensionVisibilityToggled.broadcast( ImmutablePair.of( d, false ) ) );

			cDimsH.add( cboxH );
			cDimsV.add( cboxV );

			cboxesHorizontal[i] = cboxH;
			cboxesVertical[i] = cboxV;
		}
	}

	private void recreateLabels( String[] dataNames, int[] visibleDimsH, int[] visibleDimsV )
	{
		cCols.removeAll();
		cRows.removeAll();

		int insetsH = displayInsets.left + displayInsets.right;
		int insetsV = displayInsets.top + displayInsets.bottom;
		Dimension labelSizeH = new Dimension( visWidth + insetsH, defaultLabelHeight );
		Dimension labelSizeV = new Dimension( defaultLabelHeight, visHeight + insetsV );

		cCols.setPreferredSize( new Dimension( ( visWidth + insetsH ) * visibleDimsH.length, defaultLabelHeight ) );
		cRows.setPreferredSize( new Dimension( defaultLabelHeight, ( visHeight + insetsV ) * visibleDimsV.length ) );

		for ( int i = 0; i < visibleDimsH.length; ++i ) {
			JLabel lbl = new JLabel( dataNames[visibleDimsH[i]] );
			lbl.setHorizontalAlignment( SwingConstants.CENTER );
			lbl.setMaximumSize( labelSizeH );
			cCols.add( lbl );
		}

		BasicLabelUI verticalUI = new VerticalLabelUI( false );
		for ( int i = 0; i < visibleDimsV.length; ++i ) {
			JLabel lbl = new JLabel( dataNames[visibleDimsV[i]] );
			lbl.setUI( verticalUI );
			lbl.setHorizontalAlignment( SwingConstants.CENTER );
			lbl.setMaximumSize( labelSizeV );
			cRows.add( lbl );
		}
	}

	private JComponent createDisplayHolder( int dimY )
	{
		JComponent result = Box.createHorizontalBox();
		result.setAlignmentX( 0 );
		result.setAlignmentY( 0 );

		for ( int x = 0; x < dimY; ++x ) {
			result.add( Box.createHorizontalStrut( 5 ) );
			Box box = Box.createHorizontalBox();
			box.add( Box.createHorizontalStrut( visWidth ) );
			box.add( Box.createHorizontalGlue() );
			result.add( box );
			result.add( Box.createHorizontalStrut( 5 ) );
		}

		return result;
	}

	public void updateFrameSize()
	{
		// TODO: Tweak this further so it behaves more reasonably / intuitively.

		pack();
		Dimension d = getPreferredSize();

		if ( d.width < visWidth || d.height < visHeight ) {
			d = new Dimension( visWidth, visHeight );
			setPreferredSize( d );
			setSize( d );
		}
		else {
			// https://stackoverflow.com/questions/3680221/how-can-i-get-screen-resolution-in-java
			GraphicsDevice gd = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
			int screenW = gd.getDisplayMode().getWidth();
			int screenH = gd.getDisplayMode().getHeight();

			if ( d.width > screenW || d.height > screenH ) {
				d = new Dimension( screenW / 2, screenH / 2 );
				setPreferredSize( d );
				setSize( d );

				setExtendedState( getExtendedState() | JFrame.MAXIMIZED_BOTH );
			}
		}
	}

	private boolean isDisplayVisible( int x, int y )
	{
		return cboxesHorizontal[x].isSelected() && cboxesVertical[y].isSelected();
	}

	// ----------------------------------------------------------------------------------------

	private void onDimensionVisibilityToggled( Pair<Integer, Boolean> args )
	{
		// TODO: Make this a config property?
		final boolean includeFlippedDims = false;

		// Unpack event arguments
		int dim = args.getLeft();
		boolean horizontal = args.getRight();

		Node node = context.findGroup( context.getSelectedRow() );

		for ( int i = 0; i < cboxesVertical.length; ++i ) {
			int x = horizontal ? dim : i;
			int y = horizontal ? i : dim;

			Display display = displayMap.get( ImmutablePair.of( x, y ) );
			boolean vis = isDisplayVisible( x, y );

			if ( display == null ) {
				if ( vis ) {
					if ( includeFlippedDims || ( !includeFlippedDims && x >= y ) ) {
						// Lazily create the requested display.
						display = createInstanceDisplayFor( node, x, y );
						showDisplay( display, x, y );
					}
				}
			}
			else {
				// If the display was previously hidden, redraw it.
				setDisplayVisible( x, y, vis );
				if ( vis ) {
					redrawDisplay( display );
				}
			}
		}

		int[] visDimsH = getVisibleDimensions( true );
		int[] visDimsV = getVisibleDimensions( false );
		String[] dataNames = HierarchyProcessor.getFeatureNames( context.getHierarchy() );
		recreateLabels( dataNames, visDimsH, visDimsV );

		revalidate();
		repaint();
	}

	private void onHierarchyChanging( Hierarchy h )
	{
		// Clear all visualizations
		displayMap.clear();
		dimHolderMap.clear();

		cDimsH.removeAll();
		cDimsV.removeAll();
		cCols.removeAll();
		cRows.removeAll();
		cViewport.removeAll();

		revalidate();
		repaint();
	}

	private void onHierarchyChanged( Hierarchy h )
	{
		recreateUI();

		revalidate();
		repaint();
	}

	private void onNodeSelectionChanged( int row )
	{
		for ( Entry<Pair<Integer, Integer>, Display> entry : displayMap.entrySet() ) {
			Display display = entry.getValue();

			// Don't redraw hidden displays.
			if ( display.isVisible() ) {
				redrawDisplay( display );
			}
		}
	}

	private static void redrawDisplay( Display d )
	{
		// Unzoom the display so that drawing is not botched.
		Utils.unzoom( d, 0 );
		d.getVisualization().run( "draw" );
	}
}
