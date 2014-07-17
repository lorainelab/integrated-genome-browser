package com.affymetrix.igb.shared;

import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.action.SeqMapViewActionA;
import java.awt.event.ActionEvent;

public class DeselectAllAction extends SeqMapViewActionA {

    private static final long serialVersionUID = 1L;
    private static DeselectAllAction ACTION = new DeselectAllAction();

    public static DeselectAllAction getAction() {
        return ACTION;
    }

    protected DeselectAllAction() {
        super(IGBConstants.BUNDLE.getString("selectNone"), null, null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        execute();
    }

    public void execute() {
        getSeqMapView().deselectAll();
    }
}
