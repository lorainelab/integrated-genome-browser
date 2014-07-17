package com.affymetrix.igb.shared;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.widget.tieredmap.CollapsedTierPacker;
import java.awt.Rectangle;
import java.util.Collections;

/**
 *
 * @author hiralv
 */
public class GraphCollapsedPacker extends CollapsedTierPacker {

    private static final GraphGlyphPosComparator comparator = new GraphGlyphPosComparator();

    @Override
    public Rectangle pack(GlyphI parent, ViewI view) {
        int child_count = parent.getChildCount();
        if (child_count == 0) {
            return null;
        }
        Collections.sort(parent.getChildren(), comparator);
        Collections.reverse(parent.getChildren());
        maxHeight = 0;
        return super.pack(parent, view);
    }

}
