package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.igb.IGBConstants;
import java.awt.event.ActionEvent;

public class SelectParentAction extends SeqMapViewActionA {

    private static final long serialVersionUID = 1L;
    private static SelectParentAction ACTION = new SelectParentAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static SelectParentAction getAction() {
        return ACTION;
    }

    protected SelectParentAction() {
        super(IGBConstants.BUNDLE.getString("selectParentAction"), null, null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        getSeqMapView().selectParents();
    }
}
