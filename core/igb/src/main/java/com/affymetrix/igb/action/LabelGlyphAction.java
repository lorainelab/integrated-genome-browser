package com.affymetrix.igb.action;

import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.event.ParameteredAction;
import org.lorainelab.igb.genoviz.extensions.glyph.TierGlyph;

/**
 *
 * @author hiralv
 */
public class LabelGlyphAction extends SeqMapViewActionA implements ParameteredAction {

    private static final long serialVersionUID = 1L;
    private final static LabelGlyphAction ACTION = new LabelGlyphAction();

//	static{
//		GenericActionHolder.getInstance().addGenericAction(ACTION);
//	}
    public static LabelGlyphAction getAction() {
        return ACTION;
    }

    protected LabelGlyphAction() {
        super("Label Glyph", null, null);
    }

    private void setLabel(String labelField) {
        for (TierGlyph tier : getTierManager().getSelectedTiers()) {
            ITrackStyleExtended style = tier.getAnnotStyle();
            style.setLabelField(labelField);
        }
    }

    @Override
    public void performAction(Object... parameters) {
        if (parameters.length < 1 || parameters[0].getClass() != String.class) {
            return;
        }

        setLabel((String) parameters[0]);
    }
}
