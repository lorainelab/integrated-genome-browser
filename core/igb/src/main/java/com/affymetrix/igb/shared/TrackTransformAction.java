package com.affymetrix.igb.shared;

import com.lorainelab.igb.genoviz.extensions.api.StyledGlyph;
import com.affymetrix.genometryImpl.operator.Operator;
import java.util.ArrayList;
import java.util.List;

public class TrackTransformAction extends TrackFunctionOperationA {

    private static final long serialVersionUID = 1L;

    public TrackTransformAction(Operator operator) {
        super(operator);
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        super.actionPerformed(e);
        List<StyledGlyph> tiers;
        for (StyledGlyph glyph : Selections.allGlyphs) {
            tiers = new ArrayList<>();
            tiers.add(glyph);
            addTier(tiers);
        }
    }
}
