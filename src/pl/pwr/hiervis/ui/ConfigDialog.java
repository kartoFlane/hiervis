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
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import pl.pwr.hiervis.core.HVConfig;
import pl.pwr.hiervis.core.HVContext;
import pl.pwr.hiervis.ui.components.NumberDocument;
import pl.pwr.hiervis.util.SwingUIUtils;


@SuppressWarnings("serial")
public class ConfigDialog extends JDialog
{
	private JTextField txtTreeWidth;
	private JTextField txtTreeHeight;
	private JTextField txtPointWidth;
	private JTextField txtPointHeight;

	private JLabel lblColorSelectedNode;
	private JLabel lblColorChildGroup;
	private JLabel lblColorParentGroup;
	private JLabel lblColorAncestorGroup;
	private JLabel lblColorOtherGroup;
	private JLabel lblColorBackground;
	private JComboBox<String> listLAF;

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
		gbl_cGeneral.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 1.0 };
		cGeneral.setLayout( gbl_cGeneral );

		JLabel lblLAF = new JLabel( "GUI Look And Feel (restart required):" );
		GridBagConstraints gbc_lblLookAndFeel = new GridBagConstraints();
		gbc_lblLookAndFeel.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblLookAndFeel.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblLookAndFeel.gridx = 0;
		gbc_lblLookAndFeel.gridy = 0;
		cGeneral.add( lblLAF, gbc_lblLookAndFeel );

		listLAF = new JComboBox<String>();
		GridBagConstraints gbc_listLAF = new GridBagConstraints();
		gbc_listLAF.insets = new Insets( 0, 5, 5, 5 );
		gbc_listLAF.fill = GridBagConstraints.BOTH;
		gbc_listLAF.gridx = 0;
		gbc_listLAF.gridy = 1;
		cGeneral.add( listLAF, gbc_listLAF );

		for ( LookAndFeelInfo info : UIManager.getInstalledLookAndFeels() ) {
			listLAF.addItem( info.getName() );
		}

		listLAF.setToolTipText(
			SwingUIUtils.toHTML( "The look and feel used by the application's GUI.\nRequires restart." )
		);

		JPanel cTreeResolution = new JPanel();
		cTreeResolution.setBorder(
			new TitledBorder(
				UIManager.getBorder( "TitledBorder.border" ),
				"Hierarchy visualization resolution"
			)
		);

		GridBagConstraints gbc_cTreeResolution = new GridBagConstraints();
		gbc_cTreeResolution.insets = new Insets( 5, 5, 5, 5 );
		gbc_cTreeResolution.fill = GridBagConstraints.BOTH;
		gbc_cTreeResolution.gridx = 0;
		gbc_cTreeResolution.gridy = 2;
		cGeneral.add( cTreeResolution, gbc_cTreeResolution );
		GridBagLayout gbl_cTreeResolution = new GridBagLayout();
		gbl_cTreeResolution.columnWidths = new int[] { 0, 150, 0 };
		gbl_cTreeResolution.rowHeights = new int[] { 0, 0, 0 };
		gbl_cTreeResolution.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gbl_cTreeResolution.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
		cTreeResolution.setLayout( gbl_cTreeResolution );

		JLabel lblTreeWidth = new JLabel( "Width:" );
		GridBagConstraints gbc_lblTreeWidth = new GridBagConstraints();
		gbc_lblTreeWidth.anchor = GridBagConstraints.WEST;
		gbc_lblTreeWidth.insets = new Insets( 5, 5, 5, 0 );
		gbc_lblTreeWidth.gridx = 0;
		gbc_lblTreeWidth.gridy = 0;
		cTreeResolution.add( lblTreeWidth, gbc_lblTreeWidth );

		txtTreeWidth = new JTextField();
		txtTreeWidth.setColumns( 6 );
		txtTreeWidth.setHorizontalAlignment( SwingConstants.RIGHT );
		GridBagConstraints gbc_txtTreeWidth = new GridBagConstraints();
		gbc_txtTreeWidth.anchor = GridBagConstraints.EAST;
		gbc_txtTreeWidth.insets = new Insets( 5, 0, 5, 5 );
		gbc_txtTreeWidth.gridx = 1;
		gbc_txtTreeWidth.gridy = 0;
		cTreeResolution.add( txtTreeWidth, gbc_txtTreeWidth );

		JLabel lblTreeHeight = new JLabel( "Height:" );
		GridBagConstraints gbc_lblTreeHeight = new GridBagConstraints();
		gbc_lblTreeHeight.anchor = GridBagConstraints.WEST;
		gbc_lblTreeHeight.insets = new Insets( 0, 5, 0, 0 );
		gbc_lblTreeHeight.gridx = 0;
		gbc_lblTreeHeight.gridy = 1;
		cTreeResolution.add( lblTreeHeight, gbc_lblTreeHeight );

		txtTreeHeight = new JTextField();
		txtTreeHeight.setColumns( 6 );
		txtTreeHeight.setHorizontalAlignment( SwingConstants.RIGHT );
		GridBagConstraints gbc_txtTreeHeight = new GridBagConstraints();
		gbc_txtTreeHeight.anchor = GridBagConstraints.EAST;
		gbc_txtTreeHeight.insets = new Insets( 0, 5, 5, 5 );
		gbc_txtTreeHeight.gridx = 1;
		gbc_txtTreeHeight.gridy = 1;
		cTreeResolution.add( txtTreeHeight, gbc_txtTreeHeight );

		String t = SwingUIUtils.toHTML( "Area that the tree visualization has to work with." );
		lblTreeWidth.setToolTipText( t );
		txtTreeWidth.setToolTipText( t );
		lblTreeHeight.setToolTipText( t );
		txtTreeHeight.setToolTipText( t );

		JPanel cPointResolution = new JPanel();
		cPointResolution.setBorder(
			new TitledBorder(
				UIManager.getBorder( "TitledBorder.border" ),
				"Point visualization resolution"
			)
		);

		GridBagConstraints gbc_cPointResolution = new GridBagConstraints();
		gbc_cPointResolution.insets = new Insets( 0, 5, 5, 5 );
		gbc_cPointResolution.fill = GridBagConstraints.BOTH;
		gbc_cPointResolution.gridx = 0;
		gbc_cPointResolution.gridy = 3;
		cGeneral.add( cPointResolution, gbc_cPointResolution );
		GridBagLayout gbl_cPointResolution = new GridBagLayout();
		gbl_cPointResolution.columnWidths = new int[] { 0, 150, 0 };
		gbl_cPointResolution.rowHeights = new int[] { 0, 0, 0 };
		gbl_cPointResolution.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gbl_cPointResolution.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
		cPointResolution.setLayout( gbl_cPointResolution );

		JLabel lblPointWidth = new JLabel( "Width:" );
		GridBagConstraints gbc_lblPointWidth = new GridBagConstraints();
		gbc_lblPointWidth.anchor = GridBagConstraints.WEST;
		gbc_lblPointWidth.insets = new Insets( 5, 5, 5, 0 );
		gbc_lblPointWidth.gridx = 0;
		gbc_lblPointWidth.gridy = 0;
		cPointResolution.add( lblPointWidth, gbc_lblPointWidth );

		txtPointWidth = new JTextField();
		txtPointWidth.setColumns( 6 );
		txtPointWidth.setHorizontalAlignment( SwingConstants.RIGHT );
		GridBagConstraints gbc_txtPointWidth = new GridBagConstraints();
		gbc_txtPointWidth.anchor = GridBagConstraints.EAST;
		gbc_txtPointWidth.insets = new Insets( 5, 0, 5, 5 );
		gbc_txtPointWidth.gridx = 1;
		gbc_txtPointWidth.gridy = 0;
		cPointResolution.add( txtPointWidth, gbc_txtPointWidth );

		JLabel lblPointHeight = new JLabel( "Height:" );
		GridBagConstraints gbc_lblPointHeight = new GridBagConstraints();
		gbc_lblPointHeight.anchor = GridBagConstraints.WEST;
		gbc_lblPointHeight.insets = new Insets( 0, 5, 0, 0 );
		gbc_lblPointHeight.gridx = 0;
		gbc_lblPointHeight.gridy = 1;
		cPointResolution.add( lblPointHeight, gbc_lblPointHeight );

		txtPointHeight = new JTextField();
		txtPointHeight.setColumns( 6 );
		txtPointHeight.setHorizontalAlignment( SwingConstants.RIGHT );
		GridBagConstraints gbc_txtPointHeight = new GridBagConstraints();
		gbc_txtPointHeight.anchor = GridBagConstraints.EAST;
		gbc_txtPointHeight.insets = new Insets( 0, 5, 5, 5 );
		gbc_txtPointHeight.gridx = 1;
		gbc_txtPointHeight.gridy = 1;
		cPointResolution.add( txtPointHeight, gbc_txtPointHeight );

		t = SwingUIUtils.toHTML( "Resolution of the node instances visualization." );
		lblPointWidth.setToolTipText( t );
		txtPointWidth.setToolTipText( t );
		lblPointHeight.setToolTipText( t );
		txtPointHeight.setToolTipText( t );

		// Apply current config values
		HVConfig cfg = context.getConfig();

		listLAF.setSelectedItem( cfg.getPreferredLookAndFeel() );

		txtTreeWidth.setDocument( new NumberDocument( 4 ) );
		txtTreeHeight.setDocument( new NumberDocument( 4 ) );
		txtPointWidth.setDocument( new NumberDocument( 4 ) );
		txtPointHeight.setDocument( new NumberDocument( 4 ) );

		txtTreeWidth.setText( "" + cfg.getTreeWidth() );
		txtTreeHeight.setText( "" + cfg.getTreeHeight() );
		txtPointWidth.setText( "" + cfg.getInstanceWidth() );
		txtPointHeight.setText( "" + cfg.getInstanceHeight() );
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

		JLabel lblSelectedNodeColor = new JLabel( "Selected node color:" );
		GridBagConstraints gbc_lblSelectedNodeColor = new GridBagConstraints();
		gbc_lblSelectedNodeColor.anchor = GridBagConstraints.WEST;
		gbc_lblSelectedNodeColor.insets = new Insets( 5, 5, 5, 5 );
		gbc_lblSelectedNodeColor.gridx = 0;
		gbc_lblSelectedNodeColor.gridy = 0;
		cColors.add( lblSelectedNodeColor, gbc_lblSelectedNodeColor );

		lblColorSelectedNode = new JLabel();
		GridBagConstraints gbc_lblColorSelectedNode = new GridBagConstraints();
		gbc_lblColorSelectedNode.anchor = GridBagConstraints.EAST;
		gbc_lblColorSelectedNode.fill = GridBagConstraints.VERTICAL;
		gbc_lblColorSelectedNode.insets = new Insets( 5, 0, 5, 5 );
		gbc_lblColorSelectedNode.gridx = 1;
		gbc_lblColorSelectedNode.gridy = 0;
		cColors.add( lblColorSelectedNode, gbc_lblColorSelectedNode );
		lblColorSelectedNode.setOpaque( true );
		lblColorSelectedNode.setBorder( new LineBorder( Color.lightGray ) );
		lblColorSelectedNode.setPreferredSize( colorLabelDim );

		JLabel lblChildGroupColor = new JLabel( "Child group color:" );
		GridBagConstraints gbc_lblChildGroupColor = new GridBagConstraints();
		gbc_lblChildGroupColor.insets = new Insets( 0, 5, 5, 5 );
		gbc_lblChildGroupColor.anchor = GridBagConstraints.WEST;
		gbc_lblChildGroupColor.gridx = 0;
		gbc_lblChildGroupColor.gridy = 1;
		cColors.add( lblChildGroupColor, gbc_lblChildGroupColor );

		lblColorChildGroup = new JLabel();
		GridBagConstraints gbc_lblColorChildGroup = new GridBagConstraints();
		gbc_lblColorChildGroup.anchor = GridBagConstraints.EAST;
		gbc_lblColorChildGroup.fill = GridBagConstraints.VERTICAL;
		gbc_lblColorChildGroup.insets = new Insets( 0, 0, 5, 5 );
		gbc_lblColorChildGroup.gridx = 1;
		gbc_lblColorChildGroup.gridy = 1;
		cColors.add( lblColorChildGroup, gbc_lblColorChildGroup );
		lblColorChildGroup.setOpaque( true );
		lblColorChildGroup.setBorder( new LineBorder( Color.lightGray ) );
		lblColorChildGroup.setPreferredSize( colorLabelDim );

		JLabel lblParentGroupColor = new JLabel( "Parent group color:" );
		GridBagConstraints gbc_lblParentGroupColor = new GridBagConstraints();
		gbc_lblParentGroupColor.insets = new Insets( 0, 5, 5, 5 );
		gbc_lblParentGroupColor.anchor = GridBagConstraints.WEST;
		gbc_lblParentGroupColor.gridx = 0;
		gbc_lblParentGroupColor.gridy = 2;
		cColors.add( lblParentGroupColor, gbc_lblParentGroupColor );

		lblColorParentGroup = new JLabel();
		GridBagConstraints gbc_lblColorParentGroup = new GridBagConstraints();
		gbc_lblColorParentGroup.anchor = GridBagConstraints.EAST;
		gbc_lblColorParentGroup.fill = GridBagConstraints.VERTICAL;
		gbc_lblColorParentGroup.insets = new Insets( 0, 0, 5, 5 );
		gbc_lblColorParentGroup.gridx = 1;
		gbc_lblColorParentGroup.gridy = 2;
		cColors.add( lblColorParentGroup, gbc_lblColorParentGroup );
		lblColorParentGroup.setOpaque( true );
		lblColorParentGroup.setBorder( new LineBorder( Color.lightGray ) );
		lblColorParentGroup.setPreferredSize( colorLabelDim );

		JLabel lblAncestorGroupColor = new JLabel( "Ancestor group color:" );
		GridBagConstraints gbc_lblAncestorGroupColor = new GridBagConstraints();
		gbc_lblAncestorGroupColor.anchor = GridBagConstraints.WEST;
		gbc_lblAncestorGroupColor.insets = new Insets( 0, 5, 5, 5 );
		gbc_lblAncestorGroupColor.gridx = 0;
		gbc_lblAncestorGroupColor.gridy = 3;
		cColors.add( lblAncestorGroupColor, gbc_lblAncestorGroupColor );

		lblColorAncestorGroup = new JLabel();
		GridBagConstraints gbc_lblColorAncestorGroup = new GridBagConstraints();
		gbc_lblColorAncestorGroup.anchor = GridBagConstraints.EAST;
		gbc_lblColorAncestorGroup.fill = GridBagConstraints.VERTICAL;
		gbc_lblColorAncestorGroup.insets = new Insets( 0, 0, 5, 5 );
		gbc_lblColorAncestorGroup.gridx = 1;
		gbc_lblColorAncestorGroup.gridy = 3;
		cColors.add( lblColorAncestorGroup, gbc_lblColorAncestorGroup );
		lblColorAncestorGroup.setOpaque( true );
		lblColorAncestorGroup.setBorder( new LineBorder( Color.lightGray ) );
		lblColorAncestorGroup.setPreferredSize( colorLabelDim );

		JLabel lblOtherGroupColor = new JLabel( "Other group color:" );
		GridBagConstraints gbc_lblOtherGroupColor = new GridBagConstraints();
		gbc_lblOtherGroupColor.anchor = GridBagConstraints.WEST;
		gbc_lblOtherGroupColor.insets = new Insets( 0, 5, 5, 5 );
		gbc_lblOtherGroupColor.gridx = 0;
		gbc_lblOtherGroupColor.gridy = 4;
		cColors.add( lblOtherGroupColor, gbc_lblOtherGroupColor );

		lblColorOtherGroup = new JLabel();
		GridBagConstraints gbc_lblColorOtherGroup = new GridBagConstraints();
		gbc_lblColorOtherGroup.anchor = GridBagConstraints.EAST;
		gbc_lblColorOtherGroup.fill = GridBagConstraints.VERTICAL;
		gbc_lblColorOtherGroup.insets = new Insets( 0, 0, 5, 5 );
		gbc_lblColorOtherGroup.gridx = 1;
		gbc_lblColorOtherGroup.gridy = 4;
		cColors.add( lblColorOtherGroup, gbc_lblColorOtherGroup );
		lblColorOtherGroup.setOpaque( true );
		lblColorOtherGroup.setBorder( new LineBorder( Color.lightGray ) );
		lblColorOtherGroup.setPreferredSize( colorLabelDim );

		JLabel lblBackgroundColor = new JLabel( "Background color:" );
		GridBagConstraints gbc_lblBackgroundColor = new GridBagConstraints();
		gbc_lblBackgroundColor.anchor = GridBagConstraints.WEST;
		gbc_lblBackgroundColor.insets = new Insets( 0, 5, 5, 5 );
		gbc_lblBackgroundColor.gridx = 0;
		gbc_lblBackgroundColor.gridy = 5;
		cColors.add( lblBackgroundColor, gbc_lblBackgroundColor );

		lblColorBackground = new JLabel();
		GridBagConstraints gbc_lblColorBackground = new GridBagConstraints();
		gbc_lblColorBackground.anchor = GridBagConstraints.EAST;
		gbc_lblColorBackground.fill = GridBagConstraints.VERTICAL;
		gbc_lblColorBackground.insets = new Insets( 0, 0, 5, 5 );
		gbc_lblColorBackground.gridx = 1;
		gbc_lblColorBackground.gridy = 5;
		cColors.add( lblColorBackground, gbc_lblColorBackground );
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

		JButton btnConfirm = new JButton( "Confirm" );
		GridBagConstraints gbc_btnConfirm = new GridBagConstraints();
		gbc_btnConfirm.anchor = GridBagConstraints.EAST;
		gbc_btnConfirm.insets = new Insets( 5, 0, 5, 5 );
		gbc_btnConfirm.gridx = 0;
		gbc_btnConfirm.gridy = 0;
		cButtons.add( btnConfirm, gbc_btnConfirm );

		JButton btnCancel = new JButton( "Cancel" );
		GridBagConstraints gbc_btnCancel = new GridBagConstraints();
		gbc_btnCancel.insets = new Insets( 5, 0, 5, 5 );
		gbc_btnCancel.gridx = 1;
		gbc_btnCancel.gridy = 0;
		cButtons.add( btnCancel, gbc_btnCancel );

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
		newConfig.setTreeWidth( Integer.parseInt( getText( txtTreeWidth ) ) );
		newConfig.setTreeHeight( Integer.parseInt( getText( txtTreeHeight ) ) );
		newConfig.setPointWidth( Integer.parseInt( getText( txtPointWidth ) ) );
		newConfig.setPointHeight( Integer.parseInt( getText( txtPointHeight ) ) );
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
