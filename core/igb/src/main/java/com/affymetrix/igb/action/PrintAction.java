package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.gui.PrintDialog;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Level;

/**
 *
 * @author tkanapar
 */
public class PrintAction extends GenericAction {

    private static final long serialVersionUID = 1l;
    private static final PrintAction ACTION = new PrintAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static PrintAction getAction() {
        return ACTION;
    }

    private PrintAction() {
        super(BUNDLE.getString("print"), null, "16x16/actions/print.png", "22x22/actions/print.png", KeyEvent.VK_P, null, true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        super.actionPerformed(e);
        try {
            PrintDialog.getSingleton().display();
        } catch (Exception ex) {
            com.affymetrix.genometry.util.ErrorHandler.errorPanel("Problem during print.", ex, Level.SEVERE);
        }
    }
}
