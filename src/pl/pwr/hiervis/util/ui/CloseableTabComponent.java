package pl.pwr.hiervis.util.ui;


import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.plaf.basic.BasicButtonUI;


/**
 * Component to be used as tabComponent; contains a JLabel to show the text and
 * a JButton to close the tab it belongs to.
 * 
 * Modified version of:
 * http://docs.oracle.com/javase/tutorial/uiswing/examples/zipfiles/components-TabComponentsDemoProject.zip
 */
@SuppressWarnings("serial")
public class CloseableTabComponent extends JPanel
{
	private final CloseTabButton btnClose;


	public CloseableTabComponent( final JTabbedPane pane )
	{
		super( new GridBagLayout() );

		if ( pane == null ) {
			throw new NullPointerException( "TabbedPane is null" );
		}

		setOpaque( false );

		// Make JLabel read titles from JTabbedPane
		JLabel label = new JLabel() {
			public String getText()
			{
				int i = pane.indexOfTabComponent( CloseableTabComponent.this );
				if ( i != -1 ) {
					String tabTitle = pane.getTitleAt( i );

					if ( pane.getToolTipTextAt( i ) == null ) {
						pane.setToolTipTextAt( i, tabTitle );
					}

					return tabTitle;
				}
				return null;
			}
		};

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.fill = GridBagConstraints.HORIZONTAL;

		add( label, gbc );
		label.setBorder( BorderFactory.createEmptyBorder( 0, 0, 0, 5 ) );
		label.setOpaque( false );
		btnClose = new CloseTabButton();
		add( btnClose );
		setBorder( BorderFactory.createEmptyBorder( 2, 0, 0, 0 ) );

		label.setPreferredSize( new Dimension( 200, btnClose.getPreferredSize().height ) );
	}

	public void addCloseListener( ActionListener l )
	{
		btnClose.addActionListener( l );
	}

	public void removeCloseListener( ActionListener l )
	{
		btnClose.removeActionListener( l );
	}


	private class CloseTabButton extends JButton
	{
		public CloseTabButton()
		{
			int size = 17;
			setPreferredSize( new Dimension( size, size ) );
			setToolTipText( "Click to close this tab" );
			setUI( new BasicButtonUI() );
			setContentAreaFilled( false );
			setFocusable( false );
			setBorder( BorderFactory.createEtchedBorder() );
			setBorderPainted( false );
			addMouseListener( buttonMouseListener );
			setRolloverEnabled( true );
		}

		public void updateUI()
		{
		}

		protected void paintComponent( Graphics g )
		{
			super.paintComponent( g );
			Graphics2D g2 = (Graphics2D)g.create();
			// shift the image for pressed buttons
			if ( getModel().isPressed() ) {
				g2.translate( 1, 1 );
			}
			g2.setStroke( new BasicStroke( 2 ) );
			g2.setColor( Color.BLACK );
			if ( getModel().isRollover() ) {
				g2.setColor( Color.RED );
			}
			int delta = 6;
			g2.drawLine( delta, delta, getWidth() - delta - 1, getHeight() - delta - 1 );
			g2.drawLine( getWidth() - delta - 1, delta, delta, getHeight() - delta - 1 );
			g2.dispose();
		}
	}


	private final static MouseListener buttonMouseListener = new MouseAdapter() {
		public void mouseEntered( MouseEvent e )
		{
			Component component = e.getComponent();
			if ( component instanceof AbstractButton ) {
				AbstractButton button = (AbstractButton)component;
				button.setBorderPainted( true );
			}
		}

		public void mouseExited( MouseEvent e )
		{
			Component component = e.getComponent();
			if ( component instanceof AbstractButton ) {
				AbstractButton button = (AbstractButton)component;
				button.setBorderPainted( false );
			}
		}
	};
}
