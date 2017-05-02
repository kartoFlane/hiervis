package pl.pwr.hiervis.util.ui;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;


/**
 * File chooser with added confirmation dialog when overwriting an existing file.
 */
public class JFileChooserEx extends JFileChooser
{
	@Override
	public void approveSelection()
	{
		File f = getSelectedFile();

		if ( f.exists() && getDialogType() == SAVE_DIALOG ) {
			int result = JOptionPane.showConfirmDialog(
				this,
				String.format(
					"File %s already exists. Do you want to overwrite it?",
					f.getName()
				),
				"Existing file", JOptionPane.YES_NO_OPTION
			);
			switch ( result ) {
				case JOptionPane.YES_OPTION:
					super.approveSelection();
				case JOptionPane.NO_OPTION:
				case JOptionPane.CLOSED_OPTION:
					return;
			}
		}

		super.approveSelection();
	}
}
