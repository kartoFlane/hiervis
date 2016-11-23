package pl.pwr.hiervis.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import pl.pwr.hiervis.ui.components.HuePicker;
import pl.pwr.hiervis.ui.components.NumberDocument;
import pl.pwr.hiervis.ui.components.ShadePicker;
import pl.pwr.hiervis.util.HSV;
import pl.pwr.hiervis.util.SwingUIUtils;
import pl.pwr.hiervis.util.Utils;


/**
 * A dialog window for color selection.
 * Because the default Swing component for color selection is ugly as sin.
 * 
 * @author Tomasz Bachmiński
 *
 */
public class SquareColorPickerDialog extends JDialog
{
	private static final long serialVersionUID = 3476247193320539039L;
	private static final Logger log = LogManager.getLogger( SquareColorPickerDialog.class );

	private ShadePicker shadePicker;
	private HuePicker huePicker;
	private JLabel lblNewColor;
	private JLabel lblOldColor;

	private JTextField txtH;
	private JTextField txtS;
	private JTextField txtV;
	private JTextField txtR;
	private JTextField txtG;
	private JTextField txtB;

	/** Prevents listeners from reacting to events when true. */
	private boolean selectionUpdating = false;
	private HSV tempSelection = null;
	private HSV finalSelection = null;


	public SquareColorPickerDialog( Window parent, Color inputColor )
	{
		super( parent, "Color Picker" );
		setDefaultCloseOperation( DISPOSE_ON_CLOSE );
		setModal( true );
		setResizable( false );

		finalSelection = new HSV( inputColor );

		GridBagLayout gridBagLayout = new GridBagLayout();
		gridBagLayout.columnWeights = new double[] { 0.0, 0.0, 1.0 };
		gridBagLayout.rowWeights = new double[] { 0.0, 1.0 };
		getContentPane().setLayout( gridBagLayout );

		createPickerPanel();
		createToolsPanel( inputColor );
		createButtonsPanel();

		pack();
		setLocationRelativeTo( null );
		SwingUIUtils.installEscapeCloseOperation( this );

		setSelection( inputColor );
	}

	public void setSelection( HSV hsv )
	{
		selectionUpdating = true;

		tempSelection = new HSV( hsv );
		lblNewColor.setBackground( tempSelection.toColor() );
		shadePicker.setSelection(
			tempSelection.getSaturation(),
			tempSelection.getValue()
		);
		shadePicker.setHue( tempSelection.getHue() );
		huePicker.setSelection( tempSelection.getHue() );

		selectionUpdating = false;

		updateInputFields();
	}

	public void setSelection( Color color )
	{
		setSelection( new HSV( color ) );
	}

	public void setSelection( int r, int g, int b )
	{
		setSelection( new Color( r, g, b ) );
	}

	private void updateInputFields()
	{
		selectionUpdating = true;
		txtH.setText( "" + (int)( tempSelection.getHue() * 360 ) );
		txtS.setText( "" + (int)( tempSelection.getSaturation() * 100 ) );
		txtV.setText( "" + (int)( tempSelection.getValue() * 100 ) );
		Color c = tempSelection.toColor();
		txtR.setText( "" + c.getRed() );
		txtG.setText( "" + c.getGreen() );
		txtB.setText( "" + c.getBlue() );
		selectionUpdating = false;
	}

	public Color getSelection()
	{
		return finalSelection.toColor();
	}

	public HSV getSelectionHSV()
	{
		return new HSV( finalSelection );
	}

