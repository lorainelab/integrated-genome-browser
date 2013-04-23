package com.affymetrix.genoviz.swing;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.*;

/**
 *
 * @author lorainelab
 */
public class JTextButtonCellRendererImpl extends JTextButtonCellRenderer {
	private final JFrame frame;
	
	public JTextButtonCellRendererImpl(JFrame frame) {
		super("...");
		this.frame = frame;
	}

	protected final void copyAction()
	{
		Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
		StringBuffer hackbuf = new StringBuffer(temp);
		String hackstr = new String(hackbuf);
		StringSelection data = new StringSelection(hackstr);
		clipboard.setContents(data, null);
	}
	@Override
	public void actionPerformed(ActionEvent e) {
		final JTextArea tfa = new JTextArea();
		final JScrollPane scrollPane = new JScrollPane(tfa);
		final JButton copy = new JButton("Copy");
		final JButton copyClose = new JButton("Copy And Close");
		final JButton ok = new JButton("Close");
		tfa.setEditable(false);
		tfa.setLineWrap(true);
		final JDialog dialog = new JDialog(frame, "Value") {

			@Override
			public void dispose() {
				super.dispose();
				field.setText(tfa.getText());
				fireEditingStopped();
			}
		};
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

		ok.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				dialog.dispose();
			}
		});

		copy.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				copyAction();
			}
		});
		copyClose.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				copyAction();
				dialog.dispose();
			}
		});
		
		tfa.setText(temp);
		
		if(temp.length() > 200) {
			tfa.setColumns(60);
		} else {
			tfa.setColumns(12);
		}	
		
		tfa.setRows(6);
		
		Box box = Box.createHorizontalBox();
		box.add(Box.createGlue());
		box.add(copy);
		box.add(copyClose);
		box.add(ok);
		dialog.getContentPane().add(scrollPane, "Center");
		dialog.getContentPane().add(box, "South");

		//dialog.setUndecorated(true);

		dialog.validate();
		dialog.pack();

		java.awt.Point location = frame.getLocation();
		dialog.setLocation(location.x + frame.getWidth() / 2 - dialog.getWidth() / 2, location.y + frame.getHeight() / 2 - dialog.getHeight() / 2);

		dialog.setVisible(true);
	}
}
