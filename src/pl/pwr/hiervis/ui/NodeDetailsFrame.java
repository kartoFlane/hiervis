package pl.pwr.hiervis.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Window;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.text.NumberFormatter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import basic_hierarchy.interfaces.Hierarchy;
import basic_hierarchy.interfaces.Node;
import pl.pwr.hiervis.HierarchyVisualizer;
import pl.pwr.hiervis.core.HKPlusPlusWrapper;
import pl.pwr.hiervis.core.HVContext;
import pl.pwr.hiervis.util.GridBagConstraintsBuilder;
import pl.pwr.hiervis.util.HierarchyUtils;
import pl.pwr.hiervis.util.SwingUIUtils;


@SuppressWarnings("serial")
public class NodeDetailsFrame extends JFrame
{
	private static final Logger logHK = LogManager.getLogger( NodeDetailsFrame.class );

	private HVContext context;
	private Node node;

	private HKPlusPlusWrapper wrapper;

	private JTabbedPane cTabs;

	private JTextField txtClusters;
	private JTextField txtIterations;
	private JTextField txtRepeats;
	private JTextField txtDendrogram;
	private JTextField txtMaxNodes;
	private JTextField txtEpsilon;
	private JTextField txtLittleVal;

	private JCheckBox cboxTrueClass;
	private JCheckBox cboxInstanceNames;
	private JCheckBox cboxDiagonalMatrix;
	private JCheckBox cboxNoStaticCenter;
	private JCheckBox cboxGenerateImages;


	public NodeDetailsFrame( HVContext context, Window frame, Node node, String subtitle )
	{
		super( "Node Details: " + node.getId() + ( subtitle == null ? "" : ( " [ " + subtitle + " ]" ) ) );

		this.context = context;
		this.node = node;

		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		setMinimumSize( new Dimension( 400, 200 ) );
		setSize( 400, 500 );

		createGUI();

		SwingUIUtils.addCloseCallback( frame, this );
		SwingUIUtils.installEscapeCloseOperation( this );
	}

	// ----------------------------------------------------------------------------------------
	// GUI creation methods

	private void createGUI()
	{
		createTabPanel();

		setupDefaultValues();
	}

	private void createTabPanel()
	{
		cTabs = new JTabbedPane( JTabbedPane.TOP );
		getContentPane().add( cTabs, BorderLayout.CENTER );

		createHKPlusPlusTab( cTabs );
		createNodeDetailsTab( cTabs );
	}

