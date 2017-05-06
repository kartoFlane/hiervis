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
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JViewport;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.basic.BasicLabelUI;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pl.pwr.hiervis.core.HVConfig;
import pl.pwr.hiervis.core.HVConstants;
import pl.pwr.hiervis.core.HVContext;
import pl.pwr.hiervis.hierarchy.HierarchyProcessor;
import pl.pwr.hiervis.hierarchy.LoadedHierarchy;
import pl.pwr.hiervis.prefuse.DisplayEx;
import pl.pwr.hiervis.prefuse.control.CustomToolTipControl;
import pl.pwr.hiervis.prefuse.control.PanControl;
import pl.pwr.hiervis.prefuse.control.ZoomScrollControl;
import pl.pwr.hiervis.prefuse.histogram.HistogramGraph;
import pl.pwr.hiervis.prefuse.histogram.HistogramTable;
import pl.pwr.hiervis.util.Utils;
import pl.pwr.hiervis.util.ui.GridBagConstraintsBuilder;
import pl.pwr.hiervis.util.ui.MouseWheelEventBubbler;
import pl.pwr.hiervis.util.ui.VerticalLabelUI;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.data.Table;
import prefuse.visual.VisualItem;
import prefuse.visual.sort.ItemSorter;


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

	private int visSizeIncrement = 5;
	private int visWidth = 200;
	private int visHeight = 200;

	private JCheckBox[] cboxesHorizontal = new JCheckBox[0];
	private JCheckBox[] cboxesVertical = new JCheckBox[0];

	private JPanel cDimsH;
	private JPanel cDimsV;
	private JPanel cCols;
	private JPanel cRows;
	private JScrollPane scrollPane;
	private JPanel cViewport;
	private JScrollPane scrollPaneH;
	private JScrollPane scrollPaneV;
	private JCheckBox cboxAllH;
	private JCheckBox cboxAllV;
	private JPopupMenu displayPopupMenu;


	public InstanceVisualizationsFrame( HVContext context, Frame owner, String subtitle )
	{
		super( "Instance Visualizations" + ( subtitle == null ? "" : ( " [ " + subtitle + " ]" ) ) );
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

		VisualizerFrame.createFileDrop( this, log, "csv", file -> context.loadFile( this, file ) );
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

		displayPopupMenu = createDisplayPopupMenu();

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
			e -> Arrays.stream( cboxesHorizontal ).forEach( cbox -> cbox.setSelected( cboxAllH.isSelected() ) )
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
			e -> Arrays.stream( cboxesVertical ).forEach( cbox -> cbox.setSelected( cboxAllV.isSelected() ) )
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
		scrollPane = new JScrollPane();

		getContentPane().add(
			scrollPane,
			new GridBagConstraintsBuilder().position( 1, 1 ).fill().build()
		);

		cViewport = new JPanel();
		cViewport.setLayout( new GridBagLayout() );

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
					if ( e.isControlDown() && displaysVisible() ) {
						Display dis = getFirstVisibleDisplay();
						visWidth = Math.min( dis.getSize().width, dis.getSize().height );
						visHeight = visWidth;

						visWidth -= e.getWheelRotation() * visSizeIncrement;
						visHeight -= e.getWheelRotation() * visSizeIncrement;

						visWidth = Utils.clamp( visWidthMin, visWidth, visWidthMax );
						visHeight = Utils.clamp( visHeightMin, visHeight, visHeightMax );
						visSizeIncrement = getSizeIncrement( Math.min( visWidth, visHeight ) );

						// Update the components' preferred sizes so they can shrink to the new size
						Dimension d = new Dimension( visWidth, visHeight );
						for ( Component c : cViewport.getComponents() ) {
							c.setPreferredSize( d );
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
	}

	/**
	 * Returns new size increment based on the current size of a display, so that we don't have
	 * to scroll 10000 times when the displays are already large.
	 * 
	 * @param w
	 *            number to decide the new size increment on; usually width or height, whichever is higher
	 * @return the new size increment
	 */
	private int getSizeIncrement( int w )
	{
		return w <= 300 ? 5 : w <= 500 ? 10 : 20;
	}

	/**
	 * Recreates the UI, updating it to match the currently loaded hierarchy's data.
	 */
	private void recreateUI()
	{
		String[] dataNames = HierarchyProcessor.getFeatureNames( context.getHierarchy() );

		// Store the previous selections so that we can restore them for the new hierarchy.
		final boolean[] checkedH = new boolean[cboxesHorizontal.length];
		final boolean[] checkedV = new boolean[cboxesVertical.length];

		for ( int i = 0; i < checkedH.length; ++i ) {
			checkedH[i] = cboxesHorizontal[i].isSelected();
			checkedV[i] = cboxesVertical[i].isSelected();
		}

		recreateCheckboxes( dataNames );
		recreateLabels( dataNames );

		recreateViewportLayout( dataNames.length );
		updateViewportLayout();

		// Invoke this part later, so that it's executed after all UI-creating code is done running.
		SwingUtilities.invokeLater(
			() -> {
				for ( int i = 0; i < cboxesHorizontal.length; ++i ) {
					if ( cboxAllH.isSelected() ) {
						cboxesHorizontal[i].setSelected( true );
					}
					if ( cboxAllV.isSelected() ) {
						cboxesVertical[i].setSelected( true );
					}

					if ( ( !cboxAllH.isSelected() && !cboxAllV.isSelected() ) && i < checkedH.length ) {
						if ( checkedH[i] ) cboxesHorizontal[i].setSelected( true );
						if ( checkedV[i] ) cboxesVertical[i].setSelected( true );
					}
				}
			}
		);
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
			cboxH.addActionListener( e -> updateToggleAll( d, true ) );
			cboxV.addActionListener( e -> updateToggleAll( d, false ) );

			cDimsH.add( cboxH );
			if ( i + 1 < dims ) {
				// Add a 10-pixel strut to visually separate the neighboring checkboxes
				cDimsH.add( Box.createHorizontalStrut( 10 ) );
			}

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

	/**
	 * Creates a display for the specified dimensions and the specified node
	 * 
	 * @param dimX
	 *            index of the X dimension
	 * @param dimY
	 *            index of the Y dimension
	 * @return the display
	 */
	private DisplayEx createDisplayFor( int dimX, int dimY )
	{
		DisplayEx display = null;

		if ( dimX == dimY ) {
			display = createHistogramDisplayFor( dimX );
		}
		else {
			display = createInstanceDisplayFor( dimX, dimY );
		}

		GridBagConstraintsBuilder builder = new GridBagConstraintsBuilder();
		cViewport.add(
			display,
			builder.position( dimX, dimY ).insets( displayInsets ).fill().build()
		);

		return display;
	}

	private Component createLabelFor( int dimX, int dimY, String msg )
	{
		GridBagConstraintsBuilder builder = new GridBagConstraintsBuilder();

		JLabel lbl = new JLabel( msg );
		lbl.setHorizontalAlignment( SwingConstants.CENTER );
		lbl.setVerticalAlignment( SwingConstants.CENTER );
		lbl.setPreferredSize( new Dimension( visWidth, visHeight ) );

		cViewport.add(
			lbl,
			builder.position( dimX, dimY ).insets( displayInsets ).build()
		);

		return lbl;
	}

	/**
	 * Creates a histogram display for the specified dimension and the specified node
	 * 
	 * @param dim
	 *            dimension index
	 * @return the display
	 */
	private DisplayEx createHistogramDisplayFor( int dim )
	{
		Table table = context.getHierarchy().getInstanceTable();

		HistogramTable histoTable = new HistogramTable( table, context.getConfig().getNumberOfHistogramBins() );
		HistogramGraph display = new HistogramGraph(
			histoTable,
			table.getColumnName( dim ),
			context.getConfig().getHistogramColor()
		);

		display.setBackground( context.getConfig().getBackgroundColor() );
		display.setPreferredSize( new Dimension( visWidth, visHeight ) );

		display.addControlListener( new PanControl( true ) );
		ZoomScrollControl zoomControl = new ZoomScrollControl();
		zoomControl.setModifierControl( true );
		display.addControlListener( zoomControl );
		display.addMouseWheelListener( new MouseWheelEventBubbler( display, e -> !e.isControlDown() && !e.isAltDown() ) );
		display.addControlListener(
			new CustomToolTipControl(
				item -> {
					StringBuilder buf = new StringBuilder();

					buf.append( "<html>" );
					buf.append( "Count: " ).append( item.get( HVConstants.PREFUSE_VISUAL_TABLE_COLUMN_OFFSET + dim * 2 + 1 ) );
					// TODO: Add bin value range
					buf.append( "</html>" );

					return buf.toString();
				}
			)
		);

		display.addKeyListener(
			new KeyAdapter() {
				@Override
				public void keyReleased( KeyEvent e )
				{
					if ( e.getKeyCode() == KeyEvent.VK_ALT ) {
						// Consume alt key releases, so that the display doesn't lose focus
						// (default behaviour of Alt key on Windows is to switch to menu bar when Alt
						// is pressed, but this window has no menu bar anyway)
						e.consume();
					}
				}
			}
		);

		display.addComponentListener(
			new ComponentAdapter() {
				public void componentResized( ComponentEvent e )
				{
					redrawDisplayIfVisible( (DisplayEx)e.getComponent() );
				}
			}
		);

		return display;
	}

	/**
	 * Creates an instance display for the specified dimensions and the specified node
	 * 
	 * @param dimX
	 *            index of the X dimension
	 * @param dimY
	 *            index of the Y dimension
	 * @return the display
	 */
	private DisplayEx createInstanceDisplayFor( int dimX, int dimY )
	{
		Visualization vis = createInstanceVisualizationFor( dimX, dimY );
		DisplayEx display = createInstanceDisplayFor( vis, dimX, dimY );

		return display;
	}

	private Visualization createInstanceVisualizationFor( int dimX, int dimY )
	{
		return HierarchyProcessor.createInstanceVisualization(
			context, context.getConfig().getPointSize(), dimX, dimY, true
		);
	}

	/**
	 * Creates a properly configured, interactable display which can be used to show instance visualizations.
	 * 
	 * @param vis
	 *            the visualization to create the display for.
	 * @param dimX
	 *            index of the X dimension
	 * @param dimY
	 *            index of the Y dimension
	 * @return the display
	 */
	private DisplayEx createInstanceDisplayFor( Visualization vis, int dimX, int dimY )
	{
		DisplayEx display = new DisplayEx( vis );
		display.setHighQuality( context.getHierarchy().data.getOverallNumberOfInstances() < HVConstants.INSTANCE_COUNT_MED );
		display.setBackground( context.getConfig().getBackgroundColor() );
		display.setPreferredSize( new Dimension( visWidth, visHeight ) );

		// Via: http://www.ifs.tuwien.ac.at/~rind/w/doku.php/java/prefuse-scatterplot
		display.setItemSorter(
			new ItemSorter() {
				public int score( VisualItem item )
				{
					if ( item.isInGroup( HVConstants.INSTANCE_DATA_NAME ) ) {
						prefuse.data.Node node = (prefuse.data.Node)item.get( HVConstants.PREFUSE_INSTANCE_NODE_COLUMN_NAME );
						int roleId = node.getInt( HVConstants.PREFUSE_NODE_ROLE_COLUMN_NAME );

						// Sort the nodes so that instances belonging to the currently selected node are always topmost.
						// Direct parents next, then indirect parents, then children, then other unrelated instances.
						return Integer.MAX_VALUE - roleId;
					}

					return 0;
				}
			}
		);

		display.addControlListener( new PanControl( true ) );
		ZoomScrollControl zoomControl = new ZoomScrollControl();
		zoomControl.setModifierControl( true );
		display.addControlListener( zoomControl );
		display.addMouseWheelListener( new MouseWheelEventBubbler( display, e -> !e.isControlDown() && !e.isAltDown() ) );
		display.addControlListener(
			new CustomToolTipControl(
				item -> {
					if ( item.isInGroup( HVConstants.INSTANCE_DATA_NAME ) ) {
						StringBuilder buf = new StringBuilder();

						buf.append( "<html>" );
						if ( item.canGetString( HVConstants.PREFUSE_INSTANCE_LABEL_COLUMN_NAME ) ) {
							buf.append( "<b>" )
								.append( item.getString( HVConstants.PREFUSE_INSTANCE_LABEL_COLUMN_NAME ) )
								.append( "</b>" ).append( "<br/>" );
						}

						prefuse.data.Node node = (prefuse.data.Node)item.get( HVConstants.PREFUSE_INSTANCE_NODE_COLUMN_NAME );
						String assignId = node.getString( HVConstants.PREFUSE_NODE_ID_COLUMN_NAME );
						buf.append( "Assign class: " ).append( assignId ).append( "<br/>" );

						if ( item.canGetString( HVConstants.PREFUSE_INSTANCE_TRUENODE_ID_COLUMN_NAME ) ) {
							String trueId = item.getString( HVConstants.PREFUSE_INSTANCE_TRUENODE_ID_COLUMN_NAME );
							buf.append( "True class: " ).append( trueId ).append( "<br/>" );
						}

						String x = cboxesHorizontal[dimX].getText();
						String y = cboxesHorizontal[dimY].getText();
						buf.append( x ).append( ": " )
							.append( item.getDouble( HVConstants.PREFUSE_VISUAL_TABLE_COLUMN_OFFSET + dimX ) ).append( "<br/>" );
						buf.append( y ).append( ": " )
							.append( item.getDouble( HVConstants.PREFUSE_VISUAL_TABLE_COLUMN_OFFSET + dimY ) );

						buf.append( "</html>" );

						return buf.toString();
					}

					return null;
				}
			)
		);

		display.addMouseWheelListener(
			new MouseWheelListener() {
				@Override
				public void mouseWheelMoved( MouseWheelEvent e )
				{
					if ( e.isAltDown() ) {
						DisplayEx d = (DisplayEx)e.getComponent();

						Rectangle2D layoutBounds = HierarchyProcessor.getLayoutBounds( d.getVisualization() );

						// Scale the current layout area by 10%
						double zoomDelta = 1 - 0.1 * e.getWheelRotation();
						double newW = layoutBounds.getWidth() * zoomDelta;
						double newH = layoutBounds.getHeight() * zoomDelta;

						layoutBounds.setFrame( 0, 0, newW, newH );

						if ( layoutBounds != null ) {
							AffineTransform transform = d.getTransform();
							AffineTransform transformI = d.getInverseTransform();

							Point2D focusOld = transformI.transform( e.getPoint(), new Point2D.Double() );
							Point2D focusNew = new Point2D.Double( focusOld.getX() * zoomDelta, focusOld.getY() * zoomDelta );
							Point2D focusDelta = new Point2D.Double( focusNew.getX() - focusOld.getX(), focusNew.getY() - focusOld.getY() );

							transform.translate( -focusDelta.getX(), -focusDelta.getY() );
							Utils.setTransform( d, transform );
							HierarchyProcessor.updateLayoutBounds( d.getVisualization(), layoutBounds );

							redrawDisplayIfVisible( d );
						}
					}
				}
			}
		);

		display.addKeyListener(
			new KeyAdapter() {
				@Override
				public void keyReleased( KeyEvent e )
				{
					if ( e.getKeyCode() == KeyEvent.VK_ALT ) {
						// Consume alt key releases, so that the display doesn't lose focus
						// (default behaviour of Alt key on Windows is to switch to menu bar when Alt
						// is pressed, but this window has no menu bar anyway)
						e.consume();
					}
				}
			}
		);

		display.addComponentListener(
			new ComponentAdapter() {
				public void componentResized( ComponentEvent e )
				{
					redrawDisplayIfVisible( (DisplayEx)e.getComponent() );
				}
			}
		);

		display.setComponentPopupMenu( displayPopupMenu );

		Utils.unzoom( display, 0 );

		return display;
	}

	private JPopupMenu createDisplayPopupMenu()
	{
		JPopupMenu popup = new JPopupMenu();

		JMenuItem mntmUnzoom = new JMenuItem( "Unzoom" );
		mntmUnzoom.addActionListener(
			e -> {
				JMenuItem tm = (JMenuItem)e.getSource();
				JPopupMenu p = (JPopupMenu)tm.getParent();
				Display d = (Display)p.getInvoker();
				Utils.fitToBounds( d, Visualization.ALL_ITEMS, 0, 500 );
			}
		);
		popup.add( mntmUnzoom );

		JMenuItem mntmReset = new JMenuItem( "Reset visualization" );
		mntmReset.addActionListener(
			e -> {
				JMenuItem tm = (JMenuItem)e.getSource();
				JPopupMenu p = (JPopupMenu)tm.getParent();
				DisplayEx d = (DisplayEx)p.getInvoker();

				Utils.unzoom( d, 0 );
				HierarchyProcessor.updateLayoutBounds( d.getVisualization(), null );
				redrawDisplayIfVisible( d );
			}
		);
		popup.add( mntmReset );

		return popup;
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
	 * @return the component associated with the specified dimensions, or null if it wasn't created yet.
	 */
	private Component getComponent( int dimX, int dimY )
	{
		GridBagLayout layout = (GridBagLayout)cViewport.getLayout();

		for ( Component c : cViewport.getComponents() ) {
			GridBagConstraints gbc = layout.getConstraints( c );
			if ( gbc.gridx == dimX && gbc.gridy == dimY ) {
				return c;
			}
		}

		return null;
	}

	/**
	 * @return the first visible instance display, or null if none are visible
	 */
	private DisplayEx getFirstVisibleDisplay()
	{
		for ( Component c : cViewport.getComponents() ) {
			if ( c.isVisible() && c instanceof DisplayEx ) {
				return (DisplayEx)c;
			}
		}

		return null;
	}

	private boolean componentsVisible()
	{
		return cViewport.getComponents().length > 0;
	}

	/**
	 * @return true if any instance display is visible, false otherwise.
	 */
	private boolean displaysVisible()
	{
		return Arrays.stream( cViewport.getComponents() )
			.filter( c -> c.isVisible() && c instanceof DisplayEx )
			.count() > 0;
	}

	/**
	 * Executes the specified function for each existing display in the grid.
	 */
	private void forEachDisplay( Consumer<DisplayEx> func )
	{
		for ( Component c : cViewport.getComponents() ) {
			if ( c instanceof DisplayEx ) {
				func.accept( (DisplayEx)c );
			}
		}
	}

	private void redrawDisplayIfVisible( DisplayEx d )
	{
		if ( d.isVisible() ) {
			// Unzoom the display so that drawing is not botched.
			// Utils.unzoom( d, 0 );
			d.getVisualization().run( "draw" );
		}
		else {
			d.reset();
		}
	}

	/**
	 * Updates the toggle-all checkbox state, so that it correctly illustrates
	 * the selection states of its member checkboxes.
	 * 
	 * @param dim
	 *            the index of the checkbox whose selection state changed
	 * @param horizontal
	 *            whether the checkbox was horizontal or vertical
	 */
	private void updateToggleAll( int dim, boolean horizontal )
	{
		JCheckBox[] arr = horizontal ? cboxesHorizontal : cboxesVertical;
		JCheckBox cboxToggle = horizontal ? cboxAllH : cboxAllV;

		if ( cboxToggle.isSelected() ) {
			// We were selected, and one dim checkbox was toggled -> need to deselect
			setCheckboxSilent( cboxToggle, false );
		}
		else {
			// We were not selected, and one dim checkbox was toggled
			// -> need to query all checkboxes to see if they're selected
			setCheckboxSilent(
				cboxToggle,
				Arrays.stream( arr ).allMatch( cbox -> cbox.isSelected() )
			);
		}
	}

	/**
	 * Updates the state of the visualization viewport, creating visualizations or making
	 * them visible depending on whether the dimension that corresponds to them is currently selected or not.
	 * 
	 * @param dimX
	 *            Index of the horizontal dimension to update.
	 *            Can be negative to update all horizontal dimensions.
	 * @param dimY
	 *            Index of the vertical dimension to update.
	 *            Can be negative to update all vertical dimensions.
	 */
	private void updateDimensionVisibility( int dimX, int dimY )
	{
		for ( int y = 0; y < cboxesVertical.length; ++y ) {
			if ( dimY >= 0 && dimY != y )
				continue;
			for ( int x = 0; x < cboxesHorizontal.length; ++x ) {
				if ( dimX >= 0 && dimX != x )
					continue;

				boolean vis = shouldDisplayBeVisible( x, y );
				Component c = getComponent( x, y );

				if ( c != null ) {
					c.setVisible( vis );
					if ( c instanceof DisplayEx )
						redrawDisplayIfVisible( (DisplayEx)c );
				}
				else if ( vis ) {
					if ( x >= y ) {
						// Lazily create the requested display.
						createDisplayFor( x, y );
					}
					else {
						createLabelFor(
							x, y,
							"<html>Visualizations are created only for the upper half of the matrix.</html>"
						);
					}
				}
			}
		}

		updateViewportLayout();

		cCols.revalidate();
		cRows.revalidate();
		revalidate();
		repaint();
	}

	/**
	 * Sets the specified checkbox's value to the specified boolean value without notifying listeners.
	 * 
	 * @param cbox
	 *            the checkbox to set
	 * @param newValue
	 *            the new selection state of the checkbox
	 */
	private void setCheckboxSilent( JCheckBox cbox, boolean newValue )
	{
		ItemListener l = cbox.getItemListeners()[0];
		cbox.removeItemListener( l );
		cbox.setSelected( newValue );
		cbox.addItemListener( l );
	}

	/**
	 * Disposes all {@link Display}s currently in the frame.
	 * 
	 * Calling this method while the displays / visualizations are still being
	 * laid out will cause a plethora of errors to be printed out in the console,
	 * but it should be possible to safely ignore them (the displays those errors
	 * complain about have been disposed, and are no longer used)
	 */
	public void disposeDisplays()
	{
		forEachDisplay(
			display -> {
				if ( display instanceof HistogramGraph )
					HierarchyProcessor.disposeHistogramVis( display.getVisualization() );
				else
					HierarchyProcessor.disposeInstanceVis( display.getVisualization() );
				display.reset();
				display.dispose();
			}
		);
	}

	// ----------------------------------------------------------------------------------------
	// Listeners

	private void onDimensionVisibilityToggled( Pair<Integer, Boolean> args )
	{
		// Unpack event arguments
		int dim = args.getLeft();
		boolean horizontal = args.getRight();

		int dimX = horizontal ? dim : -1;
		int dimY = horizontal ? -1 : dim;

		updateDimensionVisibility( dimX, dimY );

		// Update dimension label visibility & layout
		Component c = horizontal ? cCols.getComponent( dim ) : cRows.getComponent( dim );
		c.setVisible( horizontal ? cboxesHorizontal[dim].isSelected() : cboxesVertical[dim].isSelected() );
		updateLabelLayout( horizontal );
	}

	private void onHierarchyChanging( LoadedHierarchy h )
	{
		cboxAllH.setEnabled( false );
		cboxAllV.setEnabled( false );

		cDimsH.removeAll();
		cDimsV.removeAll();
		cCols.removeAll();
		cRows.removeAll();

		disposeDisplays();
		cViewport.removeAll();

		cDimsH.revalidate();
		cDimsV.revalidate();
		cCols.revalidate();
		cRows.revalidate();
		cViewport.revalidate();
		repaint();
	}

	private void onHierarchyChanged( LoadedHierarchy h )
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
		if ( !context.isHierarchyDataLoaded() )
			return;

		forEachDisplay(
			display -> {
				GridBagLayout layout = (GridBagLayout)cViewport.getLayout();
				GridBagConstraints gbc = layout.getConstraints( display );
				int dimX = gbc.gridx;
				int dimY = gbc.gridy;

				if ( dimX != dimY ) {
					// Nothing, color is updated by InstanceColorAction when redrawing
				}
				else {
					HistogramGraph histogram = (HistogramGraph)display;
					histogram.setBarColor( cfg.getHistogramColor() );
				}

				display.setBackground( cfg.getBackgroundColor() );

				redrawDisplayIfVisible( display );
			}
		);
	}
}
