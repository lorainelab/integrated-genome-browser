package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.util.ModalUtils;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;

/**
 *
 * @author sgblanch
 * @version $Id: ClearAllAction.java 11335 2012-05-01 18:00:52Z anuj4159 $
 */
public class ClearAllAction extends GenericAction {

    private static final long serialVersionUID = 1l;
    private static final ClearAllAction ACTION = new ClearAllAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ClearAllAction getAction() {
        return ACTION;
    }

    private ClearAllAction() {
        super(MessageFormat.format(BUNDLE.getString("menuItemHasDialog"), BUNDLE.getString("clearAll")), KeyEvent.VK_C);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        if (ModalUtils.confirmPanel("Really clear entire view?")) {
            //IGB.getInstance().getMapView().clear();
        }
    }
}
