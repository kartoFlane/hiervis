package pl.pwr.hiervis.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagLayout;
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
import javax.swing.text.Document;

import pl.pwr.hiervis.ui.components.HuePicker;
import pl.pwr.hiervis.ui.components.NumberDocument;
import pl.pwr.hiervis.ui.components.ShadePicker;
import pl.pwr.hiervis.util.GridBagConstraintsBuilder;
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

		createGUI( inputColor );

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

	public Color getSelection()
	{
		return finalSelection.toColor();
	}

	public HSV getSelectionHSV()
	{
		return new HSV( finalSelection );
	}

	// -----------------------------------------------------------------------------------------------
	// GUI creation methods

	private void createGUI( Color inputColor )
	{
		GridBagLayout layout = new GridBagLayout();
		layout.columnWeights = new double[] { 0.0, 0.0, 1.0 };
		layout.rowWeights = new double[] { 0.0, 1.0 };
		getContentPane().setLayout( layout );

		createPickerPanel();
		createToolsPanel( inputColor );
		createButtonsPanel();
	}

	private void createPickerPanel()
	{
		GridBagConstraintsBuilder builder = new GridBagConstraintsBuilder();

		shadePicker = new ShadePicker();
		huePicker = new HuePicker();

		int hueInset = 5 + huePicker.getIndicatorSize() / 2;

		shadePicker.setBorder( BorderFactory.createLineBorder( Color.lightGray ) );
		getContentPane().add( shadePicker, builder.insets( hueInset, 5, hueInset, 5 ).anchorNorthWest().position( 0, 0 ).build() );
		getContentPane().add( huePicker, builder.insets( 5 ).anchorNorthWest().position( 1, 0 ).build() );

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
		GridBagConstraintsBuilder builder = new GridBagConstraintsBuilder();

		int hueInset = 5 + huePicker.getIndicatorSize() / 2;

		JPanel cTools = new JPanel();
		getContentPane().add( cTools, builder.insets( hueInset, 0, hueInset, 5 ).fill().position( 2, 0 ).build() );

		GridBagLayout layout = new GridBagLayout();
		layout.columnWeights = new double[] { 1.0 };
		layout.rowWeights = new double[] { 0.0, 1.0 };
		cTools.setLayout( layout );

		JPanel cColors = new JPanel();
		cColors.setBorder( BorderFactory.createLineBorder( Color.lightGray ) );
		cTools.add( cColors, builder.insets( 0, 0, 5, 0 ).anchorNorthWest().position( 0, 0 ).build() );

		GridBagLayout gbl_cColors = new GridBagLayout();
		cColors.setLayout( gbl_cColors );

		lblNewColor = new JLabel();
		cColors.add( lblNewColor, builder.position( 0, 0 ).build() );
		lblNewColor.setOpaque( true );

		lblOldColor = new JLabel();
		cColors.add( lblOldColor, builder.position( 0, 1 ).build() );
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
		GridBagConstraintsBuilder builder = new GridBagConstraintsBuilder();

		JPanel cInputFields = new JPanel();
		container.add( cInputFields, builder.fill().position( 0, 1 ).build() );

		GridBagLayout layout = new GridBagLayout();
		layout.columnWidths = new int[] { 0, 0, 0 };
		layout.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		layout.columnWeights = new double[] { 0.0, 1.0, Double.MIN_VALUE };
		layout.rowWeights = new double[] { 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, Double.MIN_VALUE };
		cInputFields.setLayout( layout );

		JPanel filler = new JPanel();
		cInputFields.add( filler, builder.insets( 0, 0, 5, 0 ).fill().position( 0, 0 ).spanHorizontal( 2 ).build() );

		JLabel lblH = new JLabel( "H:" );
		cInputFields.add( lblH, builder.insets( 0, 0, 5, 5 ).anchorEast().position( 0, 1 ).build() );

		txtH = buildNumberTextField();
		cInputFields.add( txtH, builder.insets( 0, 0, 5, 0 ).fillHorizontal().position( 1, 1 ).build() );

		JLabel lblS = new JLabel( "S:" );
		cInputFields.add( lblS, builder.insets( 0, 0, 5, 5 ).anchorEast().position( 0, 2 ).build() );

		txtS = buildNumberTextField();
		cInputFields.add( txtS, builder.insets( 0, 0, 5, 0 ).fillHorizontal().position( 1, 2 ).build() );

		JLabel lblV = new JLabel( "V:" );
		cInputFields.add( lblV, builder.insets( 0, 0, 5, 5 ).anchorEast().position( 0, 3 ).build() );

		txtV = buildNumberTextField();
		cInputFields.add( txtV, builder.insets( 0, 0, 5, 0 ).fillHorizontal().position( 1, 3 ).build() );

		cInputFields.add(
			new JSeparator(),
			builder.insets( 0, 0, 5, 0 ).fillHorizontal().position( 0, 4 ).spanHorizontal( 2 ).build()
		);

		JLabel lblR = new JLabel( "R:" );
		cInputFields.add( lblR, builder.insets( 0, 0, 5, 5 ).anchorEast().position( 0, 5 ).build() );

		txtR = buildNumberTextField();
		cInputFields.add( txtR, builder.insets( 0, 0, 5, 0 ).fillHorizontal().position( 1, 5 ).build() );

		JLabel lblG = new JLabel( "G:" );
		cInputFields.add( lblG, builder.insets( 0, 0, 5, 5 ).anchorEast().position( 0, 6 ).build() );

		txtG = buildNumberTextField();
		cInputFields.add( txtG, builder.insets( 0, 0, 5, 0 ).fillHorizontal().position( 1, 6 ).build() );

		JLabel lblB = new JLabel( "B:" );
		cInputFields.add( lblB, builder.insets( 0, 0, 5, 5 ).anchorEast().position( 0, 7 ).build() );

		txtB = buildNumberTextField();
		cInputFields.add( txtB, builder.insets( 0, 0, 5, 0 ).fillHorizontal().position( 1, 7 ).build() );

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

	private void createButtonsPanel()
	{
		GridBagConstraintsBuilder builder = new GridBagConstraintsBuilder();

		JPanel cButtons = new JPanel();
		getContentPane().add(
			cButtons,
			builder.insets( 0, 5, 5, 5 ).anchorSouth().fillHorizontal().position( 0, 1 ).spanHorizontal( 3 ).build()
		);

		GridBagLayout layout = new GridBagLayout();
		layout.columnWeights = new double[] { 1.0, 0.0 };
		layout.rowWeights = new double[] { 0.0 };
		cButtons.setLayout( layout );

		JButton btnConfirm = new JButton( "Confirm" );
		cButtons.add( btnConfirm, builder.insets( 0, 0, 0, 5 ).anchorEast().position( 0, 0 ).build() );

		JButton btnCancel = new JButton( "Cancel" );
		cButtons.add( btnCancel, builder.position( 1, 0 ).build() );

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

	private static JTextField buildNumberTextField()
	{
		JTextField result = new JTextField();
		result.setHorizontalAlignment( SwingConstants.RIGHT );
		result.setColumns( 3 );
		result.setDocument( new NumberDocument( 3 ) );

		return result;
	}

	// -----------------------------------------------------------------------------------------------
	// Misc methods

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

	private void update( DocumentEvent e )
	{
		if ( selectionUpdating ) {
			return;
		}

		selectionUpdating = true;
		Document d = e.getDocument();

		if ( d.equals( txtH.getDocument() ) ||
			d.equals( txtS.getDocument() ) ||
			d.equals( txtV.getDocument() ) ) {

			float h = Utils.clamp( 0, Float.parseFloat( getText( txtH ) ) / 360f, 1f );
			float s = Utils.clamp( 0, Float.parseFloat( getText( txtS ) ) / 100f, 1f );
			float v = Utils.clamp( 0, Float.parseFloat( getText( txtV ) ) / 100f, 1f );
			SwingUtilities.invokeLater( () -> setSelection( new HSV( h, s, v ) ) );
		}
		else if ( d.equals( txtR.getDocument() ) ||
			d.equals( txtG.getDocument() ) ||
			d.equals( txtB.getDocument() ) ) {

			int r = Utils.clamp( 0, Integer.parseInt( getText( txtR ) ), 255 );
			int g = Utils.clamp( 0, Integer.parseInt( getText( txtG ) ), 255 );
			int b = Utils.clamp( 0, Integer.parseInt( getText( txtB ) ), 255 );
			SwingUtilities.invokeLater( () -> setSelection( r, g, b ) );
		}

		selectionUpdating = false;
	}

	/**
	 * Gets the text of the specified text field, or "0", if the text field is empty.
	 */
	private static String getText( JTextField text )
	{
		String t = text.getText();
		return t == null || t.equals( "" ) ? "0" : t;
	}
}