	private void createPickerPanel()
	{
		shadePicker = new ShadePicker();
		shadePicker.setBorder( BorderFactory.createLineBorder( Color.lightGray ) );

		huePicker = new HuePicker();
		int hueInset = 5 + huePicker.getIndicatorSize() / 2;

		GridBagConstraints gbc_shadePicker = new GridBagConstraints();
		gbc_shadePicker.anchor = GridBagConstraints.NORTHWEST;
		gbc_shadePicker.insets = new Insets( hueInset, 5, hueInset, 5 );
		gbc_shadePicker.gridx = 0;
		gbc_shadePicker.gridy = 0;
		getContentPane().add( shadePicker, gbc_shadePicker );

		GridBagConstraints gbc_huePicker = new GridBagConstraints();
		gbc_huePicker.insets = new Insets( 5, 5, 5, 5 );
		gbc_huePicker.anchor = GridBagConstraints.NORTHWEST;
		gbc_huePicker.gridx = 1;
		gbc_huePicker.gridy = 0;
		getContentPane().add( huePicker, gbc_huePicker );

		huePicker.setPreferredSize(
			new Dimension(
				20 + 2 * huePicker.getIndicatorSize(),
				300 + huePicker.getIndicatorSize()
			)
		);
		shadePicker.setPreferredSize( new Dimension( 300, 300 ) );

		huePicker.addPropertyChangeListener(
			HuePicker.SELECTED_HUE, ( e ) -> {
				float h = (float)e.getNewValue();
				shadePicker.setHue( h );

				updateInputFields();
			}
		);

		PropertyChangeListener shadeListener = ( e ) -> {
			if ( !selectionUpdating ) {
				selectionUpdating = true;

				tempSelection = shadePicker.getSelection();
				lblNewColor.setBackground( tempSelection.toColor() );
				updateInputFields();

				selectionUpdating = false;
			}
		};
		shadePicker.addPropertyChangeListener( ShadePicker.SELECTED_HUE, shadeListener );
		shadePicker.addPropertyChangeListener( ShadePicker.SELECTED_SATURATION, shadeListener );
		shadePicker.addPropertyChangeListener( ShadePicker.SELECTED_VALUE, shadeListener );
	}

	private void createToolsPanel( Color inputColor )
	{
		int hueInset = 5 + huePicker.getIndicatorSize() / 2;

		JPanel cTools = new JPanel();
		GridBagConstraints gbc_cTools = new GridBagConstraints();
		gbc_cTools.insets = new Insets( hueInset, 0, hueInset, 5 );
		gbc_cTools.fill = GridBagConstraints.BOTH;
		gbc_cTools.gridx = 2;
		gbc_cTools.gridy = 0;
		getContentPane().add( cTools, gbc_cTools );
		GridBagLayout gbl_cTools = new GridBagLayout();
		gbl_cTools.columnWeights = new double[] { 1.0 };
		gbl_cTools.rowWeights = new double[] { 0.0, 1.0 };
		cTools.setLayout( gbl_cTools );

		JPanel cColors = new JPanel();
		cColors.setBorder( BorderFactory.createLineBorder( Color.lightGray ) );
		GridBagConstraints gbc_cColors = new GridBagConstraints();
		gbc_cColors.insets = new Insets( 0, 0, 5, 0 );
		gbc_cColors.anchor = GridBagConstraints.NORTHWEST;
		gbc_cColors.gridx = 0;
		gbc_cColors.gridy = 0;
		cTools.add( cColors, gbc_cColors );
		GridBagLayout gbl_cColors = new GridBagLayout();
		cColors.setLayout( gbl_cColors );

		lblNewColor = new JLabel();
		GridBagConstraints gbc_lblNewColor = new GridBagConstraints();
		gbc_lblNewColor.gridx = 0;
		gbc_lblNewColor.gridy = 0;
		cColors.add( lblNewColor, gbc_lblNewColor );
		lblNewColor.setOpaque( true );

		lblOldColor = new JLabel();
		GridBagConstraints gbc_lblOldColor = new GridBagConstraints();
		gbc_lblOldColor.gridx = 0;
		gbc_lblOldColor.gridy = 1;
		cColors.add( lblOldColor, gbc_lblOldColor );
		lblOldColor.setOpaque( true );
		lblOldColor.setBackground( inputColor );

		lblOldColor.setPreferredSize( new Dimension( 70, 25 ) );
		lblNewColor.setPreferredSize( new Dimension( 70, 25 ) );

		lblOldColor.addMouseListener(
			new MouseAdapter() {
				@Override
				public void mouseClicked( MouseEvent e )
				{
					if ( SwingUtilities.isLeftMouseButton( e ) ) {
						setSelection( lblOldColor.getBackground() );
					}
				}
			}
		);

		createInputFields( cTools );
	}

