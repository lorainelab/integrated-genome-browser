package com.affymetrix.igb.view.layout;

import com.affymetrix.igb.view.layout.FasterExpandPacker;
import org.lorainelab.igb.igb.genoviz.extensions.GraphGlyphPosComparator;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.Rectangle;
import java.util.Collections;

/**
 *
 * @author hiralv
 */
public class GraphFasterExpandPacker extends FasterExpandPacker {

    private static final GraphGlyphPosComparator comparator = new GraphGlyphPosComparator();

    @Override
    public Rectangle pack(GlyphI parent, ViewI view) {
        int child_count = parent.getChildCount();
        if (child_count == 0) {
            return null;
        }
        Collections.sort(parent.getChildren(), comparator);
        return super.pack(parent, view);
    }
}
