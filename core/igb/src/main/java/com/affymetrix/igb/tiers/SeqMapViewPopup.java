/**
 * Copyright (c) 2005-2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.tiers;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.genometry.event.GenericAction;
import com.affymetrix.genometry.general.IParameters;
import com.affymetrix.genometry.operator.Operator;
import com.affymetrix.genometry.operator.service.OperatorServiceRegistry;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.style.ITrackStyleExtended;
import com.affymetrix.genometry.symmetry.RootSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.util.IDComparator;
import static com.affymetrix.igb.IGBConstants.BUNDLE;
import static com.affymetrix.igb.IGBConstants.GENOME_SEQ_ID;
import com.affymetrix.igb.action.ChangeBackgroundColorAction;
import com.affymetrix.igb.action.ChangeExpandMaxAction;
import com.affymetrix.igb.action.ChangeFontSizeAction;
import com.affymetrix.igb.action.ChangeForegroundColorAction;
import com.affymetrix.igb.action.ChangeLabelColorAction;
import com.affymetrix.igb.action.ChangeTierHeightAction;
import com.affymetrix.igb.action.CloseTracksAction;
import com.affymetrix.igb.action.CollapseAction;
import com.affymetrix.igb.action.ColorByAction;
import com.affymetrix.igb.action.CustomizeAction;
import com.affymetrix.igb.action.ExpandAction;
import com.affymetrix.igb.action.ExportFileAction;
import com.affymetrix.igb.action.ExportSelectedAnnotationFileAction;
import com.affymetrix.igb.action.FilterAction;
import com.affymetrix.igb.action.HideAction;
import com.affymetrix.igb.action.RemoveDataFromTracksAction;
import com.affymetrix.igb.action.RenameTierAction;
import com.affymetrix.igb.action.ShadeUsingBaseQualityAction;
import com.affymetrix.igb.action.ShowAllAction;
import com.affymetrix.igb.action.ShowMinusStrandAction;
import com.affymetrix.igb.action.ShowMismatchAction;
import com.affymetrix.igb.action.ShowPlusStrandAction;
import com.affymetrix.igb.action.ToggleShowAsPairedAction;
import com.affymetrix.igb.shared.ChangeExpandMaxOptimizeAction;
import com.affymetrix.igb.shared.RepackTiersAction;
import com.affymetrix.igb.shared.Selections;
import com.lorainelab.igb.genoviz.extensions.api.StyledGlyph;
import com.lorainelab.igb.genoviz.extensions.api.TierGlyph;
import com.affymetrix.igb.shared.TrackListProvider;
import com.affymetrix.igb.shared.TrackOperationAction;
import com.affymetrix.igb.shared.TrackOperationWithParametersAction;
import com.affymetrix.igb.shared.TrackUtils;
import com.affymetrix.igb.swing.JRPMenuItem;
import com.affymetrix.igb.tiers.AffyTieredMap.ActionToggler;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.factories.DefaultTierGlyph;
import com.affymetrix.igb.view.load.GeneralLoadView;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.swing.border.Border;
import org.apache.commons.lang3.StringUtils;

public final class SeqMapViewPopup implements TierLabelManager.PopupListener {

    private static final boolean DEBUG = false;
    private final SeqMapView gviewer;
    private final TierLabelManager tierLabelManager;
    private final JMenu strandsMenu = new JMenu(BUNDLE.getString("strandsMenu"));
    private final ActionToggler at1;
    private final ActionToggler at2;
    private final RepackTiersAction repackStub;

    public SeqMapViewPopup(TierLabelManager tierLabelManager, SeqMapView smv) {
        this.tierLabelManager = tierLabelManager;
        this.gviewer = smv;
        this.repackStub = new RepackTiersAction(null, null, null) {
            private static final long serialVersionUID = 1L;
        };
        at1 = new ActionToggler(smv.getClass().getSimpleName() + "_SeqMapViewPopup.showPlus", ShowPlusStrandAction.getAction());
        at2 = new ActionToggler(smv.getClass().getSimpleName() + "_SeqMapViewPopup.showMinus", ShowMinusStrandAction.getAction());
    }

    public void refreshMap(boolean stretchVertically, boolean stretchHorizonatally) {
        if (gviewer != null) {
            // if an AnnotatedSeqViewer is being used, ask it to update itself.
            // later this can be made more specific to just update the tiers that changed
            boolean preserveViewX = !stretchVertically;
            boolean preserveViewY = !stretchHorizonatally;
            gviewer.updatePanel(preserveViewX, preserveViewY);
        } else {
            // if no AnnotatedSeqViewer (as in simple test programs), update the tiermap itself.
            tierLabelManager.repackTheTiers(false, stretchVertically);
        }
    }

    public void repack(final boolean fullRepack, boolean tierChanged) {
        repackStub.repack(fullRepack, tierChanged);
    }

    private JMenu addOperationMenu(List<? extends SeqSymmetry> syms) {
        JMenu operationsMenu = new JMenu(BUNDLE.getString("operationsMenu"));
        if (GENOME_SEQ_ID.equals(gviewer.getAnnotatedSeq().getID())) {
            return operationsMenu;
        }
        TreeSet<Operator> operators = new TreeSet<>(new IDComparator());
        operators.addAll(OperatorServiceRegistry.getOperators());
        for (Operator operator : operators) {
            if (TrackUtils.getInstance().checkCompatible(syms, operator, true)) {
                String title = operator.getDisplay();
                Operator newOperator = operator.newInstance();
                if (newOperator == null) {
                    Logger.getLogger(SeqMapViewPopup.class.getName()).log(Level.SEVERE, "Could not create instance for operator {0}", title);
                    continue;
                }

                Map<String, Class<?>> params = operator instanceof IParameters ? ((IParameters) operator).getParametersType() : null;
                
                if (params != null && !params.isEmpty()) {
                    JMenu operatorSMI = new JMenu(title);

                    JMenuItem operatorMI = new JMenuItem("Use Default");
                    operatorMI.addActionListener(new TrackOperationAction(newOperator));
                    operatorSMI.add(operatorMI);

                    operatorMI = new JMenuItem("Configure...");
                    operatorMI.addActionListener(new TrackOperationWithParametersAction(newOperator));
                    operatorSMI.add(operatorMI);

                    operationsMenu.add(operatorSMI);
                } else {
                    JMenuItem operatorMI = new JMenuItem(title);
                    operatorMI.addActionListener(new TrackOperationAction(newOperator));
                    operationsMenu.add(operatorMI);
                }
            }
        }
        return operationsMenu;
    }

    private JMenu addChangeMenu(int numSelections, boolean anyAreExpanded, boolean anyAreSeparateTiers, boolean anyAreSingleTier, boolean anyAreColorOff, boolean coordinatesTrackSelected) {
        JMenu changeMenu = new JMenu(BUNDLE.getString("changeMenu"));
        JMenuItem change_foreground_color = new JRPMenuItemTLP(ChangeForegroundColorAction.getAction());
        change_foreground_color.setEnabled(numSelections > 0);
        changeMenu.add(change_foreground_color);
        JMenuItem change_background_color = new JRPMenuItemTLP(ChangeBackgroundColorAction.getAction());
        change_background_color.setEnabled(numSelections > 0);
        changeMenu.add(change_background_color);
        JMenuItem change_label_color = new JRPMenuItemTLP(ChangeLabelColorAction.getAction());
        change_label_color.setEnabled(numSelections > 0);
        changeMenu.add(change_label_color);
        JMenuItem rename = new JRPMenuItemTLP(RenameTierAction.getAction());
        rename.setEnabled(numSelections == 1);
        changeMenu.add(rename);
        JMenuItem change_font_size = new JRPMenuItemTLP(ChangeFontSizeAction.getAction());
        change_font_size.setEnabled(numSelections > 0);
        changeMenu.add(change_font_size);
        JMenuItem change_Tier_Height = new JRPMenuItemTLP(ChangeTierHeightAction.getAction());
        if (numSelections > 0 && !(tierLabelManager.getSelectedTierLabels().get(0).getReferenceTier().getAnnotStyle().getTrackName().equals(TrackConstants.NAME_OF_COORDINATE_INSTANCE))
                && (((DefaultTierGlyph) (tierLabelManager.getSelectedTierLabels().get(0).getReferenceTier())).isHeightFixed())) {
            change_Tier_Height.setEnabled(true);
        } else {
            change_Tier_Height.setEnabled(false);
        }
        changeMenu.add(change_Tier_Height);
        JMenuItem change_expand_max = new JRPMenuItemTLP(ChangeExpandMaxAction.getAction());
        change_expand_max.setEnabled(anyAreExpanded);
        changeMenu.add(change_expand_max);
        return changeMenu;
    }

    private JMenu addShowMenu(boolean containHiddenTiers) {
        final JMenu showMenu = new JMenu(BUNDLE.getString("showMenu"));
        showMenu.removeAll();
        JMenuItem showAllAction = new JMenuItem(ShowAllAction.getAction());
        showAllAction.setIcon(null);
        showAllAction.setText("All");
        showMenu.add(showAllAction);
        showMenu.add(new JSeparator());
        //showMenu.setEnabled(false);
        List<TierLabelGlyph> tiervec = tierLabelManager.getAllTierLabels();

        for (TierLabelGlyph label : tiervec) {
            TierGlyph tier = (TierGlyph) label.getInfo();
            final ITrackStyleExtended style = tier.getAnnotStyle();
            if (style != null && !style.getShow() && tier.getDirection() != StyledGlyph.Direction.REVERSE) {
                final JMenuItem show_tier = new JMenuItem() {

                    private static final long serialVersionUID = 1L;
                    // override getText() because the HumanName of the style might change

                    @Override
                    public String getText() {
                        String name = style.getTrackName();
                        if (name == null) {
                            name = "<" + BUNDLE.getString("unnamed") + ">";
                        }
                        if (name.length() > 30) {
                            name = name.substring(0, 30) + "...";
                        }
                        return name;
                    }
                };
                show_tier.setName(style.getMethodName());
                show_tier.setAction(new AbstractAction() {

                    private static final long serialVersionUID = 1L;

                    @Override
                    public void actionPerformed(ActionEvent e) {
                        style.setShow(true);
                        showMenu.remove(show_tier);
                        tierLabelManager.sortTiers();
                        GeneralLoadView.getLoadView().refreshDataManagementView();
                        repack(false, true);
                    }
                });
                showMenu.add(show_tier);
                containHiddenTiers = true;
            }
        }
        showMenu.setEnabled(containHiddenTiers);
        return showMenu;
    }

    @Override
    public void popupNotify(javax.swing.JPopupMenu popup, final TierLabelManager handler) {
        int numSelections = Selections.allGlyphs.size();
        boolean anyAreCollapsed = false;
        boolean anyAreExpanded = false;
        boolean coordinatesTrackSelected = false;
        boolean containHiddenTiers = false;
        boolean anyAlignment = false;
        boolean anyShowResidueMask = false;
        boolean anyShadeBasedOnQuality = false;
        boolean allSameCategory = numSelections > 0;
        int noOfLocked = 0;
        FileTypeCategory category = numSelections > 0 && Selections.allGlyphs.get(0).getInfo() != null
                ? ((RootSeqSymmetry) Selections.allGlyphs.get(0).getInfo()).getCategory()
                : null;

        for (StyledGlyph glyph : Selections.allGlyphs) {
            ITrackStyleExtended astyle = glyph.getAnnotStyle();

            if (astyle.getExpandable()) {
                anyAreCollapsed = anyAreCollapsed || astyle.getCollapsed();
                anyAreExpanded = anyAreExpanded || !astyle.getCollapsed();
            }

            if (astyle.getShow() && glyph instanceof DefaultTierGlyph && ((DefaultTierGlyph) glyph).isHeightFixed()) {
                noOfLocked++;
            }

            if (glyph.getInfo() != null && ((RootSeqSymmetry) glyph.getInfo()).getCategory() != category) {
                allSameCategory = false;
            }

            if (glyph.getInfo() != null && ((RootSeqSymmetry) glyph.getInfo()).getCategory() == FileTypeCategory.Alignment) {
                anyAlignment = true;
                anyShowResidueMask = anyShowResidueMask || astyle.getShowResidueMask();
                anyShadeBasedOnQuality = anyShadeBasedOnQuality || astyle.getShadeBasedOnQualityScore();
            }

            if (astyle == CoordinateStyle.coordinate_annot_style) {
                coordinatesTrackSelected = true;
            }

            if (!astyle.getShow()) {
                containHiddenTiers = true;
            }

        }

        StyledGlyph styledGlyph = (numSelections == 1 ? Selections.allGlyphs.get(0) : null);

        Border emptyBorder = BorderFactory.createEmptyBorder(5, 5, 5, 5);
        Border colorBorder, finalBorder;
        if (numSelections == 1 && styledGlyph != null) {
            colorBorder = BorderFactory.createLineBorder(styledGlyph.getAnnotStyle().getForeground());
            finalBorder = BorderFactory.createCompoundBorder(colorBorder, emptyBorder);

        } else {
            colorBorder = BorderFactory.createLineBorder(Color.BLACK);
            finalBorder = BorderFactory.createCompoundBorder(colorBorder, emptyBorder);
        }
        popup.setBorder(finalBorder);

        JMenuItem optimizeStackHeight = new JRPMenuItemTLP(ChangeExpandMaxOptimizeAction.getAction());
        optimizeStackHeight.setIcon(null);
        optimizeStackHeight.setEnabled(Selections.annotSyms.size() > 0);
        optimizeStackHeight.setText("Optimize Stack Height");
        popup.add(optimizeStackHeight);
        JMenuItem changeExpandMax = new JRPMenuItemTLP(ChangeExpandMaxAction.getAction());
        changeExpandMax.setText("Set Stack Height...");
        changeExpandMax.setIcon(null);
        popup.add(changeExpandMax);
        JMenuItem hide = new JMenuItem();
        hide.setAction(HideAction.getAction());
        hide.setIcon(null);
        hide.setEnabled(numSelections > 0);
        popup.add(hide);
        JMenu showMenu = addShowMenu(containHiddenTiers);
        popup.add(showMenu);
        showMenu.getPopupMenu().setBorder(finalBorder);
        JMenuItem collapse = new JCheckBoxMenuItem();
        if ((!anyAreExpanded && !anyAreCollapsed) || (anyAreExpanded && anyAreCollapsed) || coordinatesTrackSelected) {
            collapse.setEnabled(false);
        } else if (anyAreExpanded) {
            collapse.setAction(CollapseAction.getAction());
            collapse.setSelected(false);
        } else if (anyAreCollapsed) {
            collapse.setAction(ExpandAction.getAction());
            collapse.setSelected(true);
        }
        collapse.setText("Collapse");
        collapse.setIcon(null);
        popup.add(collapse);
        
        JMenuItem customize = new JRPMenuItemTLP(CustomizeAction.getAction());
        customize.setIcon(null);
        customize.setText("Customize...");
        popup.add(customize);

        popup.add(new JSeparator());
        JCheckBoxMenuItem showResidueMask = new JCheckBoxMenuItem(ShowMismatchAction.getAction());
        showResidueMask.setEnabled(anyAlignment);
        showResidueMask.setSelected(anyAlignment && anyShowResidueMask);
        popup.add(showResidueMask);
        JCheckBoxMenuItem useBaseQuality = new JCheckBoxMenuItem(ShadeUsingBaseQualityAction.getAction());
        useBaseQuality.setEnabled(anyAlignment);
        useBaseQuality.setSelected(anyAlignment && anyShadeBasedOnQuality);
        popup.add(useBaseQuality);
        popup.add(new JSeparator());

        JMenu operationsMenu = addOperationMenu(Selections.rootSyms);
        popup.add(operationsMenu);
        operationsMenu.getPopupMenu().setBorder(finalBorder);
        operationsMenu.setEnabled(operationsMenu.getItemCount() > 0);
        operationsMenu.setEnabled(!coordinatesTrackSelected);

        popup.add(new JSeparator());

        JMenuItem setColorBy = new JRPMenuItemTLP(ColorByAction.getAction());
        setColorBy.setIcon(null);
        setColorBy.setEnabled(allSameCategory && (category == FileTypeCategory.Annotation
                || category == FileTypeCategory.Alignment || category == FileTypeCategory.ProbeSet));
        popup.add(setColorBy);

        JMenuItem filterAction = new JRPMenuItemTLP(FilterAction.getAction());
        filterAction.setIcon(null);
        filterAction.setEnabled(allSameCategory && (category == FileTypeCategory.Annotation
                || category == FileTypeCategory.Alignment || category == FileTypeCategory.ProbeSet));
        popup.add(filterAction);

        popup.add(new JSeparator());

        JMenuItem saveSelectedAnnotations = new JRPMenuItemTLP(ExportSelectedAnnotationFileAction.getAction());
        saveSelectedAnnotations.setEnabled(styledGlyph != null && styledGlyph instanceof TierGlyph
                && !((TierGlyph) styledGlyph).getSelected().isEmpty() && ExportSelectedAnnotationFileAction.getAction().isExportable(styledGlyph.getFileTypeCategory()));
        saveSelectedAnnotations.setIcon(null);
        popup.add(saveSelectedAnnotations);
        JMenuItem saveTrack = new JRPMenuItemTLP(ExportFileAction.getAction());
        saveTrack.setEnabled(numSelections == 1 && !coordinatesTrackSelected && styledGlyph.getInfo() != null && ExportSelectedAnnotationFileAction.getAction().isExportable(styledGlyph.getFileTypeCategory()));
        saveTrack.setIcon(null);
        popup.add(saveTrack);

        //for now do not allow multiselect action for show as paired, but this can be added easily if desired
        if (!handler.getSelectedTiers().isEmpty() && !coordinatesTrackSelected && handler.getSelectedTiers().size() == 1) {
            boolean canShowAsPaired = false;
            for (TierGlyph tierGlyph : handler.getSelectedTiers()) {
                String methodName = tierGlyph.getAnnotStyle().getMethodName();
                if (StringUtils.endsWithIgnoreCase(methodName, "bam") || StringUtils.endsWithIgnoreCase(methodName, "sam")) {
                    canShowAsPaired = true;
                }
            }
            if (canShowAsPaired) {
                JCheckBoxMenuItem showAsPaired = new JCheckBoxMenuItem(ToggleShowAsPairedAction.getAction());
                TierGlyph glyph = handler.getSelectedTiers().get(0);
                showAsPaired.setSelected(glyph.getAnnotStyle().isShowAsPaired());
                popup.add(showAsPaired);
            }
        }

        popup.add(
                new JSeparator());

        JMenuItem removeDataFromTracks = new JRPMenuItemTLP(RemoveDataFromTracksAction.getAction());

        removeDataFromTracks.setText(
                "Clear Data");
        removeDataFromTracks.setEnabled(Selections.rootSyms.size() > 0);
        removeDataFromTracks.setIcon(
                null);
        popup.add(removeDataFromTracks); // Remove data from selected tracks.

        JMenuItem deleteTrack = new JRPMenuItemTLP(CloseTracksAction.getAction());

        deleteTrack.setText(
                "Delete Track");
        deleteTrack.setEnabled(
                !coordinatesTrackSelected);
        deleteTrack.setIcon(
                null);
        popup.add(deleteTrack);

        if (DEBUG) {
            popup.add(new AbstractAction("DEBUG") {

                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    doDebugAction();
                }
            });
        }
    }

// purely for debugging
    private void doDebugAction() {
        for (TierGlyph tg : tierLabelManager.getSelectedTiers()) {
            ITrackStyleExtended style = tg.getAnnotStyle();
            System.out.println("Track: " + tg);
            System.out.println("Style: " + style);
        }
    }

    SeqMapView getSeqMapView() {
        return gviewer;

    }

    private class JRPMenuItemTLP extends JRPMenuItem implements TrackListProvider {

        private static final long serialVersionUID = 1L;

        private JRPMenuItemTLP(GenericAction genericAction) {
            super("Toolbar_" + genericAction.getId(), genericAction);
        }

        @Override
        public List<TierGlyph> getTrackList() {
            return gviewer.getTierManager().getSelectedTiers();
        }
    }
}
