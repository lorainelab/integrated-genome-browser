package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.igb.IGBConstants;

public class ShowTwoTiersAction extends ShowStrandActionA {

    private static final long serialVersionUID = 1L;
    private static final ShowTwoTiersAction ACTION = new ShowTwoTiersAction();

    static {
        GenericActionHolder.getInstance().addGenericActionSilently(ACTION);
    }

    public static ShowTwoTiersAction getAction() {
        return ACTION;
    }

    protected ShowTwoTiersAction() {
        super(IGBConstants.BUNDLE.getString("showTwoTiersAction"),
                "16x16/actions/2_track.png",
                "22x22/actions/2_track.png");
        this.ordinal = -6006012;
        separateStrands = true;
        setEnabled(false);
    }

    @Override
    protected void processChange(boolean hasSeparate, boolean hasMixed) {
        setEnabled(hasMixed);
        ShowOneTierAction.getAction().setEnabled(hasSeparate);
    }

    @Override
    public boolean isToolbarAction() {
        return false;
    }
}