	private void createInputFields( JPanel container )
	{
		JPanel cInputFields = new JPanel();
		GridBagConstraints gbc_cInputFields = new GridBagConstraints();
		gbc_cInputFields.fill = GridBagConstraints.BOTH;
		gbc_cInputFields.gridx = 0;
		gbc_cInputFields.gridy = 1;
		container.add( cInputFields, gbc_cInputFields );

		GridBagLayout gbl_cInputFields = new GridBagLayout();
		gbl_cInputFields.columnWidths = new int[] { 0, 0, 0 };
		gbl_cInputFields.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		gbl_cInputFields.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		gbl_cInputFields.rowWeights = new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		cInputFields.setLayout( gbl_cInputFields );

		JPanel filler = new JPanel();
		GridBagConstraints gbc_filler = new GridBagConstraints();
		gbc_filler.gridwidth = 2;
		gbc_filler.insets = new Insets( 0, 0, 5, 0 );
		gbc_filler.fill = GridBagConstraints.BOTH;
		gbc_filler.gridx = 0;
		gbc_filler.gridy = 0;
		cInputFields.add( filler, gbc_filler );

		JLabel lblH = new JLabel( "H:" );
		GridBagConstraints gbc_lblH = new GridBagConstraints();
		gbc_lblH.anchor = GridBagConstraints.EAST;
		gbc_lblH.insets = new Insets( 0, 0, 5, 5 );
		gbc_lblH.gridx = 0;
		gbc_lblH.gridy = 1;
		cInputFields.add( lblH, gbc_lblH );

		txtH = new JTextField();
		txtH.setHorizontalAlignment( SwingConstants.RIGHT );
		GridBagConstraints gbc_txtH = new GridBagConstraints();
		gbc_txtH.insets = new Insets( 0, 0, 5, 0 );
		gbc_txtH.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtH.gridx = 1;
		gbc_txtH.gridy = 1;
		cInputFields.add( txtH, gbc_txtH );
		txtH.setColumns( 3 );
		txtH.setDocument( new NumberDocument( 3 ) );

		JLabel lblS = new JLabel( "S:" );
		GridBagConstraints gbc_lblS = new GridBagConstraints();
		gbc_lblS.anchor = GridBagConstraints.EAST;
		gbc_lblS.insets = new Insets( 0, 0, 5, 5 );
		gbc_lblS.gridx = 0;
		gbc_lblS.gridy = 2;
		cInputFields.add( lblS, gbc_lblS );

		txtS = new JTextField();
		txtS.setHorizontalAlignment( SwingConstants.RIGHT );
		txtS.setColumns( 3 );
		GridBagConstraints gbc_txtS = new GridBagConstraints();
		gbc_txtS.insets = new Insets( 0, 0, 5, 0 );
		gbc_txtS.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtS.gridx = 1;
		gbc_txtS.gridy = 2;
		cInputFields.add( txtS, gbc_txtS );
		txtS.setDocument( new NumberDocument( 3 ) );

		JLabel lblV = new JLabel( "V:" );
		GridBagConstraints gbc_lblV = new GridBagConstraints();
		gbc_lblV.anchor = GridBagConstraints.EAST;
		gbc_lblV.insets = new Insets( 0, 0, 5, 5 );
		gbc_lblV.gridx = 0;
		gbc_lblV.gridy = 3;
		cInputFields.add( lblV, gbc_lblV );

		txtV = new JTextField();
		txtV.setHorizontalAlignment( SwingConstants.RIGHT );
		txtV.setColumns( 3 );
		GridBagConstraints gbc_txtV = new GridBagConstraints();
		gbc_txtV.insets = new Insets( 0, 0, 5, 0 );
		gbc_txtV.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtV.gridx = 1;
		gbc_txtV.gridy = 3;
		cInputFields.add( txtV, gbc_txtV );
		txtV.setDocument( new NumberDocument( 3 ) );

		JSeparator separator = new JSeparator();
		GridBagConstraints gbc_separator = new GridBagConstraints();
		gbc_separator.fill = GridBagConstraints.HORIZONTAL;
		gbc_separator.gridwidth = 2;
		gbc_separator.insets = new Insets( 0, 0, 5, 0 );
		gbc_separator.gridx = 0;
		gbc_separator.gridy = 4;
		cInputFields.add( separator, gbc_separator );

		JLabel lblR = new JLabel( "R:" );
		GridBagConstraints gbc_lblR = new GridBagConstraints();
		gbc_lblR.anchor = GridBagConstraints.EAST;
		gbc_lblR.insets = new Insets( 0, 0, 5, 5 );
		gbc_lblR.gridx = 0;
		gbc_lblR.gridy = 5;
		cInputFields.add( lblR, gbc_lblR );

		txtR = new JTextField();
		txtR.setHorizontalAlignment( SwingConstants.RIGHT );
		txtR.setColumns( 3 );
		GridBagConstraints gbc_txtR = new GridBagConstraints();
		gbc_txtR.insets = new Insets( 0, 0, 5, 0 );
		gbc_txtR.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtR.gridx = 1;
		gbc_txtR.gridy = 5;
		cInputFields.add( txtR, gbc_txtR );
		txtR.setDocument( new NumberDocument( 3 ) );

		JLabel lblG = new JLabel( "G:" );
		GridBagConstraints gbc_lblG = new GridBagConstraints();
		gbc_lblG.anchor = GridBagConstraints.EAST;
		gbc_lblG.insets = new Insets( 0, 0, 5, 5 );
		gbc_lblG.gridx = 0;
		gbc_lblG.gridy = 6;
		cInputFields.add( lblG, gbc_lblG );

		txtG = new JTextField();
		txtG.setHorizontalAlignment( SwingConstants.RIGHT );
		txtG.setColumns( 3 );
		GridBagConstraints gbc_txtG = new GridBagConstraints();
		gbc_txtG.insets = new Insets( 0, 0, 5, 0 );
		gbc_txtG.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtG.gridx = 1;
		gbc_txtG.gridy = 6;
		cInputFields.add( txtG, gbc_txtG );
		txtG.setDocument( new NumberDocument( 3 ) );

		JLabel lblB = new JLabel( "B:" );
		GridBagConstraints gbc_lblB = new GridBagConstraints();
		gbc_lblB.anchor = GridBagConstraints.EAST;
		gbc_lblB.insets = new Insets( 0, 0, 0, 5 );
		gbc_lblB.gridx = 0;
		gbc_lblB.gridy = 7;
		cInputFields.add( lblB, gbc_lblB );

		txtB = new JTextField();
		txtB.setHorizontalAlignment( SwingConstants.RIGHT );
		txtB.setColumns( 3 );
		GridBagConstraints gbc_txtB = new GridBagConstraints();
		gbc_txtB.fill = GridBagConstraints.HORIZONTAL;
		gbc_txtB.gridx = 1;
		gbc_txtB.gridy = 7;
		cInputFields.add( txtB, gbc_txtB );
		txtB.setDocument( new NumberDocument( 3 ) );

		DocumentListener docListener = new DocumentListener() {
			public void insertUpdate( DocumentEvent e )
			{
				update( e );
			}

			public void removeUpdate( DocumentEvent e )
			{
				update( e );
			}

			public void changedUpdate( DocumentEvent e )
			{
				update( e );
			}
		};

		txtH.getDocument().addDocumentListener( docListener );
		txtS.getDocument().addDocumentListener( docListener );
		txtV.getDocument().addDocumentListener( docListener );
		txtR.getDocument().addDocumentListener( docListener );
		txtG.getDocument().addDocumentListener( docListener );
		txtB.getDocument().addDocumentListener( docListener );
	}