	private void createNodeDetailsTab( JTabbedPane tabPane )
	{
		JPanel cNodeDetails = new JPanel();
		tabPane.addTab( "Node Details", null, cNodeDetails, null );

		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 0, 0 };
		gridBagLayout.rowHeights = new int[] { 0, 0 };
		gridBagLayout.columnWeights = new double[] { 0.0, 1.0 };
		gridBagLayout.rowWeights = new double[] { 0.0, 1.0 };
		cNodeDetails.setLayout( gridBagLayout );
	}

	private void createHKPlusPlusTab( JTabbedPane tabPane )
	{
		GridBagConstraintsBuilder builder = new GridBagConstraintsBuilder();

		JPanel cHkTabContainer = new JPanel();
		tabPane.addTab( "HK++", null, cHkTabContainer, null );

		GridBagLayout layout = new GridBagLayout();
		layout.columnWidths = new int[] { 0 };
		layout.rowHeights = new int[] { 0, 0 };
		layout.columnWeights = new double[] { 1.0 };
		layout.rowWeights = new double[] { 1.0, 0.0 };
		cHkTabContainer.setLayout( layout );

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.getVerticalScrollBar().setUnitIncrement( 16 );
		scrollPane.setViewportView( createHKPlusPlusPanel() );

		cHkTabContainer.add( scrollPane, builder.fill().position( 0, 0 ).build() );

		JButton btnGenerate = new JButton( "Generate" );
		btnGenerate.addActionListener( e -> generate() );
		cHkTabContainer.add( btnGenerate, builder.insets( 5 ).fillHorizontal().position( 0, 1 ).build() );
	}

	private JPanel createHKPlusPlusPanel()
	{
		GridBagConstraintsBuilder builder = new GridBagConstraintsBuilder();

		JPanel cHK = new JPanel();

		GridBagLayout layout = new GridBagLayout();
		layout.columnWidths = new int[] { 0, 0 };
		layout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		layout.columnWeights = new double[] { 1.0, 0.0 };
		layout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0 };
		cHK.setLayout( layout );

		JLabel lblClusters = new JLabel( "[-k] Clusters:" );
		txtClusters = buildNumberTextField();
		cHK.add( lblClusters, builder.insets( 5 ).anchorWest().position( 0, 0 ).build() );
		cHK.add( txtClusters, builder.insets( 5 ).anchorEast().position( 1, 0 ).build() );

		JLabel lblIterations = new JLabel( "[-n] Iterations:" );
		txtIterations = buildNumberTextField();
		cHK.add( lblIterations, builder.insets( 5 ).anchorWest().position( 0, 1 ).build() );
		cHK.add( txtIterations, builder.insets( 5 ).anchorEast().position( 1, 1 ).build() );

		JLabel lblRepeats = new JLabel( "[-r] Repeats:" );
		txtRepeats = buildNumberTextField();
		cHK.add( lblRepeats, builder.insets( 5 ).anchorWest().position( 0, 2 ).build() );
		cHK.add( txtRepeats, builder.insets( 5 ).anchorEast().position( 1, 2 ).build() );

		JLabel lblDendrogram = new JLabel( "[-s] Max Dendrogram Height:" );
		txtDendrogram = buildNumberTextField();
		cHK.add( lblDendrogram, builder.insets( 5 ).anchorWest().position( 0, 3 ).build() );
		cHK.add( txtDendrogram, builder.insets( 5 ).anchorEast().position( 1, 3 ).build() );

		JLabel lblMaxNodes = new JLabel( "[-w] Max Generated Nodes:" );
		txtMaxNodes = buildNumberTextField();
		cHK.add( lblMaxNodes, builder.insets( 5 ).anchorWest().position( 0, 4 ).build() );
		cHK.add( txtMaxNodes, builder.insets( 5 ).anchorEast().position( 1, 4 ).build() );

		JLabel lblEpsilon = new JLabel( "[-e] Epsilon:" );
		txtEpsilon = buildNumberTextField();
		cHK.add( lblEpsilon, builder.insets( 5 ).anchorWest().position( 0, 5 ).build() );
		cHK.add( txtEpsilon, builder.insets( 5 ).anchorEast().position( 1, 5 ).build() );

		JLabel lblLittleVal = new JLabel( "[-l] Little Value:" );
		txtLittleVal = buildNumberTextField();
		cHK.add( lblLittleVal, builder.insets( 5 ).anchorWest().position( 0, 6 ).build() );
		cHK.add( txtLittleVal, builder.insets( 5 ).anchorEast().position( 1, 6 ).build() );

		JLabel lblTrueClass = new JLabel( "[-c] Include True Class:" );
		cboxTrueClass = new JCheckBox();
		cboxTrueClass.setHorizontalAlignment( SwingConstants.CENTER );
		cHK.add( lblTrueClass, builder.insets( 5 ).anchorWest().position( 0, 7 ).build() );
		cHK.add( cboxTrueClass, builder.insets( 5 ).anchorCenter().fill().position( 1, 7 ).build() );

		JLabel lblInstanceNames = new JLabel( "[-in] Include Instance Names:" );
		cboxInstanceNames = new JCheckBox();
		cboxInstanceNames.setHorizontalAlignment( SwingConstants.CENTER );
		cHK.add( lblInstanceNames, builder.insets( 5 ).anchorWest().position( 0, 8 ).build() );
		cHK.add( cboxInstanceNames, builder.insets( 5 ).anchorCenter().fill().position( 1, 8 ).build() );

		JLabel lblDiagonalMatrix = new JLabel( "[-dm] Use Diagonal Matrix:" );
		cboxDiagonalMatrix = new JCheckBox();
		cboxDiagonalMatrix.setHorizontalAlignment( SwingConstants.CENTER );
		cHK.add( lblDiagonalMatrix, builder.insets( 5 ).anchorWest().position( 0, 9 ).build() );
		cHK.add( cboxDiagonalMatrix, builder.insets( 5 ).anchorCenter().fill().position( 1, 9 ).build() );

		JLabel lblNoStaticCenter = new JLabel( "[-ds] Disable Static Center:" );
		cboxNoStaticCenter = new JCheckBox();
		cboxNoStaticCenter.setHorizontalAlignment( SwingConstants.CENTER );
		cHK.add( lblNoStaticCenter, builder.insets( 5 ).anchorWest().position( 0, 10 ).build() );
		cHK.add( cboxNoStaticCenter, builder.insets( 5 ).anchorCenter().fill().position( 1, 10 ).build() );

		JLabel lblGenerateImages = new JLabel( "[-gi] Generate Images:" );
		cboxGenerateImages = new JCheckBox();
		cboxGenerateImages.setHorizontalAlignment( SwingConstants.CENTER );
		cHK.add( lblGenerateImages, builder.insets( 5 ).anchorWest().position( 0, 11 ).build() );
		cHK.add( cboxGenerateImages, builder.insets( 5 ).anchorCenter().fill().position( 1, 11 ).build() );

		return cHK;
	}

	private void setupDefaultValues()
	{
		txtClusters.setText( "2" );
		txtIterations.setText( "10" );
		txtRepeats.setText( "10" );
		txtDendrogram.setText( "2" );
		txtMaxNodes.setText( "-1" );
		txtEpsilon.setText( "10" );
		txtLittleVal.setText( "5" );

		cboxTrueClass.setSelected( false );
		cboxInstanceNames.setSelected( false );
		cboxDiagonalMatrix.setSelected( false );
		cboxNoStaticCenter.setSelected( false );
		cboxGenerateImages.setSelected( false );

		cboxTrueClass.setEnabled( context.getConfig().hasTrueClassAttribute() );
		cboxInstanceNames.setEnabled( context.getConfig().hasInstanceNameAttribute() );
	}

	private static JTextField buildNumberTextField()
	{
		// TODO: Verifying user input
		NumberFormatter formatter = new NumberFormatter( new DecimalFormat( "0" ) );
		formatter.setValueClass( Integer.class );
		formatter.setMinimum( Integer.MIN_VALUE );
		formatter.setMaximum( Integer.MAX_VALUE );
		formatter.setAllowsInvalid( false );

		JFormattedTextField result = new JFormattedTextField();
		result.setHorizontalAlignment( SwingConstants.RIGHT );
		result.setColumns( 10 );

		return result;
	}

	/**
	 * Gets the text of the specified text field, or "0", if the text field is empty.
	 */
	private static String getText( JTextField text )
	{
		String t = text.getText();
		return t == null || t.equals( "" ) ? "0" : t;
	}

	private void generate()
	{
		int clusters = Integer.parseInt( getText( txtClusters ) ); // -k
		int iterations = Integer.parseInt( getText( txtIterations ) ); // -n
		int repeats = Integer.parseInt( getText( txtRepeats ) ); // -r
		int dendrogramHeight = Integer.parseInt( getText( txtDendrogram ) ); // -s
		int maxNodes = Integer.parseInt( getText( txtMaxNodes ) ); // -w
		int epsilon = Integer.parseInt( getText( txtEpsilon ) ); // -e
		int littleVal = Integer.parseInt( getText( txtLittleVal ) ); // -l

		if ( maxNodes < 0 ) {
			maxNodes = Integer.MAX_VALUE;
		}

		try {
			wrapper = new HKPlusPlusWrapper();
			wrapper.subprocessFinished.addListener( this::onSubprocessFinished );
			wrapper.subprocessAborted.addListener( this::onSubprocessAborted );

			logHK.trace( "Preparing input file..." );
			wrapper.prepareInputFile(
				context.getHierarchy(), node,
				cboxTrueClass.isSelected(), cboxInstanceNames.isSelected()
			);

			logHK.trace( "Starting..." );
			wrapper.start(
				this,
				cboxTrueClass.isSelected(), cboxInstanceNames.isSelected(),
				cboxDiagonalMatrix.isSelected(), cboxNoStaticCenter.isSelected(),
				cboxGenerateImages.isSelected(),
				epsilon, littleVal,
				clusters, iterations, repeats,
				dendrogramHeight, maxNodes
			);
		}
		catch ( IOException ex ) {
			logHK.error( ex );
		}
	}

	private String getParameterString()
	{
		int clusters = Integer.parseInt( getText( txtClusters ) ); // -k
		int iterations = Integer.parseInt( getText( txtIterations ) ); // -n
		int repeats = Integer.parseInt( getText( txtRepeats ) ); // -r
		int dendrogramHeight = Integer.parseInt( getText( txtDendrogram ) ); // -s
		int maxNodes = Integer.parseInt( getText( txtMaxNodes ) ); // -w
		int epsilon = Integer.parseInt( getText( txtEpsilon ) ); // -e
		int littleVal = Integer.parseInt( getText( txtLittleVal ) ); // -l

		String maxNodesStr = maxNodes < 0 ? "MAX_INT" : ( "" + maxNodes );

		return String.format(
			"%s / -k %s / -n %s / -r %s / -s %s / -e %s / -l %s / -w %s",
			node.getId(),
			clusters, iterations,
			repeats, dendrogramHeight,
			epsilon, littleVal,
			maxNodesStr
		);
	}

	private void onSubprocessAborted( Void v )
	{
		logHK.trace( "Aborted." );
		wrapper = null;
	}

	private void onSubprocessFinished( int exitCode )
	{
		if ( exitCode == 0 ) {
			logHK.trace( "Finished successfully." );

			try {
				Hierarchy outputHierarchy = wrapper.getOutputHierarchy(
					cboxTrueClass.isSelected(),
					cboxInstanceNames.isSelected(),
					false
				);

				Hierarchy finalHierarchy = HierarchyUtils.merge( outputHierarchy, context.getHierarchy(), node.getId() );

				File tmp = File.createTempFile( "hv-h-", ".tmp.csv" );
				logHK.trace( "Saving merged hierarchy to: " + tmp.getAbsolutePath() );
				HierarchyUtils.save(
					tmp.getAbsolutePath(), finalHierarchy,
					true, cboxTrueClass.isSelected(), cboxInstanceNames.isSelected(), true
				);

				// TODO: Check if the selection was an internal node or leaf node, and decide where to load the new hierarchy based on that
				HierarchyVisualizer.spawnNewInstance( getParameterString(), tmp );
			}
			catch ( IOException ex ) {
				logHK.error( ex );
			}
		}
		else {
			logHK.error( "Failed! Error code: " + exitCode );
		}

		wrapper = null;
	}
}

