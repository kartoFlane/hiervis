package pl.pwr.hiervis.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JPanel;

import pl.pwr.hiervis.core.HVContext;
import pl.pwr.hiervis.hierarchy.LoadedHierarchy;
import pl.pwr.hiervis.util.SwingUIUtils;
import pl.pwr.hiervis.util.ui.JMultilineLabel;


@SuppressWarnings("serial")
public class FileLoadingOptionsDialog extends JDialog
{
	private LoadedHierarchy.Options options = null;

	private JCheckBox cboxTrueClass;
	private JCheckBox cboxInstanceName;
	private JCheckBox cboxDataNames;
	private JCheckBox cboxFillGaps;


	public FileLoadingOptionsDialog( HVContext context, Window frame )
	{
		super( frame, "File Loading Options" );
		setResizable( false );

		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		setModal( true );

		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWidths = new int[] { 350 };
		gridBagLayout.columnWeights = new double[] { 1.0 };
		gridBagLayout.rowWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
		getContentPane().setLayout( gridBagLayout );

		createOptionsPanel( context );
		createButtonPanel( context );

		SwingUIUtils.installEscapeCloseOperation( this );

		// Try to coax the pane into correct dimensions...
		revalidate();
		setPreferredSize( getPreferredSize() );
		pack();
	}

	private void createOptionsPanel( HVContext context )
	{
		JPanel cOptions = new JPanel();
		GridBagConstraints gbc_cOptions = new GridBagConstraints();
		gbc_cOptions.fill = GridBagConstraints.BOTH;
		gbc_cOptions.insets = new Insets( 0, 0, 5, 0 );
		gbc_cOptions.gridx = 0;
		gbc_cOptions.gridy = 0;
		getContentPane().add( cOptions, gbc_cOptions );
		GridBagLayout gbl_cOptions = new GridBagLayout();
		gbl_cOptions.columnWidths = new int[] { 350, 0 };
		gbl_cOptions.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_cOptions.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		cOptions.setLayout( gbl_cOptions );

		cboxTrueClass = new JCheckBox( "True Class" );
		GridBagConstraints gbc_cboxTrueClass = new GridBagConstraints();
		gbc_cboxTrueClass.fill = GridBagConstraints.HORIZONTAL;
		gbc_cboxTrueClass.insets = new Insets( 5, 5, 5, 0 );
		gbc_cboxTrueClass.gridx = 0;
		gbc_cboxTrueClass.gridy = 0;
		cOptions.add( cboxTrueClass, gbc_cboxTrueClass );

		JMultilineLabel lblTrueClass = new JMultilineLabel(
			"Whether the input file contains a column specifying the true class of each instance."
		);
		lblTrueClass.setRows( 3 );
		GridBagConstraints gbc_lblTrueClass = new GridBagConstraints();
		gbc_lblTrueClass.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblTrueClass.insets = new Insets( 0, 5, 10, 5 );
		gbc_lblTrueClass.gridx = 0;
		gbc_lblTrueClass.gridy = 1;
		cOptions.add( lblTrueClass, gbc_lblTrueClass );

		cboxInstanceName = new JCheckBox( "Instance Name" );
		GridBagConstraints gbc_cboxInstanceName = new GridBagConstraints();
		gbc_cboxInstanceName.fill = GridBagConstraints.HORIZONTAL;
		gbc_cboxInstanceName.insets = new Insets( 0, 5, 5, 0 );
		gbc_cboxInstanceName.gridx = 0;
		gbc_cboxInstanceName.gridy = 2;
		cOptions.add( cboxInstanceName, gbc_cboxInstanceName );

		JMultilineLabel lblInstanceName = new JMultilineLabel(
			"Whether the input file contains a column specifying instance name of each instance."
		);
		lblInstanceName.setRows( 3 );
		GridBagConstraints gbc_lblInstanceName = new GridBagConstraints();
		gbc_lblInstanceName.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblInstanceName.insets = new Insets( 0, 5, 10, 5 );
		gbc_lblInstanceName.gridx = 0;
		gbc_lblInstanceName.gridy = 3;
		cOptions.add( lblInstanceName, gbc_lblInstanceName );

		cboxDataNames = new JCheckBox( "Data Names" );
		GridBagConstraints gbc_cboxDataNames = new GridBagConstraints();
		gbc_cboxDataNames.fill = GridBagConstraints.HORIZONTAL;
		gbc_cboxDataNames.insets = new Insets( 0, 5, 5, 0 );
		gbc_cboxDataNames.gridx = 0;
		gbc_cboxDataNames.gridy = 4;
		cOptions.add( cboxDataNames, gbc_cboxDataNames );

		JMultilineLabel lblDataNames = new JMultilineLabel(
			"Whether the input file contains names of each data column."
		);
		lblDataNames.setRows( 3 );
		GridBagConstraints gbc_lblDataNames = new GridBagConstraints();
		gbc_lblDataNames.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblDataNames.insets = new Insets( 0, 5, 10, 5 );
		gbc_lblDataNames.gridx = 0;
		gbc_lblDataNames.gridy = 5;
		cOptions.add( lblDataNames, gbc_lblDataNames );

		cboxFillGaps = new JCheckBox( "Fill Breadth Gaps" );
		GridBagConstraints gbc_cboxFillGaps = new GridBagConstraints();
		gbc_cboxFillGaps.fill = GridBagConstraints.HORIZONTAL;
		gbc_cboxFillGaps.insets = new Insets( 0, 5, 5, 0 );
		gbc_cboxFillGaps.gridx = 0;
		gbc_cboxFillGaps.gridy = 6;
		cOptions.add( cboxFillGaps, gbc_cboxFillGaps );

		JMultilineLabel lblFillGaps = new JMultilineLabel(
			"Whether the hierarchy constructed from the input file should be " +
				"fixed to account for possible gaps in instance siblings."
		);
		lblDataNames.setRows( 3 );
		GridBagConstraints gbc_lblFillGaps = new GridBagConstraints();
		gbc_lblFillGaps.fill = GridBagConstraints.HORIZONTAL;
		gbc_lblFillGaps.insets = new Insets( 0, 5, 10, 5 );
		gbc_lblFillGaps.gridx = 0;
		gbc_lblFillGaps.gridy = 7;
		cOptions.add( lblFillGaps, gbc_lblFillGaps );

		// Apply previous options
		LoadedHierarchy.Options prevOptions = context.getHierarchyOptions();

		cboxTrueClass.setSelected( prevOptions.hasTrueClassAttribute );
		cboxInstanceName.setSelected( prevOptions.hasTnstanceNameAttribute );
		cboxDataNames.setSelected( prevOptions.hasColumnHeader );
		cboxFillGaps.setSelected( prevOptions.isFillBreadthGaps );
	}

