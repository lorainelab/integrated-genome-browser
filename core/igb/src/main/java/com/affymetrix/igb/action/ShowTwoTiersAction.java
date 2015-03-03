package com.affymetrix.igb.action;

import aQute.bnd.annotation.component.Component;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.shared.NoToolbarActions;

@Component(name = ShowTwoTiersAction.COMPONENT_NAME, immediate = true, provide = NoToolbarActions.class)
public class ShowTwoTiersAction extends ShowStrandActionA implements NoToolbarActions {

    public static final String COMPONENT_NAME = "ShowTwoTiersAction";
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
}
