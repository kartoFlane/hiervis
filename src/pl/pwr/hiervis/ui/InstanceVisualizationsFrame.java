package pl.pwr.hiervis.ui;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseWheelListener;
import java.util.HashMap;
import java.util.Map.Entry;

import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import basic_hierarchy.interfaces.Hierarchy;
import basic_hierarchy.interfaces.Node;
import pl.pwr.hiervis.core.HVContext;
import pl.pwr.hiervis.util.Event;
import pl.pwr.hiervis.util.SwingUIUtils;
import pl.pwr.hiervis.util.Utils;
import pl.pwr.hiervis.visualisation.HierarchyProcessor;
import prefuse.Display;
import prefuse.Visualization;


@SuppressWarnings("serial")
public class InstanceVisualizationsFrame extends JFrame
{
	public final Event<Pair<Integer, Boolean>> dimensionVisibilityToggled = new Event<>();

	private HVContext context;

	// TODO: Move this to config, but make modifiable in the vis frame
	private int visWidth = 200;
	private int visHeight = 200;
	private int pointSize = 1;

	private HashMap<Pair<Integer, Integer>, Display> displayMap;
	private HashMap<Pair<Integer, Integer>, Boolean> visibilityMap;
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
		SwingUIUtils.addCloseCallback( this, this::onWindowClosing );

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

		cCols = new JPanel();
		GridBagConstraints gbc_cCols = new GridBagConstraints();
		gbc_cCols.anchor = GridBagConstraints.WEST;
		gbc_cCols.fill = GridBagConstraints.VERTICAL;
		gbc_cCols.insets = new Insets( 5, 5, 5, 5 );
		gbc_cCols.gridx = 1;
		gbc_cCols.gridy = 0;
		getContentPane().add( cCols, gbc_cCols );
		cCols.setLayout( new BoxLayout( cCols, BoxLayout.X_AXIS ) );

		cRows = new JPanel();
		GridBagConstraints gbc_cRows = new GridBagConstraints();
		gbc_cRows.anchor = GridBagConstraints.NORTH;
		gbc_cRows.fill = GridBagConstraints.HORIZONTAL;
		gbc_cRows.insets = new Insets( 5, 5, 5, 5 );
		gbc_cRows.gridx = 0;
		gbc_cRows.gridy = 1;
		getContentPane().add( cRows, gbc_cRows );
		cRows.setLayout( new BoxLayout( cRows, BoxLayout.Y_AXIS ) );

		JScrollPane scrollPane = new JScrollPane();
		GridBagConstraints gbc_cScrollVis = new GridBagConstraints();
		gbc_cScrollVis.fill = GridBagConstraints.BOTH;
		gbc_cScrollVis.gridx = 1;
		gbc_cScrollVis.gridy = 1;
		getContentPane().add( scrollPane, gbc_cScrollVis );

		scrollPane.getHorizontalScrollBar().setUnitIncrement( 16 );
		scrollPane.getVerticalScrollBar().setUnitIncrement( 16 );

		cViewport = new JPanel();
		scrollPane.setViewportView( cViewport );

		if ( context.isHierarchyDataLoaded() ) {
			createVisualizations();
		}
	}

	private Display createInstanceDisplay( Visualization vis )
	{
		Display display = new Display( vis );
		display.setHighQuality( true );
		display.setBackground( context.getConfig().getBackgroundColor() );

		// Remove mouse wheel listeners so that the display doesn't needlessly consume them,
		// allowing the parent scroll pane to scroll even when the mouse cursor is over a display.
		for ( MouseWheelListener l : display.getMouseWheelListeners() ) {
			display.removeMouseWheelListener( l );
		}

		// display.addControlListener( new PanControl( true ) );
		// display.addControlListener( new ZoomScrollControl() );
		// display.addControlListener( new ToolTipControl( HVConstants.PREFUSE_INSTANCE_LABEL_COLUMN_NAME ) );

		display.setSize( visWidth, visHeight );
		display.setPreferredSize( new Dimension( visWidth, visHeight ) );
		Utils.unzoom( display, 0 );

		return display;
	}

	private void createVisualizations()
	{
		// TODO: Make this a config property?
		final boolean includeFlippedDims = true;

		String[] dataNames = HierarchyProcessor.getFeatureNames( context.getHierarchy() );
		int dims = dataNames.length;

		for ( int i = 0; i < dims; ++i ) {
			String dimName = dataNames[i];

			JCheckBox cboxX = new JCheckBox( dimName );
			cboxX.setSelected( true );
			JCheckBox cboxY = new JCheckBox( dimName );
			cboxY.setSelected( true );

			final int d = i;
			cboxX.addActionListener( e -> setDimensionVisibility( d, true, cboxX.isSelected() ) );
			cboxY.addActionListener( e -> setDimensionVisibility( d, false, cboxY.isSelected() ) );

			cCols.add( cboxX );
			cRows.add( cboxY );
		}

		GridBagLayout gbl_cViewport = new GridBagLayout();
		gbl_cViewport.columnWidths = new int[dims + 1];
		gbl_cViewport.rowHeights = new int[dims + 1];
		gbl_cViewport.columnWeights = new double[dims + 1];
		gbl_cViewport.columnWeights[dims] = Double.MIN_VALUE;
		gbl_cViewport.rowWeights = new double[dims + 1];
		gbl_cViewport.rowWeights[dims] = Double.MIN_VALUE;
		cViewport.setLayout( gbl_cViewport );

		Node node = context.findGroup( context.getSelectedRow() );

		for ( int y = 0; y < dims; ++y ) {
			// HVConstants.INSTANCE_DATA_NAME
			// Column count is equal to row count, so we can use y here.
			gbl_cViewport.columnWidths[y] = visWidth;
			gbl_cViewport.rowHeights[y] = visHeight;

			for ( int x = 0; x < dims; ++x ) {
				if ( x == y ) {
					continue;
				}
				else if ( !includeFlippedDims || ( includeFlippedDims && x > y ) ) {
					GridBagConstraints constraints = new GridBagConstraints();
					constraints.insets = new Insets( 5, 5, 5, 5 );
					constraints.gridx = x;
					constraints.gridy = y;

					Visualization vis = HierarchyProcessor.createInstanceVisualization(
						context, node, pointSize, x, y, false
					);
					Display display = createInstanceDisplay( vis );

					displayMap.put( ImmutablePair.of( x, y ), display );
					visibilityMap.put( ImmutablePair.of( x, y ), true );

					cViewport.add( display, constraints );

					vis.run( "draw" );
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

	private void onDimensionVisibilityToggled( Pair<Integer, Boolean> p )
	{
		for ( Entry<Pair<Integer, Integer>, Display> entry : displayMap.entrySet() ) {
			entry.getValue().setVisible( visibilityMap.get( entry.getKey() ) );
		}

		revalidate();
		repaint();
	}

	private void onHierarchyChanging( Hierarchy h )
	{
		// Clear all visualizations
		displayMap.clear();
		visibilityMap.clear();

		cCols.removeAll();
		cRows.removeAll();
		cViewport.removeAll();

		revalidate();
		repaint();
	}

	private void onHierarchyChanged( Hierarchy h )
	{
		createVisualizations();

		revalidate();
		repaint();
	}

	private void onNodeSelectionChanged( int row )
	{
		for ( Entry<Pair<Integer, Integer>, Display> entry : displayMap.entrySet() ) {
			Display display = entry.getValue();

			// Unzoom the display so that drawing is not botched.
			Utils.unzoom( display, 0 );
			display.getVisualization().run( "draw" );
		}
	}

	private void onWindowClosing()
	{
	}
}
