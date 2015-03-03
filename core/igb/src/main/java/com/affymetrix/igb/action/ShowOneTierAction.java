package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.igb.IGBConstants;

public class ShowOneTierAction extends ShowStrandActionA {

    private static final long serialVersionUID = 1L;
    private static final ShowOneTierAction ACTION = new ShowOneTierAction();

    static {
        GenericActionHolder.getInstance().addGenericActionSilently(ACTION);
    }

    public static ShowOneTierAction getAction() {
        return ACTION;
    }

    protected ShowOneTierAction() {
        super(IGBConstants.BUNDLE.getString("showSingleTierAction"),
                "16x16/actions/1_track.png",
                "22x22/actions/1_track.png");
        this.ordinal = -8006011;
        separateStrands = false;
        setEnabled(false);
    }

    @Override
    protected void processChange(boolean hasSeparate, boolean hasMixed) {
        setEnabled(hasSeparate);
        ShowTwoTiersAction.getAction().setEnabled(hasMixed);
    }

    @Override
    public boolean isToolbarAction() {
        return false;
    }
}
