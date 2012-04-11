package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.util.GeneralUtils;

import java.awt.event.ActionEvent;
import java.text.MessageFormat;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
import static com.affymetrix.igb.IGBConstants.APP_NAME;

/**
 *
 * @author hiralv
 */
public class ForumHelpAction extends GenericAction {
	private static final long serialVersionUID = 1L;
	private static final ForumHelpAction ACTION = new ForumHelpAction();

	public static ForumHelpAction getAction() {
		return ACTION;
	}

	private ForumHelpAction() {
		super();
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		GeneralUtils.browse("https://sourceforge.net/projects/genoviz/forums/forum/439787");
	}

	@Override
	public String getText() {
		return MessageFormat.format(
					BUNDLE.getString("forumHelp"),
					APP_NAME);
	}

	@Override
	public String getIconPath() {
		return "toolbarButtonGraphics/general/Information16.gif";
	}

	@Override
	public boolean isPopup() {
		return true;
	}
}
