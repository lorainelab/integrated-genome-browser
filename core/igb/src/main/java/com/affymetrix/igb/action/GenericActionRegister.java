package com.affymetrix.igb.action;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.IgbServiceDependencyManager;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.Action;
import javax.swing.KeyStroke;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Component(name = GenericActionRegister.COMPONENT_NAME, immediate = true)
public class GenericActionRegister {

    public static final String COMPONENT_NAME = "GenericActionRegister";

    private static final Logger logger = LoggerFactory.getLogger(GenericActionRegister.class);

    Set<GenericAction> pendingActions;

    private IgbServiceDependencyManager igbServiceDependencyManager;

    public GenericActionRegister() {
        this.pendingActions = Collections.newSetFromMap(new ConcurrentHashMap<GenericAction, Boolean>());
        GenericActionHolder.getInstance();
    }

    @Reference
    public void setIgbServiceDependencyManager(IgbServiceDependencyManager igbServiceDependencyManager) {
        this.igbServiceDependencyManager = igbServiceDependencyManager;
        pendingActions.stream().forEach((action) -> {
            try {
                processGenericAction(action);
            } catch (Exception ex) {
                logger.error(ex.getMessage(), ex);
            }
        });
        pendingActions.clear();
    }

    @Reference(optional = true, multiple = true, dynamic = true, unbind = "removeGenericAction")
    public void addGenericAction(GenericAction genericAction) {
        synchronized (this) {
            if (igbServiceDependencyManager == null) {
                pendingActions.add(genericAction);
            } else {
                processGenericAction(genericAction);
            }
        }
    }

    private void processGenericAction(GenericAction genericAction) {
        if (genericAction.getId() != null) {
            KeyStroke ks = genericAction.getKeyStroke();
            if (ks != null) {
                genericAction.putValue(Action.ACCELERATOR_KEY, ks);
            }
            ((IGB) IGB.getInstance()).addAction(genericAction);
            boolean isToolbar = PreferenceUtils.getToolbarNode().getBoolean(genericAction.getId(), false);
            if (isToolbar) {
                int index = PreferenceUtils.getToolbarNode().getInt(genericAction.getId() + ".index", -1);
                if (index == -1) {
                    ((IGB) IGB.getInstance()).addToolbarAction(genericAction);
                } else {
                    ((IGB) IGB.getInstance()).addToolbarAction(genericAction, index);
                }
            }
        }
    }

    public void removeGenericAction(GenericAction genericAction) {
    }

}
