package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.igb.shared.IGBAction;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id$
 */
public class ReportBugAction extends IGBAction {
	private static final long serialVersionUID = 1l;
	private static final ReportBugAction ACTION = new ReportBugAction();

	public ReportBugAction() {
		super();
	}

	public static ReportBugAction getAction() {
		return ACTION;
	}

	public void actionPerformed(ActionEvent e) {
		GeneralUtils.browse("http://sourceforge.net/tracker/?group_id=129420&atid=714744");
	}

	@Override
	public String getText() {
		return MessageFormat.format(
				BUNDLE.getString("menuItemHasDialog"),
				BUNDLE.getString("reportABug"));
	}

	@Override
	public int getShortcut() {
		return KeyEvent.VK_R;
	}
}
