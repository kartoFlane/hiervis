package pl.pwr.hiervis.ui;

import java.awt.Adjustable;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
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

	private static final Insets displayInsets = new Insets( 5, 5, 5, 5 );

	public final Event<Pair<Integer, Boolean>> dimensionVisibilityToggled = new Event<>();

	private HVContext context;

	private int visWidth = 200;
	private int visHeight = 200;

	// TODO: Move this to config, but make modifiable in the vis frame?
	private int pointSize = 3;

	private HashMap<Pair<Integer, Integer>, Display> displayMap;

	private JCheckBox[] cboxesHorizontal;
	private JCheckBox[] cboxesVertical;

	private JPanel cDimsH;
	private JPanel cDimsV;
	private JPanel cCols;
	private JPanel cRows;
	private JPanel cViewport;


	/*
	 * TODO
	 * - checkboxes need to wrap around when there's a large number of them, or scroll
	 * - visualizations *sometimes* have the wrong initial size; need to force max vis area
	 * - histograms
	 * - double click to open large frame to show only that vis in large mode
	 */

	public InstanceVisualizationsFrame( HVContext context, Frame owner )
	{
		super( "Instance Visualizations" );
		this.context = context;

		displayMap = new HashMap<>();

		setDefaultCloseOperation( HIDE_ON_CLOSE );

		createGUI();

		context.hierarchyChanging.addListener( this::onHierarchyChanging );
		context.hierarchyChanged.addListener( this::onHierarchyChanged );
		context.nodeSelectionChanged.addListener( this::onNodeSelectionChanged );
		dimensionVisibilityToggled.addListener( this::onDimensionVisibilityToggled );
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

	// ----------------------------------------------------------------------------------------
	// GUI creation methods

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
		scrollPane.setViewportView( cViewport );

		cCols = new JPanel();
		cRows = new JPanel();

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

						// Update the displays' preferred sizes so they can shrink to the new size
						for ( Display display : displayMap.values() ) {
							display.setPreferredSize( new Dimension( visWidth, visHeight ) );
						}

						updateViewportLayout();
						updateLabelLayout( true );
						updateLabelLayout( false );

						cCols.revalidate();
						cRows.revalidate();
						cViewport.revalidate();
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

	private void recreateUI()
	{
		String[] dataNames = HierarchyProcessor.getFeatureNames( context.getHierarchy() );

		recreateCheckboxes( dataNames );
		recreateLabels( dataNames );

		recreateViewportLayout( dataNames.length );
		updateViewportLayout();
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

	private void recreateLabels( String[] dataNames )
	{
		cCols.removeAll();
		cRows.removeAll();

		int dims = dataNames.length;

		cCols.setLayout( createLabelLayout( dims, true ) );
		cRows.setLayout( createLabelLayout( dims, false ) );

		Insets insetsH = new Insets( 0, 5, 0, 5 );
		Insets insetsV = new Insets( 5, 0, 5, 0 );

		BasicLabelUI verticalUI = new VerticalLabelUI( false );
		for ( int i = 0; i < dims; ++i ) {
			JLabel lblH = new JLabel( dataNames[i] );
			lblH.setHorizontalAlignment( SwingConstants.CENTER );
			lblH.setVisible( false );
			cCols.add( lblH, createGridConstraintsFor( i, 0, insetsH ) );

			JLabel lblV = new JLabel( dataNames[i] );
			lblV.setUI( verticalUI );
			lblV.setHorizontalAlignment( SwingConstants.CENTER );
			lblV.setVisible( false );
			cRows.add( lblV, createGridConstraintsFor( 0, i, insetsV ) );
		}

		updateLabelLayout( true );
		updateLabelLayout( false );
	}

	// ----------------------------------------------------------------------------------------
	// Helper methods for GUI creation

	/**
	 * Recreates the layout of the scrollpane viewoprt.
	 * 
	 * @param totalDims
	 *            total number of dimensions
	 */
	private void recreateViewportLayout( int totalDims )
	{
		GridBagLayout layout = new GridBagLayout();

		layout.columnWidths = new int[totalDims];
		layout.rowHeights = new int[totalDims];
		layout.columnWeights = new double[totalDims];
		layout.rowWeights = new double[totalDims];

		cViewport.setLayout( layout );
	}

	/**
	 * Updates the viewport layout so that the cells have the correct sizes
	 */
	private void updateViewportLayout()
	{
		GridBagLayout layout = (GridBagLayout)cViewport.getLayout();

		for ( int i = 0; i < cboxesHorizontal.length; ++i ) {
			boolean visH = cboxesHorizontal[i].isSelected();
			boolean visV = cboxesVertical[i].isSelected();

			layout.columnWidths[i] = visH ? visWidth : 0;
			layout.rowHeights[i] = visV ? visHeight : 0;
			layout.columnWeights[i] = visH ? 1.0 : Double.MIN_VALUE;
			layout.rowWeights[i] = visV ? 1.0 : Double.MIN_VALUE;
		}
	}

	/**
	 * Creates layout for a label holder.
	 * 
	 * @param totalDims
	 *            total number of dimensions
	 * @param horizontal
	 *            whether to update the horizontal or vertical labels
	 * @return the layout
	 */
	private GridBagLayout createLabelLayout( int totalDims, boolean horizontal )
	{
		GridBagLayout layout = new GridBagLayout();

		if ( horizontal ) {
			layout.columnWidths = new int[totalDims];
			layout.columnWeights = new double[totalDims];

			layout.rowHeights = new int[] { defaultLabelHeight };
			layout.rowWeights = new double[] { 0.0 };
		}
		else {
			layout.rowHeights = new int[totalDims];
			layout.rowWeights = new double[totalDims];

			layout.columnWidths = new int[] { defaultLabelHeight };
			layout.columnWeights = new double[] { 0.0 };
		}

		return layout;
	}

	/**
	 * Updates the label holder layout so that the cells have correct sizes.
	 * 
	 * @param horizontal
	 *            whether to update the horizontal or vertical labels
	 */
	private void updateLabelLayout( boolean horizontal )
	{
		JPanel panel = horizontal ? cCols : cRows;
		JCheckBox[] arr = horizontal ? cboxesHorizontal : cboxesVertical;

		GridBagLayout layout = (GridBagLayout)panel.getLayout();

		for ( int i = 0; i < arr.length; ++i ) {
			boolean vis = arr[i].isSelected();

			// Need to manually include insets in size calculation.
			// For some reason, the grid layout doesn't do it automatically...

			if ( horizontal ) {
				Insets insets = layout.getConstraints( cCols.getComponent( i ) ).insets;

				layout.columnWidths[i] = vis ? visWidth + insets.left + insets.right : 0;
				layout.columnWeights[i] = vis ? 1.0 : Double.MIN_VALUE;
			}
			else {
				Insets insets = layout.getConstraints( cRows.getComponent( i ) ).insets;

				layout.rowHeights[i] = vis ? visHeight + insets.top + insets.bottom : 0;
				layout.rowWeights[i] = vis ? 1.0 : Double.MIN_VALUE;
			}
		}
	}

	/**
	 * Creates a new constraint used with GridBagLayout, used to position an element correctly.
	 * 
	 * @param gridPosX
	 *            position of the item on the grid in X axis
	 * @param gridPosY
	 *            position of the item on the grid in Y axis
	 * @param insets
	 *            insets for the item, ie. the distance between this and bordering items
	 * @return the constraints
	 */
	private GridBagConstraints createGridConstraintsFor( int gridPosX, int gridPosY, Insets insets )
	{
		GridBagConstraints constraints = new GridBagConstraints();

		constraints.fill = GridBagConstraints.BOTH;
		constraints.insets = insets;
		constraints.gridx = gridPosX;
		constraints.gridy = gridPosY;

		return constraints;
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
	 * @return container serving as a holder for the display.
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
		displayMap.put( ImmutablePair.of( dimX, dimY ), display );

		cViewport.add( display, createGridConstraintsFor( dimX, dimY, displayInsets ) );

		vis.run( "draw" );

		return display;
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

		display.setPreferredSize( new Dimension( visWidth, visHeight ) );

		Utils.unzoom( display, 0 );

		return display;
	}

	/**
	 * @param x
	 *            dimension number on the X axis (0 based)
	 * @param y
	 *            dimension number on the Y axis (0 based)
	 * @return whether x-th horizontal and y-th vertical checkboxes are both selected,
	 *         indicating that the display at { x, y } should be visible.
	 */
	private boolean shouldDisplayBeVisible( int x, int y )
	{
		return cboxesHorizontal[x].isSelected() && cboxesVertical[y].isSelected();
	}

	private static void redrawDisplay( Display d )
	{
		// Unzoom the display so that drawing is not botched.
		Utils.unzoom( d, 0 );
		d.getVisualization().run( "draw" );
	}

	// ----------------------------------------------------------------------------------------
	// Listeners

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

			boolean vis = shouldDisplayBeVisible( x, y );
			Display display = displayMap.get( ImmutablePair.of( x, y ) );

			if ( display == null ) {
				if ( vis ) {
					if ( includeFlippedDims || ( !includeFlippedDims && x >= y ) ) {
						// Lazily create the requested display.
						display = createInstanceDisplayFor( node, x, y );
					}
				}
			}
			else {
				display.setVisible( vis );
				if ( vis ) {
					// If the display was previously hidden, redraw it.
					redrawDisplay( display );
				}
			}
		}

		Component c = horizontal ? cCols.getComponent( dim ) : cRows.getComponent( dim );
		c.setVisible( horizontal ? cboxesHorizontal[dim].isSelected() : cboxesVertical[dim].isSelected() );

		updateViewportLayout();
		updateLabelLayout( horizontal );

		cCols.revalidate();
		cRows.revalidate();
		cViewport.revalidate();
		repaint();
	}

	private void onHierarchyChanging( Hierarchy h )
	{
		// Clear all visualizations
		displayMap.clear();

		cDimsH.removeAll();
		cDimsV.removeAll();
		cCols.removeAll();
		cRows.removeAll();
		cViewport.removeAll();

		cDimsH.revalidate();
		cDimsV.revalidate();
		cCols.revalidate();
		cRows.revalidate();
		cViewport.revalidate();
		repaint();
	}

	private void onHierarchyChanged( Hierarchy h )
	{
		recreateUI();

		cDimsH.revalidate();
		cDimsV.revalidate();
		cCols.revalidate();
		cRows.revalidate();
		cViewport.revalidate();
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
}