	private void update( DocumentEvent e )
	{
		if ( selectionUpdating ) {
			return;
		}

		try {
			selectionUpdating = true;
			Document d = e.getDocument();

			if ( d.equals( txtH.getDocument() ) ||
				d.equals( txtS.getDocument() ) ||
				d.equals( txtV.getDocument() ) ) {

				float h = Utils.clamp( 0, Float.parseFloat( getText( txtH.getDocument() ) ) / 360f, 1f );
				float s = Utils.clamp( 0, Float.parseFloat( getText( txtS.getDocument() ) ) / 100f, 1f );
				float v = Utils.clamp( 0, Float.parseFloat( getText( txtV.getDocument() ) ) / 100f, 1f );
				SwingUtilities.invokeLater( () -> setSelection( new HSV( h, s, v ) ) );
			}
			else if ( d.equals( txtR.getDocument() ) ||
				d.equals( txtG.getDocument() ) ||
				d.equals( txtB.getDocument() ) ) {

				int r = Utils.clamp( 0, Integer.parseInt( getText( txtR.getDocument() ) ), 255 );
				int g = Utils.clamp( 0, Integer.parseInt( getText( txtG.getDocument() ) ), 255 );
				int b = Utils.clamp( 0, Integer.parseInt( getText( txtB.getDocument() ) ), 255 );
				SwingUtilities.invokeLater( () -> setSelection( r, g, b ) );
			}

			selectionUpdating = false;
		}
		catch ( BadLocationException ex ) {
			log.error( ex );
		}
	}

