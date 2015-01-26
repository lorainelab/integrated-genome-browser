package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.util.ErrorHandler;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGB;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import javax.swing.SwingWorker;

/**
 * note !!! - depending on the script, it may not be possible to cancel it
 */
public class CancelScriptAction extends GenericAction {

    private static final long serialVersionUID = 1L;
    private static final CancelScriptAction ACTION = new CancelScriptAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static CancelScriptAction getAction() {
        return ACTION;
    }

    private CancelScriptAction() {
        super(BUNDLE.getString("cancelScript"), null, "16x16/actions/cancel_script.png", "22x22/actions/cancel_script.png", KeyEvent.VK_X);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        final IGB igb = ((IGB) Application.getSingleton());
        synchronized (igb) {
            SwingWorker<Void, Void> igbScriptWorker = igb.getScriptWorker();
            if (igbScriptWorker == null) {
                ErrorHandler.errorPanel("script error", "no script is running", Level.SEVERE);
            } else {
                igbScriptWorker.cancel(true);
                igb.setScriptWorker(null);
            }
        }
    }
}
