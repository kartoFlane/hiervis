package pl.pwr.hiervis.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.LineBorder;

import pl.pwr.hiervis.core.HVConfig;
import pl.pwr.hiervis.core.HVContext;
import pl.pwr.hiervis.util.SwingUIUtils;
import pl.pwr.hiervis.util.ui.GridBagConstraintsBuilder;
import pl.pwr.hiervis.util.ui.SquareColorPickerDialog;


@SuppressWarnings("serial")
public class ConfigDialog extends JDialog
{
	private JComboBox<String> listLAF;
	private JSlider sldPointSize;
	private JSlider sldPrecision;

	private JLabel lblColorCurrentGroup;
	private JLabel lblColorChildGroup;
	private JLabel lblColorParentGroup;
	private JLabel lblColorAncestorGroup;
	private JLabel lblColorOtherGroup;
	private JLabel lblColorHistogram;
	private JLabel lblColorBackground;


	private HVConfig newConfig = null;


	public ConfigDialog( HVContext context, Window frame )
	{
		super( frame, "Config" );

		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		setModal( true );
		setResizable( false );

		SwingUIUtils.installEscapeCloseOperation( this );

		createConfigTabPanel( context );
		createButtonPanel( context );

		pack();
	}

	private void createConfigTabPanel( HVContext context )
	{
		JTabbedPane cTabs = new JTabbedPane( JTabbedPane.TOP );
		getContentPane().add( cTabs, BorderLayout.CENTER );

		createGeneralTab( context, cTabs );
		createColorsTab( context, cTabs );
	}

	private void createGeneralTab( HVContext context, JTabbedPane cTabs )
	{
		JPanel cGeneral = new JPanel();
		cTabs.addTab( "General", null, cGeneral, null );
		GridBagLayout gbl_cGeneral = new GridBagLayout();
		gbl_cGeneral.columnWidths = new int[] { 200, 0 };
		gbl_cGeneral.rowHeights = new int[] { 0, 0, 0, 0, 0, 0 };
		gbl_cGeneral.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_cGeneral.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 1.0 };
		cGeneral.setLayout( gbl_cGeneral );

		GridBagConstraintsBuilder builder = new GridBagConstraintsBuilder();

		JLabel lblLAF = new JLabel( "GUI Look And Feel (restart required):" );
		cGeneral.add( lblLAF, builder.fillHorizontal().insets( 5 ).position( 0, 0 ).build() );

		listLAF = new JComboBox<String>();
		cGeneral.add( listLAF, builder.fillHorizontal().insets( 0, 5, 10, 5 ).position( 0, 1 ).build() );

		for ( LookAndFeelInfo info : UIManager.getInstalledLookAndFeels() ) {
			listLAF.addItem( info.getName() );
		}

		listLAF.setToolTipText(
			SwingUIUtils.toHTML( "The look and feel used by the application's GUI.\nRequires restart." )
		);

		JLabel lblPointSize = new JLabel( "Instance size:" );
		cGeneral.add( lblPointSize, builder.fillHorizontal().insets( 5 ).position( 0, 2 ).build() );

		sldPointSize = new JSlider( 1, 5 );
		sldPointSize.setPaintLabels( true );
		sldPointSize.setSnapToTicks( true );
		sldPointSize.setPaintTicks( true );
		sldPointSize.setMajorTickSpacing( 1 );
		sldPointSize.setToolTipText( "Size of an individual data point in instance visualizations, in pixels." );
		cGeneral.add( sldPointSize, builder.fillHorizontal().insets( 0, 5, 10, 5 ).position( 0, 3 ).build() );

		JLabel lblPrecision = new JLabel( "Measure value precision:" );
		cGeneral.add( lblPrecision, builder.fillHorizontal().insets( 5 ).position( 0, 4 ).build() );

		sldPrecision = new JSlider( 1, 5 );
		sldPrecision.setPaintLabels( true );
		sldPrecision.setSnapToTicks( true );
		sldPrecision.setPaintTicks( true );
		sldPrecision.setMajorTickSpacing( 1 );
		sldPrecision.setToolTipText( "Precision at which fractional measure values are displayed." );
		cGeneral.add( sldPrecision, builder.fillHorizontal().insets( 0, 5, 10, 5 ).position( 0, 5 ).build() );

		// Apply current config values
		HVConfig cfg = context.getConfig();

