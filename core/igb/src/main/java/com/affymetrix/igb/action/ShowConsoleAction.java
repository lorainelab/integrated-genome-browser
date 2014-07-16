package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.ConsoleView;
import com.affymetrix.igb.IGBConstants;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 *
 * @author sgblanch
 * @version $Id: ShowConsoleAction.java 11362 2012-05-02 14:52:30Z anuj4159 $
 */
public class ShowConsoleAction extends GenericAction {

    private static final long serialVersionUID = 1l;
    private static final ShowConsoleAction ACTION = new ShowConsoleAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ShowConsoleAction getAction() {
        return ACTION;
    }

    private ShowConsoleAction() {
        super(BUNDLE.getString("showConsole"), null,
                "16x16/actions/console.png",
                "22x22/actions/console.png",
                KeyEvent.VK_C, null, false);
        this.ordinal = 150;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        ConsoleView.showConsole(IGBConstants.APP_NAME);
    }
}
