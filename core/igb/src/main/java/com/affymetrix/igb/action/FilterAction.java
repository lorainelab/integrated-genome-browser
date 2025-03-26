package com.affymetrix.igb.action;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.filter.ChainFilter;
import com.affymetrix.genometry.filter.SymmetryFilter;
import com.affymetrix.genometry.filter.SymmetryFilterI;
import com.affymetrix.genometry.general.SupportsFileTypeCategory;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.shared.ConfigureOptionsPanel;
import com.affymetrix.igb.shared.Selections;
import com.affymetrix.igb.tiers.TierLabelManager;
import com.affymetrix.igb.util.ConfigureFilters;
import org.lorainelab.igb.genoviz.extensions.glyph.TierGlyph;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 *
 * @author hiralv
 */
public class FilterAction extends SeqMapViewActionA {

    private static final long serialVersionUID = 1L;
    private static final FilterAction ACTION = new FilterAction();

    static {
        GenericActionHolder.getInstance().addGenericAction(ACTION);
    }

    public static FilterAction getAction() {
        return ACTION;
    }

    public FilterAction() {
        super("Filter...", "16x16/actions/hide.png", "22x22/actions/hide.png");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        super.actionPerformed(e);

        TierLabelManager tierLabelManager = getTierManager();
        final TierGlyph tg = tierLabelManager.getSelectedTiers().get(0);
        ITrackStyleExtended style = tg.getAnnotStyle();
        SymmetryFilterI filter = style.getFilter();

        ConfigureOptionsPanel.Filter<SymmetryFilterI> optionFilter = symmetryFilter -> {
            if (symmetryFilter instanceof SupportsFileTypeCategory) {
                Optional<FileTypeCategory> category = tg.getFileTypeCategory();
                if (category.isPresent()) {
                    return symmetryFilter.isFileTypeCategorySupported(category.get());
                } else {
                    return false;
                }
            }
            return true;
        };

        final ConfigureFilters configurefilters = new ConfigureFilters();
        configurefilters.setOptionsFilter(optionFilter);
        configurefilters.setTierLabelManager(tierLabelManager);
        if (filter != null) {
            if(filter instanceof ChainFilter chainFilter){
                List<SymmetryFilterI> newFilters = chainFilter.getFilters().stream().filter(symmetryFilterI ->
                        tierLabelManager.getSelectedTiers().stream()
                                .filter(tier -> !tier.getAnnotStyle().getUniqueName().isEmpty())
                                .anyMatch(tier -> isFilterSelected((SymmetryFilter) symmetryFilterI, tier))
                ).collect(Collectors.toList());
                ChainFilter modifiedChainFilter = new ChainFilter();
                modifiedChainFilter.setFilter(newFilters);
                configurefilters.setFilter(modifiedChainFilter);
            }
            else {
                boolean filterSelected = tierLabelManager.getSelectedTiers().stream()
                        .filter(tier -> !tier.getAnnotStyle().getUniqueName().isEmpty())
                        .anyMatch(tier -> isFilterSelected((SymmetryFilter) filter, tier));
                if(filterSelected)
                    configurefilters.setFilter(filter);
            }
        }

        JOptionPane optionPane = new JOptionPane(configurefilters, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
        optionPane.setValue(JOptionPane.UNINITIALIZED_VALUE);
        optionPane.setIcon(new ImageIcon(new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB)));

        optionPane.addPropertyChangeListener("value", evt -> {
            if (evt.getNewValue() instanceof Integer && (Integer) evt.getNewValue() == JOptionPane.OK_OPTION) {
                AbstractAction applyAction = new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        for (TierGlyph tier : getTierManager().getSelectedTiers()) {
                            applyFilter(configurefilters.getFilter(), tier);
                        }
                        getSeqMapView().getSeqMap().repackTheTiers(true, true);
                    }
                };
                getSeqMapView().preserveSelectionAndPerformAction(applyAction);
            }
        });

        JDialog dialog = optionPane.createDialog("Add/Remove/Edit Filters");
        dialog.setModal(false);
        dialog.setResizable(true);
        dialog.setSize(new Dimension(400,500));
        dialog.setVisible(true);
    }

    public static boolean isFilterSelected(SymmetryFilter symmetryFilterI, TierGlyph tier) {
        return symmetryFilterI.getFilterMap() != null && (symmetryFilterI.getFilterMap().containsKey(tier.getAnnotStyle().getUniqueName())
                && (symmetryFilterI.getFilterMap().get(tier.getAnnotStyle().getUniqueName()).contains(tier.getDirection().getDisplay())
                || symmetryFilterI.getFilterMap().get(tier.getAnnotStyle().getUniqueName()).contains(SymmetryFilter.Direction.BOTH.getDisplay())));
    }

    @Override
    public boolean isEnabled() {
        return Selections.allGlyphs.size() > 0;
    }

    private void applyFilter(SymmetryFilterI selectedTierFilter, TierGlyph tg) {
        SymmetryFilterI oldFilter = tg.getAnnotStyle().getFilter();
        SymmetryFilterI combinedTierFilter;
        List<SymmetryFilterI> notSelectedTierFilters = new ArrayList<>();
        if(oldFilter instanceof ChainFilter) {
            notSelectedTierFilters = ((ChainFilter) oldFilter).getFilters().stream()
                    .filter(filterI -> !isFilterSelected((SymmetryFilter) filterI, tg))
                    .toList();
        }
        else if (oldFilter != null && !isFilterSelected((SymmetryFilter) oldFilter, tg))
            notSelectedTierFilters = Collections.singletonList(oldFilter);
        if(!notSelectedTierFilters.isEmpty() && selectedTierFilter != null){
            combinedTierFilter = new ChainFilter();
            List<SymmetryFilterI> combinedTierFilters = new ArrayList<>(notSelectedTierFilters);
            if(selectedTierFilter instanceof ChainFilter chainFilter)
                combinedTierFilters.addAll(chainFilter.getFilters());
            else
                combinedTierFilters.add(selectedTierFilter);
            ((ChainFilter) combinedTierFilter).setFilter(combinedTierFilters);
        }
        else if (!notSelectedTierFilters.isEmpty()) {
            if (notSelectedTierFilters.size() > 1) {
                combinedTierFilter = new ChainFilter();
                List<SymmetryFilterI> combinedTierFilters = new ArrayList<>(notSelectedTierFilters);
                ((ChainFilter) combinedTierFilter).setFilter(combinedTierFilters);
            }
            else {
                combinedTierFilter = notSelectedTierFilters.get(0);
            }
        }
        else
            combinedTierFilter = selectedTierFilter;
        tg.getAnnotStyle().setFilter(combinedTierFilter);
        ArrayList<Boolean> bool = new ArrayList();
        if (selectedTierFilter != null) {
            BioSeq annotseq = getSeqMapView().getAnnotatedSeq();
            for (GlyphI glyph : tg.getChildren()) {
                if (glyph.getInfo() != null) {
                    bool.add(selectedTierFilter.filterSymmetry(annotseq, (SeqSymmetry) glyph.getInfo()));
                    glyph.setVisibility(selectedTierFilter.filterSymmetry(annotseq, (SeqSymmetry) glyph.getInfo()));
                } else {
                    // Should not ever happen
                    Logger.getLogger(FilterAction.class.getName()).log(Level.WARNING, "Found a glyph with null info at location {0}", glyph.getCoordBox());
                }
            }
        } else {
            for (GlyphI glyph : tg.getChildren()) {
                glyph.setVisibility(true);
            }
        }
    }
}
