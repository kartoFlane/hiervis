package pl.pwr.hierarchyvis.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.nio.file.Paths;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import pl.pwr.hierarchyvis.HierarchyVisualizer;
import pl.pwr.hierarchyvis.core.HVConfig;
import pl.pwr.hierarchyvis.core.HVContext;
import pl.pwr.hierarchyvis.ui.control.NodeSelectionControl;
import pl.pwr.hierarchyvis.ui.control.PanControl;
import pl.pwr.hierarchyvis.ui.control.SubtreeDragControl;
import pl.pwr.hierarchyvis.ui.control.ZoomScrollControl;
import pl.pwr.hierarchyvis.util.SwingUIUtils;
import pl.pwr.hierarchyvis.visualisation.HierarchyProcessor;
import prefuse.Display;
import prefuse.Visualization;
import prefuse.controls.Control;


public class VisualizerFrame extends JFrame {
	private static final long serialVersionUID = 1L;

	private static final int defaultFrameWidth = 900;
	private static final int defaultFrameHeight = 600;

	private HVContext context;

	private Display pointDisplay;
	private Display treeDisplay;


	public VisualizerFrame( HVContext context ) {
		super( HierarchyVisualizer.APP_NAME );

		if ( context == null )
			throw new RuntimeException( "Context must not be null!" );

		this.context = context;

		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		setSize( defaultFrameWidth, defaultFrameHeight );
		setMinimumSize( new Dimension( defaultFrameWidth, defaultFrameHeight / 2 ) );

		// Make the frame appear at the center of the screen
		setLocationRelativeTo( null );

		addWindowListener( new WindowAdapter() {
			@Override
			public void windowClosing( WindowEvent e ) {
				// Save the current configuration on application exit.
				context.getConfig().to( new File( HVConfig.FILE_PATH ) );
			}
		} );

		createMenu();
		createGUI();
	}

	private void createGUI() {
		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWeights = new double[] { 1.0, 1.0 };
		gridBagLayout.rowWeights = new double[] { 1.0 };
		getContentPane().setLayout( gridBagLayout );

		JPanel cTreeViewer = new JPanel();
		FlowLayout flowLayout = (FlowLayout)cTreeViewer.getLayout();
		flowLayout.setVgap( 0 );
		flowLayout.setHgap( 0 );

		GridBagConstraints gbc_cTreeViewer = new GridBagConstraints();
		gbc_cTreeViewer.insets = new Insets( 5, 5, 5, 5 );
		gbc_cTreeViewer.fill = GridBagConstraints.BOTH;
		gbc_cTreeViewer.gridx = 0;
		gbc_cTreeViewer.gridy = 0;
		getContentPane().add( cTreeViewer, gbc_cTreeViewer );

		treeDisplay = context.createHierarchyDisplay();
		cTreeViewer.add( treeDisplay );

		pointDisplay = new Display( new Visualization() );

		treeDisplay.addControlListener( new NodeSelectionControl( context, pointDisplay ) );
		treeDisplay.addControlListener( new SubtreeDragControl( Control.RIGHT_MOUSE_BUTTON ) );
		treeDisplay.addControlListener( new PanControl( true ) );
		treeDisplay.addControlListener( new ZoomScrollControl() );
		treeDisplay.setEnabled( false );

		pointDisplay.addControlListener( new PanControl() );
		pointDisplay.addControlListener( new ZoomScrollControl() );
		pointDisplay.setBackground( Color.lightGray );
		pointDisplay.setEnabled( false );

		JPanel cNodeDetail = new JPanel();
		GridBagConstraints gbc_cNodeDetail = new GridBagConstraints();
		gbc_cNodeDetail.insets = new Insets( 5, 0, 5, 5 );
		gbc_cNodeDetail.fill = GridBagConstraints.BOTH;
		gbc_cNodeDetail.gridx = 1;
		gbc_cNodeDetail.gridy = 0;
		getContentPane().add( cNodeDetail, gbc_cNodeDetail );
		GridBagLayout gbl_cNodeDetail = new GridBagLayout();
		gbl_cNodeDetail.columnWeights = new double[] { 1.0, 0.0 };
		gbl_cNodeDetail.rowWeights = new double[] { 1.0, 0.0 };
		cNodeDetail.setLayout( gbl_cNodeDetail );

		JPanel cNodeViewer = new JPanel();
		GridBagConstraints gbc_cNodeViewer = new GridBagConstraints();
		gbc_cNodeViewer.fill = GridBagConstraints.BOTH;
		gbc_cNodeViewer.gridx = 0;
		gbc_cNodeViewer.gridy = 0;
		cNodeDetail.add( cNodeViewer, gbc_cNodeViewer );
		cNodeViewer.setLayout( new BorderLayout( 0, 0 ) );
		cNodeViewer.add( pointDisplay );

		JPanel cHistogramVertical = new JPanel();
		GridBagConstraints gbc_cHistogramVertical = new GridBagConstraints();
		gbc_cHistogramVertical.insets = new Insets( 0, 0, 5, 0 );
		gbc_cHistogramVertical.fill = GridBagConstraints.BOTH;
		gbc_cHistogramVertical.gridx = 1;
		gbc_cHistogramVertical.gridy = 0;
		cNodeDetail.add( cHistogramVertical, gbc_cHistogramVertical );

		JLabel lblHistV = new JLabel( SwingUIUtils.toHTML( "TODO:\nHistogram" ) );
		cHistogramVertical.add( lblHistV );

		JPanel cHistogramHorizontal = new JPanel();
		GridBagConstraints gbc_cHistogramHorizontal = new GridBagConstraints();
		gbc_cHistogramHorizontal.insets = new Insets( 0, 0, 0, 5 );
		gbc_cHistogramHorizontal.fill = GridBagConstraints.BOTH;
		gbc_cHistogramHorizontal.gridx = 0;
		gbc_cHistogramHorizontal.gridy = 1;
		cNodeDetail.add( cHistogramHorizontal, gbc_cHistogramHorizontal );

		JLabel lblHistH = new JLabel( "TODO: Histogram" );
		cHistogramHorizontal.add( lblHistH );

		JPanel cStatistics = new JPanel();
		GridBagConstraints gbc_cStatistics = new GridBagConstraints();
		gbc_cStatistics.fill = GridBagConstraints.BOTH;
		gbc_cStatistics.gridx = 1;
		gbc_cStatistics.gridy = 1;
		cNodeDetail.add( cStatistics, gbc_cStatistics );

		JLabel lblStats = new JLabel( SwingUIUtils.toHTML( "TODO:\nStats" ) );
		cStatistics.add( lblStats );
	}

