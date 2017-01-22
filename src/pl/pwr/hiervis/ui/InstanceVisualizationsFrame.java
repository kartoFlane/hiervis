package pl.pwr.hiervis.ui;

import java.awt.Adjustable;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.Arrays;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicLabelUI;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import basic_hierarchy.interfaces.Hierarchy;
import basic_hierarchy.interfaces.Node;
import pl.pwr.hiervis.core.HVConfig;
import pl.pwr.hiervis.core.HVConstants;
import pl.pwr.hiervis.core.HVContext;
import pl.pwr.hiervis.ui.components.MouseWheelEventBubbler;
import pl.pwr.hiervis.ui.components.VerticalLabelUI;
import pl.pwr.hiervis.ui.control.PanControl;
import pl.pwr.hiervis.ui.control.ZoomScrollControl;
import pl.pwr.hiervis.util.GridBagConstraintsBuilder;
import pl.pwr.hiervis.util.Utils;
import pl.pwr.hiervis.visualisation.HierarchyProcessor;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.controls.ToolTipControl;


@SuppressWarnings("serial")
public class InstanceVisualizationsFrame extends JFrame
{
	private static final Logger log = LogManager.getLogger( InstanceVisualizationsFrame.class );

	private static final int defaultFrameWidth = 800;
	private static final int defaultFrameHeight = 800;

	private static final int defaultLabelHeight = new JLabel( " " ).getPreferredSize().height;
	private static final int visWidthMin = 100;
	private static final int visWidthMax = 1000;
	private static final int visHeightMin = 100;
	private static final int visHeightMax = 1000;

	private static final Insets displayInsets = new Insets( 5, 5, 5, 5 );

	private HVContext context;

	private int visZoomIncrement = 5;
	private int visWidth = 200;
	private int visHeight = 200;

	// TODO: Move this to config, but make modifiable in the vis frame?
	private int pointSize = 3;

	private JCheckBox[] cboxesHorizontal;
	private JCheckBox[] cboxesVertical;

	private JPanel cDimsH;
	private JPanel cDimsV;
	private JPanel cCols;
	private JPanel cRows;
	private JPanel cViewport;
	private JScrollPane scrollPaneH;
	private JScrollPane scrollPaneV;
	private JCheckBox cboxAllH;
	private JCheckBox cboxAllV;


	public InstanceVisualizationsFrame( HVContext context, Frame owner )
	{
		super( "Instance Visualizations" );
		this.context = context;

		setDefaultCloseOperation( HIDE_ON_CLOSE );
		setSize( defaultFrameWidth, defaultFrameHeight );

		createGUI();

		context.hierarchyChanging.addListener( this::onHierarchyChanging );
		context.hierarchyChanged.addListener( this::onHierarchyChanged );
		context.nodeSelectionChanged.addListener( this::onNodeSelectionChanged );
		context.configChanged.addListener( this::onConfigChanged );

		if ( context.isHierarchyDataLoaded() ) {
			recreateUI();
		}
	}

	// ----------------------------------------------------------------------------------------
	// GUI creation methods

	private void createGUI()
	{
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 1.0 };
		gridBagLayout.rowWeights = new double[] { 0.0, 1.0 };
		getContentPane().setLayout( gridBagLayout );

