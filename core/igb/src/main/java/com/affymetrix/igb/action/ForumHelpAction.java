package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.util.GeneralUtils;
import static com.affymetrix.igb.IGBConstants.APP_NAME;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;

/**
 *
 * @author hiralv
 */
public class ForumHelpAction extends GenericAction {

    private static final long serialVersionUID = 1L;
    private static final ForumHelpAction ACTION = new ForumHelpAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ForumHelpAction getAction() {
        return ACTION;
    }

    private ForumHelpAction() {
        super(MessageFormat.format(BUNDLE.getString("forumHelp"), APP_NAME),
                null, "16x16/actions/help.png",
                "22x22/actions/help.png",
                KeyEvent.VK_UNDEFINED, null, true);
        this.ordinal = 120;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        GeneralUtils.browse("https://sourceforge.net/projects/genoviz/forums/forum/439787");
    }
}
