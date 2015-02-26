/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.action;

import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.symmetry.RootSeqSymmetry;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import static com.affymetrix.igb.shared.Selections.allGlyphs;
import com.lorainelab.igb.genoviz.extensions.StyledGlyph;
import com.affymetrix.igb.tiers.TrackstylePropertyMonitor;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 *
 * @author dcnorris
 */
public class ToggleShowAsPairedAction extends SeqMapViewActionA {

    private static final long serialVersionUID = 1;

    //TODO register state in java preferences...
//     static {
//        GenericActionHolder.getInstance().addGenericAction(ACTION);
//        PreferenceUtils.saveToPreferences(UnibrowHairline.PREF_HAIRLINE_LABELED, UnibrowHairline.default_show_hairline_label, ACTION);
//    }
    private ToggleShowAsPairedAction() {
        super(BUNDLE.getString("toggleShowAsPaired"), KeyEvent.VK_UNDEFINED);
        putValue(SELECTED_KEY, false);
    }

    public static ToggleShowAsPairedAction getAction() {
        return ToggleShowAsPairedActionHolder.INSTANCE;
    }

    //Initialization-on-demand holder idiom
    private static class ToggleShowAsPairedActionHolder {

        private static final ToggleShowAsPairedAction INSTANCE = new ToggleShowAsPairedAction();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        for (StyledGlyph glyph : allGlyphs) {
            if (glyph.getInfo() == null || ((RootSeqSymmetry) glyph.getInfo()).getCategory() == null || glyph.getAnnotStyle() == null) {
                return;
            }
            if (((RootSeqSymmetry) glyph.getInfo()).getCategory() == FileTypeCategory.Alignment) {
                glyph.getAnnotStyle().setShowAsPaired(isSelected());
            }
        }
        refreshMap(false, false);
        TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
    }

    @Override
    public boolean isToggle() {
        return true;
    }

    public boolean isSelected() {
        return (Boolean) getValue(SELECTED_KEY);
    }
}
