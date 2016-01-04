package org.lorainelab.igb.igb.genoviz.extensions;

import com.affymetrix.genometry.style.GraphState;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.comparator.GlyphMinXComparator;
import org.lorainelab.igb.igb.genoviz.extensions.glyph.GraphGlyph;
import java.util.Comparator;

/**
 *
 * @author hiralv
 */
public class GraphGlyphPosComparator implements Comparator<GlyphI> {

    private static final Comparator<GlyphI> child_sorter = new GlyphMinXComparator();

    @Override
    public int compare(GlyphI g1, GlyphI g2) {
        if (!(g1 instanceof GraphGlyph) || !(g2 instanceof GraphGlyph)) {
            return Double.compare(g1.getCoordBox().x, g2.getCoordBox().x);
        }

        GraphGlyph gg1 = (GraphGlyph) g1;
        GraphState gs1 = gg1.graf.getGraphState();

        GraphGlyph gg2 = (GraphGlyph) g2;
        GraphState gs2 = gg2.graf.getGraphState();

        int ret = Double.compare(gs1.getPosition(), gs2.getPosition());

        if (ret == 0) {
            ret = child_sorter.compare(g1, g2);
        }

        return ret;
    }
}
