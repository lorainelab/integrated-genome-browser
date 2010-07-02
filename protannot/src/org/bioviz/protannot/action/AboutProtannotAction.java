/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.bioviz.protannot.action;

import javax.swing.JFrame;
import com.affymetrix.genometryImpl.util.MenuUtil;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import static org.bioviz.protannot.ProtAnnotMain.BUNDLE;


/**
 * Open a window showing information about Integrated Genome Browser.
 * @author sgblanch
 */
public class AboutProtannotAction extends AbstractAction {
	private static final long serialVersionUID = 1l;
	private static final String APP_NAME = BUNDLE.getString("appName");
	private final JFrame frm;
	public AboutProtannotAction(JFrame frm) {
		super(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					MessageFormat.format(
						BUNDLE.getString("about"),
						APP_NAME)),
				MenuUtil.getIcon("toolbarButtonGraphics/general/About16.gif"));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_A);
		this.frm = frm;
	}

	public void actionPerformed(ActionEvent e) {
		JPanel message_pane = new JPanel();
		message_pane.setLayout(new BoxLayout(message_pane, BoxLayout.Y_AXIS));
		JTextArea about_text = new JTextArea();
		about_text.setEditable(false);

		String text = APP_NAME + "\n\n" +
						"ProtAnnot implements many useful features designed for \n" +
						"understanding how alternative splicing, alternative promoters, \n" +
						"alternative promoters, and alternative polyadenylation can \n" +
						"affect the sequence and function of proteins encoded \n" +
						"by diverse variants expressed from the same gene. \n\n" +

						"ProtAnnot is a program developed by Hiral Vora, John Nicol\n " +
						"and Ann Loraine at the University of North Carolina at Charlotte. \n\n" +

						"For more details, including license information, see:\n" +
						"http://www.bioviz.org/protannot\n";

		about_text.append(text);
		message_pane.add(new JScrollPane(about_text));

		final JOptionPane pane = new JOptionPane(message_pane, JOptionPane.INFORMATION_MESSAGE,
						JOptionPane.DEFAULT_OPTION);
		final JDialog dialog = pane.createDialog(frm, MessageFormat.format(BUNDLE.getString("about"), APP_NAME));
		dialog.setVisible(true);
	}
}
