package com.affymetrix.igb.action;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.shared.NoToolbarActions;

@Component(name = ExpandAction.COMPONENT_NAME, immediate = true, provide = {NoToolbarActions.class})
public class ExpandAction extends CollapseExpandActionA implements NoToolbarActions {

    public static final String COMPONENT_NAME = "ExpandAction";
    private static final long serialVersionUID = 1L;
    private static final ExpandAction ACTION = new ExpandAction();

    static {
        GenericActionHolder.getInstance().addGenericActionSilently(ACTION);
        GenometryModel.getInstance().addSymSelectionListener(ACTION);
    }

    public static ExpandAction getAction() {
        return ACTION;
    }

    protected ExpandAction() {
        super(IGBConstants.BUNDLE.getString("expandAction"), "16x16/actions/expand.png", "22x22/actions/expand.png");
        collapsedTracks = false;
        this.setEnabled(false);
    }

    @Override
    protected void processChange(boolean hasCollapsed, boolean hasExpanded) {
        setEnabled(hasCollapsed);
        CollapseAction.getAction().setEnabled(hasExpanded);
    }
}
