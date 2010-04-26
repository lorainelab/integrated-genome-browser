package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.menuitem.MenuUtil;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class DocumentationAction extends AbstractAction {
	private static final long serialVersionUID = 1l;

	public DocumentationAction() {
		super(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("documentation")),
				MenuUtil.getIcon("toolbarButtonGraphics/general/Help16.gif"));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_D);
	}

	public void actionPerformed(ActionEvent e) {
		JPanel message_pane = new JPanel();
		message_pane.setLayout(new BoxLayout(message_pane, BoxLayout.Y_AXIS));
		JTextArea about_text = new JTextArea();
		about_text.append(getDocumentationText());
		message_pane.add(new JScrollPane(about_text));

		JButton sfB = new JButton("Visit Genoviz Project");
		sfB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent evt) {
				GeneralUtils.browse("http://genoviz.sourceforge.net");
			}
		});
		Box buttonP = Box.createHorizontalBox();
		buttonP.add(sfB);

		message_pane.add(buttonP);

		final JOptionPane pane = new JOptionPane(message_pane, JOptionPane.INFORMATION_MESSAGE,
						JOptionPane.DEFAULT_OPTION);
		final JDialog dialog = pane.createDialog(IGB.getSingleton().getFrame(), "Documentation");
		dialog.setResizable(true);
		dialog.setVisible(true);
	}

	private static final String getDocumentationText() {
		StringBuffer sb = new StringBuffer();
		sb.append("\n");
		sb.append("Documentation and user forums for IGB can be found at SourceForge.\n");
		sb.append("\n");
		sb.append("The source code is hosted at SourceForge.net as part of the GenoViz project. \n");
		sb.append("There you can find downloads of source code, pre-compiled executables, \n");
		sb.append("extra documentation, and a place to report bugs or feature requests.\n");
		sb.append("\n");
		sb.append("Introduction Page: http://genoviz.sourceforge.net/\n");
		sb.append("User's Guide (PDF): \n http://genoviz.sourceforge.net/IGB_User_Guide.pdf\n");
		sb.append("Release Notes: \n http://genoviz.sourceforge.net/release_notes/igb_release.html");
		sb.append("\n");
		sb.append("Downloads: \n http://sourceforge.net/project/showfiles.php?group_id=129420\n");
		sb.append("Documentation: \n http://sourceforge.net/apps/trac/genoviz/wiki/IGB\n");
		sb.append("Bug Reports: \n http://sourceforge.net/tracker/?group_id=129420&atid=714744\n");
		sb.append("\n");

		return sb.toString();
	}
}
