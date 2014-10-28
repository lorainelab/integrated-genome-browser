package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.shared.ExportDialog;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Level;

/**
 *
 * @author nick
 */
public class SaveImageAction extends GenericAction {

    private static final long serialVersionUID = 1l;
    private static final SaveImageAction ACTION = new SaveImageAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static SaveImageAction getAction() {
        return ACTION;
    }

    private SaveImageAction() {
        super(BUNDLE.getString("saveImage"), BUNDLE.getString("saveImageTooltip"),
                "16x16/actions/camera_toolbar.png",
                "22x22/actions/camera_toolbar.png",
                KeyEvent.VK_UNDEFINED, null, true);
        this.ordinal = -9002000;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);

        try {
            ExportDialog.getSingleton().display(false);
        } catch (Exception ex) {
            ErrorHandler.errorPanel("Problem during output.", ex, Level.SEVERE);
        }
    }

}