	private void createButtonPanel( HVContext context )
	{
		JPanel cButtons = new JPanel();
		GridBagConstraints gbc_cButtons = new GridBagConstraints();
		gbc_cButtons.fill = GridBagConstraints.BOTH;
		gbc_cButtons.gridx = 0;
		gbc_cButtons.gridy = 1;
		getContentPane().add( cButtons, gbc_cButtons );

		GridBagLayout gbl_cButtons = new GridBagLayout();
		gbl_cButtons.columnWidths = new int[] { 0, 0 };
		gbl_cButtons.rowHeights = new int[] { 0, 0 };
		gbl_cButtons.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gbl_cButtons.rowWeights = new double[] { 0.0, Double.MIN_VALUE };
		cButtons.setLayout( gbl_cButtons );

		JButton btnConfirm = new JButton( "Confirm" );
		GridBagConstraints gbc_btnConfirm = new GridBagConstraints();
		gbc_btnConfirm.insets = new Insets( 5, 0, 5, 0 );
		gbc_btnConfirm.gridx = 0;
		gbc_btnConfirm.gridy = 0;
		cButtons.add( btnConfirm, gbc_btnConfirm );

		getRootPane().setDefaultButton( btnConfirm );
		btnConfirm.requestFocus();

		btnConfirm.addActionListener(
			( e ) -> {
				updateOptions( context );
				dispatchEvent( new WindowEvent( this, WindowEvent.WINDOW_CLOSING ) );
			}
		);
	}

	private void updateOptions( HVContext context )
	{
		options = new LoadedHierarchy.Options(
			cboxInstanceName.isSelected(),
			cboxTrueClass.isSelected(),
			cboxDataNames.isSelected(),
			cboxFillGaps.isSelected(),
			false // TODO
		);
	}

	/**
	 * @return the new options instance, if the user exited the dialog by
	 *         pressing the 'Confirm' button. Null otherwise.
	 */
	public LoadedHierarchy.Options getOptions()
	{
		return options;
	}
}