	private String getText( Document d )
		throws BadLocationException
	{
		String result = d.getText( 0, d.getLength() );
		if ( result == null || result.equals( "" ) ) {
			result = "0";
		}
		return result;
	}

	private void createButtonsPanel()
	{
		JPanel cButtons = new JPanel();
		GridBagConstraints gbc_cButtons = new GridBagConstraints();
		gbc_cButtons.anchor = GridBagConstraints.SOUTH;
		gbc_cButtons.gridwidth = 3;
		gbc_cButtons.insets = new Insets( 0, 5, 5, 5 );
		gbc_cButtons.fill = GridBagConstraints.HORIZONTAL;
		gbc_cButtons.gridx = 0;
		gbc_cButtons.gridy = 1;
		getContentPane().add( cButtons, gbc_cButtons );
		GridBagLayout gbl_cButtons = new GridBagLayout();
		gbl_cButtons.columnWeights = new double[] { 1.0, 0.0 };
		gbl_cButtons.rowWeights = new double[] { 0.0 };
		cButtons.setLayout( gbl_cButtons );

		JButton btnConfirm = new JButton( "Confirm" );
		GridBagConstraints gbc_btnConfirm = new GridBagConstraints();
		gbc_btnConfirm.anchor = GridBagConstraints.EAST;
		gbc_btnConfirm.insets = new Insets( 0, 0, 0, 5 );
		gbc_btnConfirm.gridx = 0;
		gbc_btnConfirm.gridy = 0;
		cButtons.add( btnConfirm, gbc_btnConfirm );

		JButton btnCancel = new JButton( "Cancel" );
		GridBagConstraints gbc_btnCancel = new GridBagConstraints();
		gbc_btnCancel.gridx = 1;
		gbc_btnCancel.gridy = 0;
		cButtons.add( btnCancel, gbc_btnCancel );

		btnCancel.addActionListener(
			( e ) -> {
				dispatchEvent( new WindowEvent( this, WindowEvent.WINDOW_CLOSING ) );
			}
		);

		btnConfirm.addActionListener(
			( e ) -> {
				finalSelection = tempSelection;
				dispatchEvent( new WindowEvent( this, WindowEvent.WINDOW_CLOSING ) );
			}
		);

		SwingUtilities.invokeLater( () -> btnConfirm.requestFocusInWindow() );
	}
}
