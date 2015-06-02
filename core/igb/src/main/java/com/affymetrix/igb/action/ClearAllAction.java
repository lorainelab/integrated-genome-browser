package com.affymetrix.igb.action;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.event.GenericAction;
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
@Component(name = ClearAllAction.COMPONENT_NAME, immediate = true, provide = GenericAction.class)
public class ClearAllAction extends GenericAction {

    public static final String COMPONENT_NAME = "ClearAllAction";
    private static final long serialVersionUID = 1L;

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
