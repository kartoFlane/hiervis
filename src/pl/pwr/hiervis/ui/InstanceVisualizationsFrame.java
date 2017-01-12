package pl.pwr.hiervis.ui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
	public final Event<Pair<Integer, Boolean>> dimensionVisibilityToggled = new Event<>();

	private HVContext context;

	// TODO: Move this to config, but make modifiable in the vis frame
	private int visWidth = 200;
	private int visHeight = 200;
	private int pointSize = 3;
	private Insets displayInsets = new Insets( 5, 5, 5, 5 );

	private HashMap<Pair<Integer, Integer>, Display> displayMap;
	private HashMap<Pair<Integer, Integer>, Boolean> visibilityMap;

	private JPanel cDimsH;
	private JPanel cDimsV;
	private JPanel cCols;
	private JPanel cRows;
	private JPanel cViewport;


	public InstanceVisualizationsFrame( HVContext context, Frame owner )
	{
		super( "Instance Visualizations" );
		this.context = context;

		displayMap = new HashMap<>();
		visibilityMap = new HashMap<>();

		setDefaultCloseOperation( HIDE_ON_CLOSE );

		createGUI();

		context.hierarchyChanging.addListener( this::onHierarchyChanging );
		context.hierarchyChanged.addListener( this::onHierarchyChanged );
		context.nodeSelectionChanged.addListener( this::onNodeSelectionChanged );
		dimensionVisibilityToggled.addListener( this::onDimensionVisibilityToggled );
		SwingUIUtils.addCloseCallback( this, this::onWindowClosing );
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

		if ( context.isHierarchyDataLoaded() ) {
			createDisplays();
		}
	}

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

		display.setSize( visWidth, visHeight );
		display.setPreferredSize( new Dimension( visWidth, visHeight ) );
		Utils.unzoom( display, 0 );

		return display;
	}

	private void createInstanceDisplayFor( Node node, int x, int y )
	{
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = displayInsets;
		constraints.gridx = x;
		constraints.gridy = y;

		Visualization vis = HierarchyProcessor.createInstanceVisualization(
			context, node, pointSize, x, y, false
		);
		Display display = createInstanceDisplayFor( vis );

		displayMap.put( ImmutablePair.of( x, y ), display );
		visibilityMap.put( ImmutablePair.of( x, y ), true );

		cViewport.add( display, constraints );

		vis.run( "draw" );
	}

	private void createDisplays()
	{
		// TODO: Make this a config property?
		final boolean includeFlippedDims = true;

		String[] dataNames = HierarchyProcessor.getFeatureNames( context.getHierarchy() );
		int dims = dataNames.length;

		int h = new JLabel( " " ).getPreferredSize().height;
		int insetsH = displayInsets.left + displayInsets.right;
		int insetsV = displayInsets.top + displayInsets.bottom;
		Dimension labelSizeH = new Dimension( visWidth + insetsH, h );
		Dimension labelSizeV = new Dimension( h, visHeight + insetsV );

		cCols.setPreferredSize( new Dimension( ( visWidth + insetsH ) * dims, h ) );
		cRows.setPreferredSize( new Dimension( h, ( visHeight + insetsV ) * dims ) );

		BasicLabelUI verticalUI = new VerticalLabelUI( false );
		for ( int i = 0; i < dims; ++i ) {
			String dimName = dataNames[i];

			JCheckBox cboxH = new JCheckBox( dimName );
			cboxH.setSelected( false );
			JCheckBox cboxV = new JCheckBox( dimName );
			cboxV.setSelected( false );

			final int d = i;
			cboxH.addActionListener( e -> setDimensionVisibility( d, true, cboxH.isSelected() ) );
			cboxV.addActionListener( e -> setDimensionVisibility( d, false, cboxV.isSelected() ) );

			cDimsH.add( cboxH );
			cDimsV.add( cboxV );

			JLabel lblH = new JLabel( dimName );
			lblH.setHorizontalAlignment( SwingConstants.CENTER );
			lblH.setMaximumSize( labelSizeH );
			cCols.add( lblH );

			JLabel lblV = new JLabel( dimName );
			lblV.setUI( verticalUI );
			lblV.setHorizontalAlignment( SwingConstants.CENTER );
			lblV.setMaximumSize( labelSizeV );
			cRows.add( lblV );
		}

		GridBagLayout gbl_cViewport = new GridBagLayout();
		gbl_cViewport.columnWidths = new int[dims + 1];
		gbl_cViewport.rowHeights = new int[dims + 1];
		gbl_cViewport.columnWeights = new double[dims + 1];
		gbl_cViewport.columnWeights[dims] = Double.MIN_VALUE;
		gbl_cViewport.rowWeights = new double[dims + 1];
		gbl_cViewport.rowWeights[dims] = Double.MIN_VALUE;
		cViewport.setLayout( gbl_cViewport );

		for ( int y = 0; y < dims; ++y ) {
			// Column count is equal to row count, so we can use y here.
			gbl_cViewport.columnWidths[y] = visWidth;
			gbl_cViewport.rowHeights[y] = visHeight;

			for ( int x = 0; x < dims; ++x ) {
				if ( x == y ) {
					// TODO: Histogram
					continue;
				}
				else if ( !includeFlippedDims || ( includeFlippedDims && x > y ) ) {
					displayMap.put( ImmutablePair.of( x, y ), null );
					visibilityMap.put( ImmutablePair.of( x, y ), false );
				}
			}
		}
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

	private void setDimensionVisibility( int d, boolean horizontal, boolean visible )
	{
		for ( Pair<Integer, Integer> pair : visibilityMap.keySet() ) {
			if ( ( horizontal && pair.getLeft() == d ) || ( !horizontal && pair.getRight() == d ) ) {
				visibilityMap.put( pair, visible );
			}
		}

		dimensionVisibilityToggled.broadcast( ImmutablePair.of( d, horizontal ) );
	}

	// ----------------------------------------------------------------------------------------

	private void onDimensionVisibilityToggled( Pair<Integer, Boolean> args )
	{
		// Unpack event arguments
		int dim = args.getLeft();
		boolean horizontal = args.getRight();

		Node node = context.findGroup( context.getSelectedRow() );

		for ( Pair<Integer, Integer> pair : visibilityMap.keySet() ) {
			int x = pair.getLeft();
			int y = pair.getRight();

			if ( ( horizontal && x == dim ) ||
				( !horizontal && y == dim ) ) {

				if ( x == y ) {
					// TODO: Histogram
					continue;
				}
				else {
					Display display = displayMap.getOrDefault( pair, null );
					boolean vis = visibilityMap.get( pair );

					if ( display == null ) {
						// Lazily create the requested display.
						createInstanceDisplayFor( node, x, y );
					}
					else {
						// If the display was previously hidden, redraw it.
						display.setVisible( vis );
						if ( vis ) {
							redrawDisplay( display );
						}
					}
				}
			}
		}

		revalidate();
		repaint();
	}

	private void onHierarchyChanging( Hierarchy h )
	{
		// Clear all visualizations
		displayMap.clear();
		visibilityMap.clear();

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
		createDisplays();

		revalidate();
		repaint();
	}

	private void onNodeSelectionChanged( int row )
	{
		for ( Entry<Pair<Integer, Integer>, Display> entry : displayMap.entrySet() ) {
			Display display = entry.getValue();

			// Don't redraw hidden displays.
			if ( visibilityMap.get( entry.getKey() ) ) {
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

	private void onWindowClosing()
	{
	}
}
