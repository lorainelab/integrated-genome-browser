package com.affymetrix.igb.action;

import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.event.SymSelectionEvent;
import com.affymetrix.genometry.event.SymSelectionListener;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.igb.shared.MapTierTypeHolder;
import com.lorainelab.igb.genoviz.extensions.TierGlyph;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.event.ActionEvent;
import java.util.EventObject;
import java.util.List;
import java.util.Optional;

public abstract class ShowStrandActionA extends SeqMapViewActionA
        implements SymSelectionListener, TrackstylePropertyMonitor.TrackStylePropertyListener {

    private static final long serialVersionUID = 1L;
    protected boolean separateStrands;

    protected final void listenUp() {
        GenometryModel.getInstance().addSymSelectionListener(this);
        TrackstylePropertyMonitor.getPropertyTracker().addPropertyListener(this);
    }

    protected ShowStrandActionA(String text, String iconPath, String largeIconPath) {
        super(text, iconPath, largeIconPath);
        listenUp();
    }

    private void setTwoTiers(List<TierLabelGlyph> tier_label_glyphs, boolean b) {
        for (TierLabelGlyph tlg : tier_label_glyphs) {
            TierGlyph tier = (TierGlyph) tlg.getInfo();
            ITrackStyleExtended style = tier.getAnnotStyle();
            Optional<FileTypeCategory> category = tier.getFileTypeCategory();
            if (category.isPresent()) {
                if (!b || MapTierTypeHolder.supportsTwoTrack(category.get())) {
                    style.setSeparate(b);
                }
            }
        }
        refreshMap(false, true);
        getTierManager().sortTiers();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        setTwoTiers(getTierManager().getSelectedTierLabels(), separateStrands);
        TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
        List<SeqSymmetry> selected_syms = SeqMapView.glyphsToSyms(getTierManager().getSelectedTiers());
        changeStrandActionDisplay(selected_syms);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void symSelectionChanged(SymSelectionEvent evt) {
        SeqMapView gviewer = getSeqMapView();
        List<SeqSymmetry> selected_syms = SeqMapView.glyphsToSyms(getTierManager().getSelectedTiers());
        // Only pay attention to selections from the main SeqMapView or its map.
        // Ignore the splice view as well as events coming from this class itself.

        Object src = evt.getSource();
        if (!(src == gviewer || src == gviewer.getSeqMap())) {
            return;
        }

        changeStrandActionDisplay(selected_syms);
    }

    protected abstract void processChange(boolean hasSeparate, boolean hasMixed);

    private void changeStrandActionDisplay(List<SeqSymmetry> selected_syms) {
        boolean hasSeparate = false;
        boolean hasMixed = false;
        for (TierGlyph tg : getSeqMapView().getTierManager().getVisibleTierGlyphs()) {
            SeqSymmetry ss = (SeqSymmetry) tg.getInfo();
            if (selected_syms.contains(ss)) {
                if (tg.getTierType() != TierGlyph.TierType.GRAPH) {
                    Optional<FileTypeCategory> category = tg.getFileTypeCategory();
                    if (category.isPresent()) {
                        if (MapTierTypeHolder.supportsTwoTrack(category.get())) {
                            boolean separate = tg.getAnnotStyle().getSeparate();
                            hasSeparate |= separate;
                            hasMixed |= !separate;
                        }
                    }
                }
            }
        }
        processChange(hasSeparate, hasMixed);
    }

    @Override
    public void trackstylePropertyChanged(EventObject eo) {
        List<SeqSymmetry> selected_syms;
        SeqMapView gviewer = getSeqMapView();
        selected_syms = SeqMapView.glyphsToSyms(getTierManager().getSelectedTiers());
        changeStrandActionDisplay(selected_syms);
    }

}
