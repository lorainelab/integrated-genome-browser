package org.bioviz.protannot.action;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import javax.swing.AbstractAction;

import static org.bioviz.protannot.ProtAnnotMain.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id: ReportBugAction.java 5772 2010-04-26 19:51:15Z sgblanch $
 */
public class ReportBugAction extends AbstractAction {
	private static final long serialVersionUID = 1l;

	public ReportBugAction() {
		super(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("reportABug")));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_R);
	}

	public void actionPerformed(ActionEvent e) {
		String u = "https://sourceforge.net/tracker/?limit=25&group_id=129420&atid=714744&category=1343170&status=1&category=1343170";
		GeneralUtils.browse(u);
	}
}
