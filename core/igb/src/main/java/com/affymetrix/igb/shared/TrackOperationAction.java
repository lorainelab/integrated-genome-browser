package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.operator.Operator;

public class TrackOperationAction extends TrackFunctionOperationA {

    private static final long serialVersionUID = 1L;

    public TrackOperationAction(Operator operator) {
        super(operator);
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        super.actionPerformed(e);
        addTier(Selections.allGlyphs);
    }
}
