package com.affymetrix.igb.action;

import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.symmetry.RootSeqSymmetry;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import static com.affymetrix.igb.shared.Selections.allGlyphs;

import com.affymetrix.igb.tiers.TrackstylePropertyMonitor;

/**
 *
 * @author hiralv
 */
public class ShadeUsingBaseQualityAction extends SeqMapViewActionA {

    private static final long serialVersionUID = 1L;
    private final static ShadeUsingBaseQualityAction shadeUsingBaseQualityAction = new ShadeUsingBaseQualityAction();

    public static ShadeUsingBaseQualityAction getAction() {
        return shadeUsingBaseQualityAction;
    }

    private ShadeUsingBaseQualityAction() {
        super(BUNDLE.getString("shadeBasedOnQualityScore"), null, null);
        this.setSelected(true);
    }

    @Override
    public void actionPerformed(java.awt.event.ActionEvent e) {
        super.actionPerformed(e);
        allGlyphs.stream().filter(glyph -> ((RootSeqSymmetry) glyph.getInfo()).getCategory() == FileTypeCategory.Alignment).forEach(glyph -> {
            glyph.getAnnotStyle().setShadeBasedOnQualityScore(isSelected());
        });
        refreshMap(false, false);
        TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
    }

    public void setSelected(boolean selected) {
        putValue(SELECTED_KEY, selected);
    }

    public boolean isSelected() {
        return (Boolean) getValue(SELECTED_KEY);
    }
}
