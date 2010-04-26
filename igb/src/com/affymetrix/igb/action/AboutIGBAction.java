/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.menuitem.MenuUtil;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.MessageFormat;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import static com.affymetrix.igb.IGBConstants.APP_NAME;
import static com.affymetrix.igb.IGBConstants.APP_VERSION_FULL;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 */
public class AboutIGBAction extends AbstractAction {
	private static final long serialVersionUID = 1l;

	public AboutIGBAction() {
		super(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					MessageFormat.format(
						BUNDLE.getString("about"),
						APP_NAME)),
				MenuUtil.getIcon("toolbarButtonGraphics/general/About16.gif"));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_A);
	}

	public void actionPerformed(ActionEvent e) {
		JPanel message_pane = new JPanel();
		message_pane.setLayout(new BoxLayout(message_pane, BoxLayout.Y_AXIS));
		JTextArea about_text = new JTextArea();

		String text = APP_NAME + ", version: " + APP_VERSION_FULL + "\n\n" +
						"IGB (pronounced ig-bee) is a product of the open source Genoviz project,\n" +
						"which develops interactive visualization software for genomics.\n" +
						"Affymetrix, Inc., donated Genoviz and IGB to the open source community in 2004.\n" +
						"IGB and Genoviz receive support from National Science Foundation's Arabidopsis 2010 program\n" +
						"and from a growing community of developers and scientists. For details, see:\n" +
						"http://igb.bioviz.org\n" +
						"http://genoviz.sourceforge.net\n\n" +
						"Source code for IGB is released under the Common Public License, v1.0.\n" +
						"IGB is Copyright (c) 2000-2005 Affymetrix, Inc.\n" +
						"IGB uses:\n" +
						"the Fusion SDK from Affymetrix,\n" +
						"the Vector Graphics package from http://java.freehep.org\n" +
						"(released under the LGPL license),\n" +
						"the Picard package from http://picard.sourceforge.net\n" +
						"(released under the Apache version 2.0 license)\n\n";
		about_text.append(text);
		String cache_root = com.affymetrix.genometryImpl.util.LocalUrlCacher.getCacheRoot();
		File cache_file = new File(cache_root);
		if (cache_file.exists()) {
			about_text.append("\nCached data stored in: \n");
			about_text.append("  " + cache_file.getAbsolutePath() + "\n");
		}
		String data_dir = PreferenceUtils.getAppDataDirectory();
		if (data_dir != null) {
			File data_dir_f = new File(data_dir);
			about_text.append("\nApplication data stored in: \n  " +
							data_dir_f.getAbsolutePath() + "\n");
		}

		message_pane.add(new JScrollPane(about_text));
		JButton licenseB = new JButton("View IGB License");
		JButton apacheB = new JButton("View Apache License");
		JButton freehepB = new JButton("View FreeHEP Vector Graphics License");
		JButton fusionB = new JButton("View Fusion SDK License");
		licenseB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent evt) {
				GeneralUtils.browse("http://www.affymetrix.com/support/developer/tools/igbsource_terms.affx?to");
			}
		});
		apacheB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent evt) {
				GeneralUtils.browse("http://www.apache.org/licenses/LICENSE-2.0");
			}
		});
		freehepB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent evt) {
				GeneralUtils.browse("http://java.freehep.org/vectorgraphics/license.html");
			}
		});
		fusionB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent evt) {
				GeneralUtils.browse("http://www.affymetrix.com/support/developer/fusion/index.affx");
			}
		});
		JPanel buttonP = new JPanel(new GridLayout(2, 2));
		buttonP.add(licenseB);
		buttonP.add(apacheB);
		buttonP.add(freehepB);
		buttonP.add(fusionB);
		message_pane.add(buttonP);

		final JOptionPane pane = new JOptionPane(message_pane, JOptionPane.INFORMATION_MESSAGE,
						JOptionPane.DEFAULT_OPTION);
		final JDialog dialog = pane.createDialog(IGB.getSingleton().getFrame(), MessageFormat.format(BUNDLE.getString("about"), APP_NAME));
		dialog.setVisible(true);
	}
}