		listLAF.setSelectedItem( cfg.getPreferredLookAndFeel() );
		sldPointSize.setValue( cfg.getPointSize() );
		sldPrecision.setValue( cfg.getDoubleFormatPrecision() );
	}

	private void createColorsTab( HVContext context, JTabbedPane cTabs )
	{
		final Dimension colorLabelDim = new Dimension( 60, 25 );

		JPanel cColors = new JPanel();
		cTabs.addTab( "Colors", null, cColors, null );
		GridBagLayout gbl_cColors = new GridBagLayout();
		gbl_cColors.columnWidths = new int[] { 0, colorLabelDim.width, 0 };
		gbl_cColors.rowHeights = new int[] {
			colorLabelDim.height, colorLabelDim.height, colorLabelDim.height,
			colorLabelDim.height, colorLabelDim.height, colorLabelDim.height,
			colorLabelDim.height, 0
		};
		gbl_cColors.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gbl_cColors.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		cColors.setLayout( gbl_cColors );

		GridBagConstraintsBuilder builder = new GridBagConstraintsBuilder();

		JLabel lblCurrentGroupColor = new JLabel( "Current group color:" );
		cColors.add( lblCurrentGroupColor, builder.anchorWest().insets( 5 ).position( 0, 0 ).build() );

		lblColorCurrentGroup = new JLabel();
		lblColorCurrentGroup.setOpaque( true );
		lblColorCurrentGroup.setBorder( new LineBorder( Color.lightGray ) );
		lblColorCurrentGroup.setPreferredSize( colorLabelDim );
		lblColorCurrentGroup.setCursor( Cursor.getPredefinedCursor( Cursor.HAND_CURSOR ) );
		cColors.add( lblColorCurrentGroup, builder.anchorEast().fillVertical().insets( 5, 0, 5, 5 ).position( 1, 0 ).build() );

		JLabel lblChildGroupColor = new JLabel( "Child group color:" );
		cColors.add( lblChildGroupColor, builder.anchorWest().insets( 0, 5, 5, 5 ).position( 0, 1 ).build() );

		lblColorChildGroup = new JLabel();
		lblColorChildGroup.setOpaque( true );
		lblColorChildGroup.setBorder( new LineBorder( Color.lightGray ) );
		lblColorChildGroup.setPreferredSize( colorLabelDim );
		lblColorChildGroup.setCursor( Cursor.getPredefinedCursor( Cursor.HAND_CURSOR ) );
		cColors.add( lblColorChildGroup, builder.anchorEast().fillVertical().insets( 0, 0, 5, 5 ).position( 1, 1 ).build() );

		JLabel lblParentGroupColor = new JLabel( "Parent group color:" );
		cColors.add( lblParentGroupColor, builder.anchorWest().insets( 0, 5, 5, 5 ).position( 0, 2 ).build() );

		lblColorParentGroup = new JLabel();
		lblColorParentGroup.setOpaque( true );
		lblColorParentGroup.setBorder( new LineBorder( Color.lightGray ) );
		lblColorParentGroup.setPreferredSize( colorLabelDim );
		lblColorParentGroup.setCursor( Cursor.getPredefinedCursor( Cursor.HAND_CURSOR ) );
		cColors.add( lblColorParentGroup, builder.anchorEast().fillVertical().insets( 0, 0, 5, 5 ).position( 1, 2 ).build() );

		JLabel lblAncestorGroupColor = new JLabel( "Ancestor group color:" );
		cColors.add( lblAncestorGroupColor, builder.anchorWest().insets( 0, 5, 5, 5 ).position( 0, 3 ).build() );

		lblColorAncestorGroup = new JLabel();
		lblColorAncestorGroup.setOpaque( true );
		lblColorAncestorGroup.setBorder( new LineBorder( Color.lightGray ) );
		lblColorAncestorGroup.setPreferredSize( colorLabelDim );
		lblColorAncestorGroup.setCursor( Cursor.getPredefinedCursor( Cursor.HAND_CURSOR ) );
		cColors.add( lblColorAncestorGroup, builder.anchorEast().fillVertical().insets( 0, 0, 5, 5 ).position( 1, 3 ).build() );

		JLabel lblOtherGroupColor = new JLabel( "Other group color:" );
		cColors.add( lblOtherGroupColor, builder.anchorWest().insets( 0, 5, 5, 5 ).position( 0, 4 ).build() );

		lblColorOtherGroup = new JLabel();
		lblColorOtherGroup.setOpaque( true );
		lblColorOtherGroup.setBorder( new LineBorder( Color.lightGray ) );
		lblColorOtherGroup.setPreferredSize( colorLabelDim );
		lblColorOtherGroup.setCursor( Cursor.getPredefinedCursor( Cursor.HAND_CURSOR ) );
		cColors.add( lblColorOtherGroup, builder.anchorEast().fillVertical().insets( 0, 0, 5, 5 ).position( 1, 4 ).build() );

		JLabel lblHistogramColor = new JLabel( "Histogram bar color:" );
		cColors.add( lblHistogramColor, builder.anchorWest().insets( 0, 5, 5, 5 ).position( 0, 5 ).build() );

		lblColorHistogram = new JLabel();
		lblColorHistogram.setOpaque( true );
		lblColorHistogram.setBorder( new LineBorder( Color.lightGray ) );
		lblColorHistogram.setPreferredSize( colorLabelDim );
		lblColorHistogram.setCursor( Cursor.getPredefinedCursor( Cursor.HAND_CURSOR ) );
		cColors.add( lblColorHistogram, builder.anchorEast().fillVertical().insets( 0, 0, 5, 5 ).position( 1, 5 ).build() );

		JLabel lblBackgroundColor = new JLabel( "Background color:" );
		cColors.add( lblBackgroundColor, builder.anchorWest().insets( 0, 5, 5, 5 ).position( 0, 6 ).build() );

		lblColorBackground = new JLabel();
		lblColorBackground.setOpaque( true );
		lblColorBackground.setBorder( new LineBorder( Color.lightGray ) );
		lblColorBackground.setPreferredSize( colorLabelDim );
		lblColorBackground.setCursor( Cursor.getPredefinedCursor( Cursor.HAND_CURSOR ) );
		cColors.add( lblColorBackground, builder.anchorEast().fillVertical().insets( 0, 0, 5, 5 ).position( 1, 6 ).build() );

		MouseListener ml = new MouseAdapter() {
			@Override
			public void mouseClicked( MouseEvent e )
			{
				if ( SwingUtilities.isLeftMouseButton( e ) ) {
					JLabel source = (JLabel)e.getSource();

					SquareColorPickerDialog colorPickerDialog = new SquareColorPickerDialog(
						ConfigDialog.this, source.getBackground()
					);
					colorPickerDialog.setVisible( true ); // Blocks until the dialog is dismissed.

					source.setBackground( colorPickerDialog.getSelection() );
				}
			}
		};

		lblColorCurrentGroup.addMouseListener( ml );
		lblColorChildGroup.addMouseListener( ml );
		lblColorParentGroup.addMouseListener( ml );
		lblColorAncestorGroup.addMouseListener( ml );
		lblColorOtherGroup.addMouseListener( ml );
		lblColorHistogram.addMouseListener( ml );
		lblColorBackground.addMouseListener( ml );

		// Apply current config values
		HVConfig cfg = context.getConfig();
		lblColorCurrentGroup.setBackground( cfg.getCurrentGroupColor() );
		lblColorChildGroup.setBackground( cfg.getChildGroupColor() );
		lblColorParentGroup.setBackground( cfg.getParentGroupColor() );
		lblColorAncestorGroup.setBackground( cfg.getAncestorGroupColor() );
		lblColorOtherGroup.setBackground( cfg.getOtherGroupColor() );
		lblColorHistogram.setBackground( cfg.getHistogramColor() );
		lblColorBackground.setBackground( cfg.getBackgroundColor() );
	}

	private void createButtonPanel( HVContext context )
	{
		JPanel cButtons = new JPanel();
		getContentPane().add( cButtons, BorderLayout.SOUTH );
		GridBagLayout gbl_cButtons = new GridBagLayout();
		gbl_cButtons.columnWidths = new int[] { 0, 0, 0 };
		gbl_cButtons.rowHeights = new int[] { 0, 0 };
		gbl_cButtons.columnWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
		gbl_cButtons.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
		cButtons.setLayout( gbl_cButtons );

		GridBagConstraintsBuilder builder = new GridBagConstraintsBuilder();

		JButton btnConfirm = new JButton( "Confirm" );
		cButtons.add( btnConfirm, builder.anchorEast().insets( 5, 0, 5, 5 ).position( 0, 0 ).build() );

		JButton btnCancel = new JButton( "Cancel" );
		cButtons.add( btnCancel, builder.insets( 5, 0, 5, 5 ).position( 1, 0 ).build() );

		btnCancel.addActionListener(
			( e ) -> {
				// Dispatch closing event instead of calling dispose() directly,
				// so that event listeners are notified.
				dispatchEvent( new WindowEvent( this, WindowEvent.WINDOW_CLOSING ) );
			}
		);

		btnConfirm.addActionListener(
			( e ) -> {
				updateConfig( context );
				// Dispatch closing event instead of calling dispose() directly,
				// so that event listeners are notified.
				dispatchEvent( new WindowEvent( this, WindowEvent.WINDOW_CLOSING ) );
			}
		);
	}

	private void updateConfig( HVContext context )
	{
		newConfig = context.getConfig().copy();

		newConfig.setCurrentLevelColor( lblColorCurrentGroup.getBackground() );
		newConfig.setChildGroupColor( lblColorChildGroup.getBackground() );
		newConfig.setParentGroupColor( lblColorParentGroup.getBackground() );
		newConfig.setAncestorGroupColor( lblColorAncestorGroup.getBackground() );
		newConfig.setOtherGroupColor( lblColorOtherGroup.getBackground() );
		newConfig.setHistogramColor( lblColorHistogram.getBackground() );
		newConfig.setBackgroundColor( lblColorBackground.getBackground() );

		newConfig.setPreferredLookAndFeel( listLAF.getSelectedItem().toString() );
		newConfig.setPointSize( sldPointSize.getValue() );
		newConfig.setDoubleFormatPrecision( sldPrecision.getValue() );
	}

	/**
	 * @return the new config instance, if the user exited the dialog by
	 *         pressing the 'Confirm' button. Null otherwise.
	 */
	public HVConfig getConfig()
	{
		return newConfig;
	}

	/**
	 * @return true if the user has made changes to the config, false if config state is unchanged.
	 */
	public boolean hasConfigChanged()
	{
		return newConfig != null;
	}
}
