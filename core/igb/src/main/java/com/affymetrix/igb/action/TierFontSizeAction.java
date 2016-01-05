package com.affymetrix.igb.action;

import com.affymetrix.genometry.style.ITrackStyleExtended;
import org.lorainelab.igb.genoviz.extensions.glyph.GraphGlyph;
import com.affymetrix.genometry.event.ParameteredAction;
import static com.affymetrix.igb.shared.Selections.allGlyphs;
import org.lorainelab.igb.genoviz.extensions.glyph.StyledGlyph;

/**
 *
 * @author hiralv
 */
public class TierFontSizeAction extends SeqMapViewActionA implements ParameteredAction {

    private static final long serialVersionUID = 1L;
    private final static TierFontSizeAction ACTION = new TierFontSizeAction();

    public static TierFontSizeAction getAction() {
        return ACTION;
    }

    protected TierFontSizeAction() {
        super("Label Size", null, null);
    }

    private void setFontSize(int fontsize) {
        for (StyledGlyph sg : allGlyphs) {
            ITrackStyleExtended style = sg.getAnnotStyle();
            if (style != null) {
                style.setTrackNameSize(fontsize);
            }

            //If graphs is joined then apply color to combo style too.
            if (sg instanceof GraphGlyph) {
                style = ((GraphGlyph) sg).getGraphState().getComboStyle();
                if (style != null) {
                    style.setTrackNameSize(fontsize);
                }
            }
        }
    }

    @Override
    public void performAction(Object... parameters) {
        if (parameters.length < 1 || parameters[0].getClass() != Integer.class) {
            return;
        }

        setFontSize((Integer) parameters[0]);
    }
}
