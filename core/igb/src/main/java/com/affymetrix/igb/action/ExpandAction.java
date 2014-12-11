package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.IGBConstants;

public class ExpandAction extends CollapseExpandActionA {

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
