package com.affymetrix.igb.graphTypes;

import com.affymetrix.genometry.style.GraphType;
import com.affymetrix.genoviz.bioviews.ViewI;
import org.lorainelab.igb.igb.genoviz.extensions.glyph.GraphGlyph;

/**
 *
 * @author hiralv
 */
public class MismatchGraphType extends FillBarGraphType {

    protected static final double mismatch_transition_scale = 30;

    public MismatchGraphType(GraphGlyph graphGlyph) {
        super(graphGlyph);
        graphGlyph.getGraphState().lockGraphStyle();
    }

    @Override
    public String getName() {
        return "mismatchgraph";
    }

    @Override
    public GraphType getGraphStyle() {
        return null;
    }

    @Override
    public void draw(ViewI view) {
        double xpixels_per_coord = (view.getTransform()).getScaleX();
        double xcoords_per_pixel = 1 / xpixels_per_coord;
        if (xcoords_per_pixel < mismatch_transition_scale) {
            oldDraw(view);
        } else {
            drawSmart(view);
        }
    }
}
