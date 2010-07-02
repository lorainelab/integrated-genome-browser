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

		String text = APP_NAME + "\n\n" +
						"ProtAnnot is a program developed by the Loraine Lab at \n " +
						"the University of North Carolina at Charlotte. \n" +
						"ProtAnnot implements many useful features designed for \n" +
						"understanding how alternative splicing, alternative promoters, \n" +
						"alternative promoters, and alternative polyadenylation can \n" +
						"affect the sequence and function of proteins encoded \n" +
						"by diverse variants expressed from the same gene. \n\n" +

						"For suggestions, bug reports, and comments, please contact \n" +
						"Hiral Vora hvora1@uncc.edu who is the lead programmer \n" +
						"on the project or Ann Loraine aloraine@uncc.edu who designed \n" +
						"the interface and is always eager to hear \n" +
						"suggestions and comments from users or fellow developers. \n" +
						"ProtAnnot uses the Genoviz SDK to implement animated \n"+
						"zooming, a zoom stripe, and other features that make \n " +
						"visualization of genomic data easier to accomplish. \n\n"+

						"For more details, including license information, see:\n" +
						"\thttp://www.bioviz.org/protannot\n";

		about_text.append(text);
		message_pane.add(new JScrollPane(about_text));

		final JOptionPane pane = new JOptionPane(message_pane, JOptionPane.INFORMATION_MESSAGE,
						JOptionPane.DEFAULT_OPTION);
		final JDialog dialog = pane.createDialog(frm, MessageFormat.format(BUNDLE.getString("about"), APP_NAME));
		dialog.setVisible(true);
	}
}
