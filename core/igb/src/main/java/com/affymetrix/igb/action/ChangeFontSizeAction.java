package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.util.ErrorHandler;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import com.affymetrix.igb.shared.Selections;
import com.lorainelab.igb.genoviz.extensions.TierGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.tiers.TrackConstants;
import com.affymetrix.igb.tiers.TrackStyle;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.JOptionPane;

public class ChangeFontSizeAction extends SeqMapViewActionA {

    private static final long serialVersionUID = 1L;
    private static final ChangeFontSizeAction ACTION = new ChangeFontSizeAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static ChangeFontSizeAction getAction() {
        return ACTION;
    }

    private ChangeFontSizeAction() {
        super(BUNDLE.getString("changeFontSizeAction"), "16x16/actions/font_size.png",
                "22x22/actions/font_size.png");
    }

    private void changeFontSize(List<TierLabelGlyph> tier_label_glyphs, float size) {
        for (TierLabelGlyph tlg : tier_label_glyphs) {
            TierGlyph tier = (TierGlyph) tlg.getInfo();
            ITrackStyleExtended style = tier.getAnnotStyle();
            if (style != null) {
                style.setTrackNameSize(size);
            }
        }
        getSeqMapView().getSeqMap().updateWidget();
    }

    private void changeFontSize(List<TierLabelGlyph> tier_labels) {
        if (tier_labels == null || tier_labels.isEmpty()) {
            ErrorHandler.errorPanel("changeExpandMaxAll called with an empty list");
            return;
        }

        Object initial_value = TrackStyle.default_track_name_size;
        if (tier_labels.size() == 1) {
            TierLabelGlyph tlg = tier_labels.get(0);
            TierGlyph tg = (TierGlyph) tlg.getInfo();
            ITrackStyleExtended style = tg.getAnnotStyle();
            if (style != null) {
                initial_value = style.getTrackNameSize();
            }
        }

        Object input = JOptionPane.showInputDialog(null, BUNDLE.getString("selectFontSize"), BUNDLE.getString("changeSelectedTrackFontSize"), JOptionPane.PLAIN_MESSAGE, null,
                TrackConstants.SUPPORTED_SIZE, initial_value);

        if (input == null) {
            return;
        }

        changeFontSize(tier_labels, (Float) input);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        changeFontSize(getTierManager().getSelectedTierLabels());
        TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
    }

    @Override
    public boolean isEnabled() {
        return Selections.allGlyphs.size() > 0;
    }
}
