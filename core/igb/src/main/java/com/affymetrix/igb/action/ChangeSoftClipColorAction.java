package com.affymetrix.igb.action;

import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.symmetry.RootSeqSymmetry;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import static com.affymetrix.igb.shared.Selections.allGlyphs;
import com.affymetrix.igb.tiers.TrackstylePropertyMonitor;
import org.lorainelab.igb.genoviz.extensions.glyph.StyledGlyph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChangeSoftClipColorAction extends SeqMapViewActionA {

    private static final Logger logger = LoggerFactory.getLogger(ChangeSoftClipColorAction.class);
    private static final long serialVersionUID = 1L;
    private final static ChangeSoftClipColorAction changeSoftClipColorAction = new ChangeSoftClipColorAction();

    public static ChangeSoftClipColorAction getAction() {
        return changeSoftClipColorAction;
    }

    private ChangeSoftClipColorAction() {
        super(BUNDLE.getString("softClipColor"), null, null);
        this.setSelected(true);
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        super.actionPerformed(e);
        for (StyledGlyph glyph : allGlyphs) {
            if (glyph.getInfo() == null || ((RootSeqSymmetry) glyph.getInfo()).getCategory() == null || glyph.getAnnotStyle() == null) {
                return;
            }
            if (((RootSeqSymmetry) glyph.getInfo()).getCategory() == FileTypeCategory.Alignment) {
                SoftClipColorAction.getAction().actionPerformed(e);
                glyph.getAnnotStyle().setShowSoftClipCustomColor(isSelected());
                glyph.getAnnotStyle().setShowSoftClippedResidues(false);
                glyph.getAnnotStyle().setShowSoftClipped(false);
                glyph.getAnnotStyle().setShowSoftClipDefaultColor(false);
            }
        }
        refreshMap(false, false);
        TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
        logger.info("Clicked");

    }

    public void setSelected(boolean selected) {
        putValue(SELECTED_KEY, selected);
    }

    public boolean isSelected() {
        return (Boolean) getValue(SELECTED_KEY);
    }
}