package com.affymetrix.igb.action;

import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.igb.IGBConstants;

public class CollapseAction extends CollapseExpandActionA {
    
    private static final long serialVersionUID = 1L;
    private static final CollapseAction ACTION = new CollapseAction();

    static {
        GenericActionHolder.getInstance().addGenericActionSilently(ACTION);
        GenometryModel.getInstance().addSymSelectionListener(ACTION);
    }

    public static CollapseAction getAction() {
        return ACTION;
    }

    protected CollapseAction() {
        super(IGBConstants.BUNDLE.getString("collapseAction"), "16x16/actions/collapse.png", "22x22/actions/collapse.png");
        collapsedTracks = true;
        this.setEnabled(false);
    }

    @Override
    protected void processChange(boolean hasCollapsed, boolean hasExpanded) {
        setEnabled(hasExpanded);
        ExpandAction.getAction().setEnabled(hasCollapsed);
    }
    
    @Override
    public boolean isToolbarAction() {
        return false;
    }
}
