package com.affymetrix.igb.action;

import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.event.SymSelectionEvent;
import com.affymetrix.genometry.event.SymSelectionListener;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.igb.services.registry.MapTierTypeRegistry;
import com.affymetrix.igb.tiers.TrackStylePropertyListener;
import com.affymetrix.igb.tiers.TrackstylePropertyMonitor;
import com.affymetrix.igb.view.SeqMapView;
import org.lorainelab.igb.genoviz.extensions.glyph.TierGlyph;
import java.awt.event.ActionEvent;
import java.util.EventObject;
import java.util.List;
import java.util.Optional;

public abstract class ShowStrandActionA extends SeqMapViewActionA implements SymSelectionListener, TrackStylePropertyListener {

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

    private void setTwoTiers(boolean b) {
        getTierManager().getSelectedTierGlyphStyles().forEach(style -> style.setSeparate(b));
        refreshMap(false, true);
        getTierManager().sortTiers();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);
        setTwoTiers(separateStrands);
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
                        if (MapTierTypeRegistry.supportsTwoTrack(category.get())) {
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
