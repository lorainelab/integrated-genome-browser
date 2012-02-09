/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genoviz.swing;

import java.awt.*;
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

	public JTextButtonCellRendererImpl(JFrame frame) {
		super(frame);
	}

	@Override
	protected JButton getButton() {
		return new JButton("...");
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		final JTextArea tfa = new JTextArea();
		final JButton copy = new JButton("Copy & Close");
		final JButton ok = new JButton("Ok");
		tfa.setEditable(false);
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
				Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				StringBuffer hackbuf = new StringBuffer(temp);
				String hackstr = new String(hackbuf);
				StringSelection data = new StringSelection(hackstr);
				clipboard.setContents(data, null);
				dialog.dispose();
			}
		});

		tfa.setColumns(12);
		tfa.setRows(6);
		tfa.setText(temp);

		Box box = Box.createHorizontalBox();
		box.add(Box.createGlue());
		box.add(copy);
		box.add(ok);
		dialog.getContentPane().add(tfa, "Center");
		dialog.getContentPane().add(box, "South");

		//dialog.setUndecorated(true);

		dialog.validate();
		dialog.pack();

		java.awt.Point location = frame.getLocation();
		dialog.setLocation(location.x + frame.getWidth() / 2 - dialog.getWidth() / 2, location.y + frame.getHeight() / 2 - dialog.getHeight() / 2);

		dialog.setVisible(true);
	}
}
