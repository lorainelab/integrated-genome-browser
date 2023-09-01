package com.affymetrix.igb.action;

import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.igb.IGB;
import com.google.common.collect.Sets;
import org.lorainelab.igb.services.IgbService;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;


import java.util.Set;
import javax.swing.Action;
import javax.swing.KeyStroke;

/**
 *
 * @author dcnorris
 */
@Component(name = GenericActionRegister.COMPONENT_NAME, immediate = true)
public class GenericActionRegister {

    public static final String COMPONENT_NAME = "GenericActionRegister";
    static final Set<GenericAction> QUEUE = Sets.newConcurrentHashSet();
    private IgbService igbService;

    public GenericActionRegister() {
        GenericActionHolder.getInstance();
    }

    @Activate
    public void activate() {
        QUEUE.forEach(this::processAction);
        QUEUE.clear();
    }

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "removeGenericAction")
    public void addGenericAction(GenericAction genericAction) {
        if (igbService != null) {
            processAction(genericAction);
        } else {
            QUEUE.add(genericAction);
        }
    }

    @Reference
    public void setIgbService(IgbService igbService) {
        this.igbService = igbService;
    }

    private void processAction(GenericAction genericAction) {
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