	private void createMenu() {
		JMenuBar menuBar = new JMenuBar();
		setJMenuBar( menuBar );

		JMenu mnFile = new JMenu( "File" );
		menuBar.add( mnFile );

		JMenuItem mntmOpenFile = new JMenuItem( "Open file..." );
		mnFile.add( mntmOpenFile );

		mntmOpenFile.addActionListener( ( e ) -> {
			openFileSelectionDialog();
		} );

		JSeparator separator = new JSeparator();
		mnFile.add( separator );

		JMenuItem mntmConfig = new JMenuItem( "Config" );
		mnFile.add( mntmConfig );

		mntmConfig.addActionListener( ( e ) -> {
			openConfigDialog();
		} );
	}

	/**
	 * Opens a file selection dialog, alowing the user to select a hierarchy file to load.
	 */
	private void openFileSelectionDialog() {
		FileDialog dialog = new FileDialog( this, "Choose a file", FileDialog.LOAD );
		dialog.setDirectory( new File( "." ).getAbsolutePath() );

		// NOTE: In order to cover all three major platforms (Windows/Linux/Mac),
		// we need to set *both* filename filter (which works on Linux and Mac, but
		// not on Windows), as well as file name (which works on Windows, but not on
		// Linux and Mac...)
		dialog.setFilenameFilter( ( dir, name ) -> name.endsWith( ".csv" ) );
		dialog.setFile( "*.csv" );

		dialog.setVisible( true ); // Blocks until the dialog is dismissed.

		String filename = dialog.getFile();
		if ( filename != null ) {
			treeDisplay.setEnabled( true );
			pointDisplay.setEnabled( true );

			context.load( Paths.get( dialog.getDirectory(), filename ) );

			reprocess();
		}
	}

	/**
	 * Opens a configuration dialog, allowing the user to change application settings.
	 * This method blocks until the user exits the dialog.
	 * If the user made changes to the application's settings, the hierarchy visualization
	 * is recreated.
	 */
	private void openConfigDialog() {
		ConfigDialog dialog = new ConfigDialog( context, VisualizerFrame.this );
		dialog.setVisible( true ); // Blocks until the dialog is dismissed.

		// If no hierarchy data is currently loaded, then we don't need to reprocess anything.
		if ( !context.isHierarchyDataLoaded() )
			return;

		// If no changes have been made to the config, then we don't need to reprocess anything.
		if ( dialog.getConfig() != null ) {
			context.setConfig( dialog.getConfig() );
			reprocess();
		}
	}

	/**
	 * Processes the currently loaded hierarchy data, creating and laying out the visualization
	 * using the currently selected settings.
	 * This needs to be called after changes are made to the application's settings so that they
	 * take effect on the interactive visualization.
	 */
	private void reprocess() {
		Visualization vis = context.createHierarchyVisualization();
		treeDisplay.setBackground( context.getConfig().getBackgroundColor() );
		treeDisplay.setVisualization( null );
		treeDisplay.setVisualization( vis );
		HierarchyProcessor.layoutVisualization( vis );

		NodeSelectionControl.selectNode( context, treeDisplay, pointDisplay, context.getSelectedRow() );
	}
}
