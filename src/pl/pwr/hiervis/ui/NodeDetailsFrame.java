package pl.pwr.hiervis.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import basic_hierarchy.interfaces.Node;
import pl.pwr.hiervis.core.HVContext;
import pl.pwr.hiervis.ui.components.HKOptionsPanel;
import pl.pwr.hiervis.util.GridBagConstraintsBuilder;
import pl.pwr.hiervis.util.SwingUIUtils;


@SuppressWarnings("serial")
public class NodeDetailsFrame extends JFrame
{
	private static final Logger logHK = LogManager.getLogger( NodeDetailsFrame.class );

	private HVContext context;
	private Node node;

	private JTabbedPane cTabs;


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

		HKOptionsPanel hkPanel = new HKOptionsPanel( context, node, logHK );
		hkPanel.setupDefaultValues();

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.getVerticalScrollBar().setUnitIncrement( 16 );
		scrollPane.setViewportView( hkPanel );

		cHkTabContainer.add( scrollPane, builder.fill().position( 0, 0 ).build() );

		JButton btnGenerate = new JButton( "Generate" );
		btnGenerate.addActionListener( e -> hkPanel.generate() );
		cHkTabContainer.add( btnGenerate, builder.insets( 5 ).fillHorizontal().position( 0, 1 ).build() );
	}
}

