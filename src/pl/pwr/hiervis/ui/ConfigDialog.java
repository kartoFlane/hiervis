package pl.pwr.hiervis.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.LineBorder;

import pl.pwr.hiervis.core.HVConfig;
import pl.pwr.hiervis.core.HVContext;
import pl.pwr.hiervis.util.GridBagConstraintsBuilder;
import pl.pwr.hiervis.util.SwingUIUtils;


@SuppressWarnings("serial")
public class ConfigDialog extends JDialog
{
	private JComboBox<String> listLAF;
	private JSlider sldPointSize;
	private JCheckBox cboxUseTrueClass;

	private JLabel lblColorSelectedNode;
	private JLabel lblColorChildGroup;
	private JLabel lblColorParentGroup;
	private JLabel lblColorAncestorGroup;
	private JLabel lblColorOtherGroup;
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
		cGeneral.add( sldPointSize, builder.fillHorizontal().insets( 0, 5, 10, 5 ).position( 0, 3 ).build() );

		sldPointSize.setToolTipText(
			SwingUIUtils.toHTML( "Size of an individual data point in instance visualizations, in pixels." )
		);

		cboxUseTrueClass = new JCheckBox( "Use true class" );
		GridBagConstraints gbc_cboxVisTrueClass = new GridBagConstraints();
		gbc_cboxVisTrueClass.fill = GridBagConstraints.HORIZONTAL;
		gbc_cboxVisTrueClass.insets = new Insets( 0, 5, 5, 5 );
		gbc_cboxVisTrueClass.gridx = 0;
		gbc_cboxVisTrueClass.gridy = 3;
		cGeneral.add( cboxUseTrueClass, builder.fillHorizontal().insets( 0, 5, 0, 5 ).position( 0, 4 ).build() );

		// Apply current config values
		HVConfig cfg = context.getConfig();

		listLAF.setSelectedItem( cfg.getPreferredLookAndFeel() );
		sldPointSize.setValue( cfg.getPointSize() );