		createCheckboxHolders();
		createVisualizationHolder();
	}

	private void createCheckboxHolders()
	{
		GridBagConstraintsBuilder builder = new GridBagConstraintsBuilder();

		// Horizontal
		JPanel cHorizontal = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		layout.columnWidths = new int[] { 0, 0, 0 };
		layout.rowHeights = new int[] { 0 };
		layout.columnWeights = new double[] { 0.0, 0.0, 1.0 };
		layout.rowWeights = new double[] { 1.0 };
		cHorizontal.setLayout( layout );

		getContentPane().add( cHorizontal, builder.position( 1, 0 ).fillHorizontal().build() );

		cboxAllH = new JCheckBox( "Toggle All" );
		cboxAllH.setEnabled( context.isHierarchyDataLoaded() );
		cboxAllH.addItemListener(
			e -> {
				for ( JCheckBox cbox : cboxesHorizontal )
					cbox.setSelected( !cbox.isSelected() );
			}
		);

		cHorizontal.add(
			cboxAllH,
			builder.position( 0, 0 ).anchorWest().build()
		);
		cHorizontal.add(
			new JSeparator( SwingConstants.VERTICAL ),
			builder.position( 1, 0 ).fillVertical().build()
		);

		cDimsH = new JPanel();
		cDimsH.setLayout( new BoxLayout( cDimsH, BoxLayout.X_AXIS ) );

		scrollPaneH = new JScrollPane( cDimsH );
		scrollPaneH.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER );
		scrollPaneH.setBorder( BorderFactory.createEmptyBorder() );
		scrollPaneH.getHorizontalScrollBar().setUnitIncrement( 16 );

		cHorizontal.add(
			scrollPaneH,
			builder.position( 2, 0 ).anchorWest().fill().build()
		);

		// Vertical
		JPanel cVertical = new JPanel();
		layout = new GridBagLayout();
		layout.columnWidths = new int[] { 0 };
		layout.rowHeights = new int[] { 0, 0, 0 };
		layout.columnWeights = new double[] { 1.0 };
		layout.rowWeights = new double[] { 0.0, 0.0, 1.0 };
		cVertical.setLayout( layout );

		getContentPane().add( cVertical, builder.position( 0, 1 ).fillVertical().build() );

		cboxAllV = new JCheckBox( "Toggle All" );
		cboxAllV.setEnabled( context.isHierarchyDataLoaded() );
		cboxAllV.addItemListener(
			e -> {
				for ( JCheckBox cbox : cboxesVertical )
					cbox.setSelected( !cbox.isSelected() );
			}
		);

		cVertical.add(
			cboxAllV,
			builder.position( 0, 0 ).anchorNorth().build()
		);
		cVertical.add(
			new JSeparator( SwingConstants.HORIZONTAL ),
			builder.position( 0, 1 ).fillHorizontal().build()
		);

		cDimsV = new JPanel();
		cDimsV.setLayout( new BoxLayout( cDimsV, BoxLayout.Y_AXIS ) );

		scrollPaneV = new JScrollPane( cDimsV );
		scrollPaneV.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
		scrollPaneV.setBorder( BorderFactory.createEmptyBorder() );
		scrollPaneV.getVerticalScrollBar().setUnitIncrement( 16 );

		cVertical.add(
			scrollPaneV,
			builder.position( 0, 2 ).anchorNorth().fill().build()
		);
	}

	private void createVisualizationHolder()
	{
		JScrollPane scrollPane = new JScrollPane();

		getContentPane().add(
			scrollPane,
			new GridBagConstraintsBuilder().position( 1, 1 ).fill().build()
		);

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

						// Update the zoom increment so that we don't have to scroll 10000 times when
						// the displays are already large.
						visZoomIncrement = getZoomIncrement( Math.max( visWidth, visHeight ) );

						// Update the displays' preferred sizes so they can shrink to the new size
						Dimension d = new Dimension( visWidth, visHeight );
						forEachDisplay( display -> display.setPreferredSize( d ) );

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
	}

	private int getZoomIncrement( int w )
	{
		return w <= 300 ? 5 : w <= 500 ? 10 : 20;
	}

	/**
	 * Recreates the UI, updating it to match the currently loaded hierarchy's data.
	 */
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
			cboxH.addItemListener( e -> onDimensionVisibilityToggled( ImmutablePair.of( d, true ) ) );
			cboxV.addItemListener( e -> onDimensionVisibilityToggled( ImmutablePair.of( d, false ) ) );

			cDimsH.add( cboxH );
			cDimsV.add( cboxV );

			cboxesHorizontal[i] = cboxH;
			cboxesVertical[i] = cboxV;
		}

		updateCheckboxViewport();

		revalidate();
		repaint();
	}

	private void updateCheckboxViewport()
	{
		// Find the widest checkbox's width
		int w = Arrays.stream( cboxesVertical ).mapToInt( cbox -> cbox.getPreferredSize().width ).max().getAsInt();
		int h = cboxesHorizontal[0].getPreferredSize().height;

		w += scrollPaneV.getVerticalScrollBar().getPreferredSize().width;
		h += scrollPaneH.getHorizontalScrollBar().getPreferredSize().height;

		scrollPaneH.setPreferredSize( new Dimension( 0, h ) );
		scrollPaneH.setMinimumSize( new Dimension( 0, h ) );
		scrollPaneV.setPreferredSize( new Dimension( w, 0 ) );
		scrollPaneV.setMinimumSize( new Dimension( w, 0 ) );
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

		GridBagConstraintsBuilder builder = new GridBagConstraintsBuilder();

		BasicLabelUI verticalUI = new VerticalLabelUI( false );
		for ( int i = 0; i < dims; ++i ) {
			JLabel lblH = new JLabel( dataNames[i] );
			lblH.setHorizontalAlignment( SwingConstants.CENTER );
			lblH.setVisible( false );
			cCols.add( lblH, builder.position( i, 0 ).insets( insetsH ).build() );

			JLabel lblV = new JLabel( dataNames[i] );
			lblV.setUI( verticalUI );
			lblV.setHorizontalAlignment( SwingConstants.CENTER );
			lblV.setVisible( false );
			cRows.add( lblV, builder.position( 0, i ).insets( insetsV ).build() );
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

	private Visualization createVisualizationFor( Node node, int dimX, int dimY )
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

		return vis;
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
		Visualization vis = createVisualizationFor( node, dimX, dimY );
		Display display = createInstanceDisplayFor( vis );

		GridBagConstraintsBuilder builder = new GridBagConstraintsBuilder();
		cViewport.add(
			display,
			builder.position( dimX, dimY ).insets( displayInsets ).fill().build()
		);

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
		display.setHighQuality( context.getHierarchy().getOverallNumberOfInstances() < HVConstants.INSTANCE_COUNT_MED );
		display.setBackground( context.getConfig().getBackgroundColor() );

		display.addControlListener( new PanControl( true ) );
		display.addControlListener( new ToolTipControl( HVConstants.PREFUSE_INSTANCE_LABEL_COLUMN_NAME ) );
		ZoomScrollControl zoomControl = new ZoomScrollControl();
		zoomControl.setModifierControl( true );
		display.addControlListener( zoomControl );

		display.addMouseWheelListener( new MouseWheelEventBubbler( display, e -> !e.isControlDown() ) );

		display.setPreferredSize( new Dimension( visWidth, visHeight ) );

		display.addComponentListener(
			new ComponentAdapter() {
				public void componentResized( ComponentEvent e )
				{
					redrawDisplayIfVisible( display );
				}
			}
		);

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

	/**
	 * @param dimX
	 *            dimension number on the X axis (0 based)
	 * @param dimY
	 *            dimension number on the Y axis (0 based)
	 * @return the display associated with the specified dimensions, or null if it wasn't created yet.
	 */
	private Display getDisplay( int dimX, int dimY )
	{
		GridBagLayout layout = (GridBagLayout)cViewport.getLayout();
		Component result = null;

		for ( Component c : cViewport.getComponents() ) {
			GridBagConstraints gbc = layout.getConstraints( c );
			if ( gbc.gridx == dimX && gbc.gridy == dimY ) {
				result = c;
				break;
			}
		}

		return (Display)result;
	}

	/**
	 * Executes the specified function for each existing display in the grid.
	 */
	private void forEachDisplay( Consumer<Display> func )
	{
		for ( Component c : cViewport.getComponents() ) {
			func.accept( (Display)c );
		}
	}

	private void redrawDisplayIfVisible( Display d )
	{
		if ( d.isVisible() ) {
			// Unzoom the display so that drawing is not botched.
			Utils.unzoom( d, 0 );
			d.getVisualization().run( "draw" );
		}
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
			Display display = getDisplay( x, y );

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
				redrawDisplayIfVisible( display );
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
		cboxAllH.setEnabled( false );
		cboxAllV.setEnabled( false );

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

		cboxAllH.setEnabled( true );
		cboxAllV.setEnabled( true );

		cDimsH.revalidate();
		cDimsV.revalidate();
		cCols.revalidate();
		cRows.revalidate();
		cViewport.revalidate();
		repaint();
	}

	private void onNodeSelectionChanged( int row )
	{
		forEachDisplay( this::redrawDisplayIfVisible );
	}

	private void onConfigChanged( HVConfig cfg )
	{
		GridBagLayout layout = (GridBagLayout)cViewport.getLayout();
		Node node = context.findGroup( context.getSelectedRow() );

		forEachDisplay(
			display -> {
				GridBagConstraints gbc = layout.getConstraints( display );
				int dimX = gbc.gridx;
				int dimY = gbc.gridy;

				display.setVisualization( createVisualizationFor( node, dimX, dimY ) );
				display.setBackground( cfg.getBackgroundColor() );

				redrawDisplayIfVisible( display );
			}
		);
	}
}
