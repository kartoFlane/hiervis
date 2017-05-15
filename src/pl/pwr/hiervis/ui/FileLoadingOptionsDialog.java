package pl.pwr.hiervis.ui;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import pl.pwr.hiervis.core.HVContext;
import pl.pwr.hiervis.hierarchy.LoadedHierarchy;
import pl.pwr.hiervis.util.SwingUIUtils;


@SuppressWarnings("serial")
public class FileLoadingOptionsDialog extends JDialog
{
	private static final int _prefWidth = 350;

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
		gridBagLayout.columnWidths = new int[] { _prefWidth };
		gridBagLayout.columnWeights = new double[] { 1.0 };
		gridBagLayout.rowWeights = new double[] { 1.0, 0.0, Double.MIN_VALUE };
		getContentPane().setLayout( gridBagLayout );

		createOptionsPanel( context );
		createButtonPanel( context );

		SwingUIUtils.installEscapeCloseOperation( this );

		pack();
	}

	private void createOptionsPanel( HVContext context )
	{
		JPanel cOptions = new JPanel();

		cOptions.setLayout( new BoxLayout( cOptions, BoxLayout.Y_AXIS ) );

		GridBagConstraints gbc_cOptions = new GridBagConstraints();
		gbc_cOptions.fill = GridBagConstraints.BOTH;
		gbc_cOptions.insets = new Insets( 5, 5, 5, 5 );
		gbc_cOptions.gridx = 0;
		gbc_cOptions.gridy = 0;
		getContentPane().add( cOptions, gbc_cOptions );

		cboxTrueClass = new JCheckBox( "True Class" );
		cOptions.add( cboxTrueClass );

		JLabel lblTrueClass = new JLabel(
			"<html>Whether the input file contains a column specifying the true class of each instance.</html>"
		);
		fixedWidthLabel( lblTrueClass, _prefWidth );
		cOptions.add( lblTrueClass );
		cOptions.add( Box.createVerticalStrut( 10 ) );

		cboxInstanceName = new JCheckBox( "Instance Name" );
		cOptions.add( cboxInstanceName );

		JLabel lblInstanceName = new JLabel(
			"<html>Whether the input file contains a column specifying instance name of each instance.</html>"
		);
		fixedWidthLabel( lblInstanceName, _prefWidth );
		cOptions.add( lblInstanceName );
		cOptions.add( Box.createVerticalStrut( 10 ) );

		cboxDataNames = new JCheckBox( "Data Names" );
		cOptions.add( cboxDataNames );

		JLabel lblDataNames = new JLabel(
			"<html>Whether the input file contains names of each data column.</html>"
		);
		fixedWidthLabel( lblDataNames, _prefWidth );
		cOptions.add( lblDataNames );
		cOptions.add( Box.createVerticalStrut( 10 ) );

		cboxFillGaps = new JCheckBox( "Fill Breadth Gaps" );
		cOptions.add( cboxFillGaps );

		JLabel lblFillGaps = new JLabel(
			"<html>Whether the hierarchy constructed from the input file should be " +
				"fixed to account for possible gaps in instance siblings.</html>"
		);
		fixedWidthLabel( lblFillGaps, _prefWidth );
		cOptions.add( lblFillGaps );
		cOptions.add( Box.createVerticalStrut( 10 ) );

		// Apply previous options
		LoadedHierarchy.Options prevOptions = context.getHierarchyOptions();

		cboxTrueClass.setSelected( prevOptions.hasTrueClassAttribute );
		cboxInstanceName.setSelected( prevOptions.hasInstanceNameAttribute );
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

	private static void fixedWidthLabel( JLabel lbl, int width )
	{
		Dimension d = lbl.getPreferredSize();
		lbl.setPreferredSize( new Dimension( width, ( 1 + d.width / width ) * d.height ) );
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