		if ( cfg.hasTrueClassAttribute() ) {
			cboxUseTrueClass.setEnabled( true );
			cboxUseTrueClass.setSelected( cfg.isUseTrueClass() );

			cboxUseTrueClass.setToolTipText(
				SwingUIUtils.toHTML(
					"If selected, instances will be grouped according to true class instead of assign class."
				)
			);
		}
		else {
			cboxUseTrueClass.setEnabled( false );
			cboxUseTrueClass.setSelected( false );

			cboxUseTrueClass.setToolTipText(
				SwingUIUtils.toHTML(
					"Disabled: no hierarchy loaded, or hierarchy has no true class attribute."
				)
			);
		}
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
			colorLabelDim.height, colorLabelDim.height, colorLabelDim.height, 0 };
		gbl_cColors.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gbl_cColors.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		cColors.setLayout( gbl_cColors );

		GridBagConstraintsBuilder builder = new GridBagConstraintsBuilder();

		JLabel lblSelectedNodeColor = new JLabel( "Selected node color:" );
		cColors.add( lblSelectedNodeColor, builder.anchorWest().insets( 5 ).position( 0, 0 ).build() );

		lblColorSelectedNode = new JLabel();
		lblColorSelectedNode.setOpaque( true );
		lblColorSelectedNode.setBorder( new LineBorder( Color.lightGray ) );
		lblColorSelectedNode.setPreferredSize( colorLabelDim );
		cColors.add( lblColorSelectedNode, builder.anchorEast().fillVertical().insets( 5, 0, 5, 5 ).position( 1, 0 ).build() );

		JLabel lblChildGroupColor = new JLabel( "Child group color:" );
		cColors.add( lblChildGroupColor, builder.anchorWest().insets( 0, 5, 5, 5 ).position( 0, 1 ).build() );

		lblColorChildGroup = new JLabel();
		lblColorChildGroup.setOpaque( true );
		lblColorChildGroup.setBorder( new LineBorder( Color.lightGray ) );
		lblColorChildGroup.setPreferredSize( colorLabelDim );
		cColors.add( lblColorChildGroup, builder.anchorEast().fillVertical().insets( 0, 0, 5, 5 ).position( 1, 1 ).build() );

		JLabel lblParentGroupColor = new JLabel( "Parent group color:" );
		cColors.add( lblParentGroupColor, builder.anchorWest().insets( 0, 5, 5, 5 ).position( 0, 2 ).build() );

		lblColorParentGroup = new JLabel();
		lblColorParentGroup.setOpaque( true );
		lblColorParentGroup.setBorder( new LineBorder( Color.lightGray ) );
		lblColorParentGroup.setPreferredSize( colorLabelDim );
		cColors.add( lblColorParentGroup, builder.anchorEast().fillVertical().insets( 0, 0, 5, 5 ).position( 1, 2 ).build() );

		JLabel lblAncestorGroupColor = new JLabel( "Ancestor group color:" );
		cColors.add( lblAncestorGroupColor, builder.anchorWest().insets( 0, 5, 5, 5 ).position( 0, 3 ).build() );

		lblColorAncestorGroup = new JLabel();
		lblColorAncestorGroup.setOpaque( true );
		lblColorAncestorGroup.setBorder( new LineBorder( Color.lightGray ) );
		lblColorAncestorGroup.setPreferredSize( colorLabelDim );
		cColors.add( lblColorAncestorGroup, builder.anchorEast().fillVertical().insets( 0, 0, 5, 5 ).position( 1, 3 ).build() );

		JLabel lblOtherGroupColor = new JLabel( "Other group color:" );
		cColors.add( lblOtherGroupColor, builder.anchorWest().insets( 0, 5, 5, 5 ).position( 0, 4 ).build() );

		lblColorOtherGroup = new JLabel();
		lblColorOtherGroup.setOpaque( true );
		lblColorOtherGroup.setBorder( new LineBorder( Color.lightGray ) );
		lblColorOtherGroup.setPreferredSize( colorLabelDim );
		cColors.add( lblColorOtherGroup, builder.anchorEast().fillVertical().insets( 0, 0, 5, 5 ).position( 1, 4 ).build() );

		JLabel lblBackgroundColor = new JLabel( "Background color:" );
		cColors.add( lblBackgroundColor, builder.anchorWest().insets( 0, 5, 5, 5 ).position( 0, 5 ).build() );

		lblColorBackground = new JLabel();
		cColors.add( lblColorBackground, builder.anchorEast().fillVertical().insets( 0, 0, 5, 5 ).position( 1, 5 ).build() );
		lblColorBackground.setOpaque( true );
		lblColorBackground.setBorder( new LineBorder( Color.lightGray ) );
		lblColorBackground.setPreferredSize( colorLabelDim );

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

		lblColorSelectedNode.addMouseListener( ml );
		lblColorChildGroup.addMouseListener( ml );
		lblColorParentGroup.addMouseListener( ml );
		lblColorAncestorGroup.addMouseListener( ml );
		lblColorOtherGroup.addMouseListener( ml );
		lblColorBackground.addMouseListener( ml );

		// Apply current config values
		HVConfig cfg = context.getConfig();
		lblColorSelectedNode.setBackground( cfg.getCurrentGroupColor() );
		lblColorChildGroup.setBackground( cfg.getChildGroupColor() );
		lblColorParentGroup.setBackground( cfg.getParentGroupColor() );
		lblColorAncestorGroup.setBackground( cfg.getAncestorGroupColor() );
		lblColorOtherGroup.setBackground( cfg.getOtherGroupColor() );
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

		newConfig.setCurrentLevelColor( lblColorSelectedNode.getBackground() );
		newConfig.setChildGroupColor( lblColorChildGroup.getBackground() );
		newConfig.setParentGroupColor( lblColorParentGroup.getBackground() );
		newConfig.setAncestorGroupColor( lblColorAncestorGroup.getBackground() );
		newConfig.setOtherGroupColor( lblColorOtherGroup.getBackground() );
		newConfig.setBackgroundColor( lblColorBackground.getBackground() );

		newConfig.setPreferredLookAndFeel( listLAF.getSelectedItem().toString() );
		newConfig.setPointSize( sldPointSize.getValue() );
		newConfig.setUseTrueClass( cboxUseTrueClass.isSelected() );
	}

	/**
	 * Gets the text of the specified text field, or "0", if the text field is empty.
	 */
	private String getText( JTextField text )
	{
		String t = text.getText();
		return t == null || t.equals( "" ) ? "0" : t;
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
