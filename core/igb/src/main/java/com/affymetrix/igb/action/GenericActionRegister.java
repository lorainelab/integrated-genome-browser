package com.affymetrix.igb.action;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGB;
import java.util.prefs.Preferences;
import javax.swing.Action;
import javax.swing.KeyStroke;

/**
 *
 * @author dcnorris
 */
@Component(name = GenericActionRegister.COMPONENT_NAME, immediate = true)
public class GenericActionRegister {

    public static final String COMPONENT_NAME = "GenericActionRegister";

    public GenericActionRegister() {
        GenericActionHolder.getInstance();
    }

    @Reference(optional = true, multiple = true, dynamic = true, unbind = "removeGenericAction")
    public void addGenericAction(GenericAction genericAction) {
        if (genericAction.getId() != null) {
            Preferences p = PreferenceUtils.getKeystrokesNode();
            if (null != p) {
                String ak = p.get(genericAction.getId(), "");
                if (null != ak & 0 < ak.length()) {
                    KeyStroke ks = KeyStroke.getKeyStroke(ak);
                    genericAction.putValue(Action.ACCELERATOR_KEY, ks);
                }
            }
            ((IGB) Application.getSingleton()).addAction(genericAction);
            boolean isToolbar = PreferenceUtils.getToolbarNode().getBoolean(genericAction.getId(), false);
            if (isToolbar) {
                int index = PreferenceUtils.getToolbarNode().getInt(genericAction.getId() + ".index", -1);
                if (index == -1) {
                    ((IGB) Application.getSingleton()).addToolbarAction(genericAction);
                } else {
                    ((IGB) Application.getSingleton()).addToolbarAction(genericAction, index);
                }
            }
        }
    }

    public void removeGenericAction(GenericAction genericAction) {
    }

}
