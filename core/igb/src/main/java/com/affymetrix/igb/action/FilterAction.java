package com.affymetrix.igb.action;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.event.GenericActionHolder;
import com.affymetrix.genometry.filter.SymmetryFilterI;
import com.affymetrix.genometry.general.SupportsFileTypeCategory;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.shared.ConfigureOptionsPanel;
import com.affymetrix.igb.shared.Selections;
import org.lorainelab.igb.igb.genoviz.extensions.glyph.TierGlyph;
import com.affymetrix.igb.util.ConfigureFilters;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JOptionPane;

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

        final TierGlyph tg = getTierManager().getSelectedTiers().get(0);
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
        if (filter != null) {
            configurefilters.setFilter(filter.newInstance());
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
        dialog.setVisible(true);
    }

    @Override
    public boolean isEnabled() {
        return Selections.allGlyphs.size() > 0;
    }

    private void applyFilter(SymmetryFilterI filter, TierGlyph tg) {
        tg.getAnnotStyle().setFilter(filter);
        if (filter != null) {
            BioSeq annotseq = getSeqMapView().getAnnotatedSeq();
            for (GlyphI glyph : tg.getChildren()) {
                if (glyph.getInfo() != null) {
                    glyph.setVisibility(filter.filterSymmetry(annotseq, (SeqSymmetry) glyph.getInfo()));
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
