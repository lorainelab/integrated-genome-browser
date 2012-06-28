package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.GeneralUtils;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
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

	static{
		GenericActionHolder.getInstance().addGenericAction(ACTION);
	}
	
	public static ForumHelpAction getAction() {
		return ACTION;
	}

	private ForumHelpAction() {
		super(MessageFormat.format(BUNDLE.getString("forumHelp"),APP_NAME), null, "16x16/actions/address-book-new.png","22x22/actions/address-book-new.png", KeyEvent.VK_UNDEFINED, null, true);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		GeneralUtils.browse("https://sourceforge.net/projects/genoviz/forums/forum/439787");
	}
}
