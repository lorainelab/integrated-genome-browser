package com.affymetrix.igb.action;

import com.affymetrix.genometry.event.SymSelectionEvent;
import com.affymetrix.genometry.event.SymSelectionListener;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.lorainelab.igb.genoviz.extensions.TierGlyph;
import com.affymetrix.igb.tiers.TrackstylePropertyMonitor;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.event.ActionEvent;
import java.util.List;

/**
 * Some code common to collapsing and expanding tiers.
 */
public abstract class CollapseExpandActionA extends SeqMapViewActionA implements SymSelectionListener {

    private static final long serialVersionUID = 1L;
    protected boolean collapsedTracks;

    protected CollapseExpandActionA(String text, String iconPath, String largeIconPath) {
        super(text, iconPath, largeIconPath);
    }

    private void setTiersCollapsed(List<TierLabelGlyph> tier_labels, boolean collapsed) {
        getTierManager().setTiersCollapsed(tier_labels, collapsed);
        getSeqMapView().getSeqMap().updateWidget();
    }

    /**
     * Expand or collapse as appropriate. Then enable or disable ourself and our
     * partner as appropriate.
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        setTiersCollapsed(getTierManager().getSelectedTierLabels(), collapsedTracks);
        TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
        SeqMapView gviewer = getSeqMapView();
        List<SeqSymmetry> selected_syms = SeqMapView.glyphsToSyms(getTierManager().getSelectedTiers());
        changeActionDisplay(selected_syms);
    }

    /**
     * Override to enable or disable self and partner based on tracks selected.
     */
    @Override
    public void symSelectionChanged(SymSelectionEvent evt) {

		// Only pay attention to selections from the main SeqMapView or its map.
        // Ignore the splice view as well as events coming from this class itself.
        Object src = evt.getSource();
        SeqMapView gviewer = getSeqMapView();
        if (!(src == gviewer || src == gviewer.getSeqMap())) {
            return;
        }

        List<SeqSymmetry> selected_syms = SeqMapView.glyphsToSyms(getTierManager().getSelectedTiers());
        changeActionDisplay(selected_syms);
    }

    /**
     * Subclasses should enable and disable self and partner.
     *
     * @param hasCollapsed if some selected tier is collapsed.
     * @param hasExpanded if some selected tier is expanded.
     */
    protected abstract void processChange(boolean hasCollapsed, boolean hasExpanded);

    private void changeActionDisplay(List<SeqSymmetry> selected_syms) {
        boolean hasCollapsed = false;
        boolean hasExpanded = false;
        for (TierGlyph tg : getSeqMapView().getTierManager().getVisibleTierGlyphs()) {
            if (tg.getTierType() == TierGlyph.TierType.ANNOTATION || tg.getTierType() == TierGlyph.TierType.GRAPH) {
                SeqSymmetry ss = (SeqSymmetry) tg.getInfo();
                if (selected_syms.contains(ss) && tg.getAnnotStyle().getExpandable()) {
                    boolean collapsed = tg.getAnnotStyle().getCollapsed();
                    hasCollapsed |= collapsed;
                    hasExpanded |= !collapsed;
                }
            }
        }
        processChange(hasCollapsed, hasExpanded);
    }

}
