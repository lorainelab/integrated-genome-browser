/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.view;

import com.affymetrix.genometry.util.PreferenceUtils;
import com.affymetrix.genoviz.glyph.AxisGlyph;
import com.affymetrix.igb.tiers.AccordionTierResizer;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.tiers.CoordinateStyle;
import com.affymetrix.igb.tiers.TierResizer;
import static com.affymetrix.igb.view.SeqMapView.default_show_prop_tooltip;
import static com.affymetrix.igb.view.SeqMapView.setAxisFormatFromPrefs;
import static com.affymetrix.igb.view.SeqMapViewConstants.PREF_EDGE_MATCH_COLOR;
import static com.affymetrix.igb.view.SeqMapViewConstants.PREF_EDGE_MATCH_FUZZY_COLOR;
import static com.affymetrix.igb.view.SeqMapViewConstants.PREF_SHOW_TOOLTIP;
import static com.affymetrix.igb.view.SeqMapViewConstants.PREF_TRACK_RESIZING_BEHAVIOR;
import static com.affymetrix.igb.view.SeqMapViewConstants.PREF_X_ZOOMER_ABOVE;
import static com.affymetrix.igb.view.SeqMapViewConstants.PREF_Y_ZOOMER_LEFT;
import java.awt.BorderLayout;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import javax.swing.JComponent;
import javax.swing.event.MouseInputAdapter;

/**
 *
 * @author tarun
 */
public class PreferenceChangeListenerImpl implements PreferenceChangeListener {

    MouseInputAdapter resizer;
    AffyTieredMap seqmap;
    SeqMapView seqMapView;
    JComponent xzoombox;
    JComponent yzoombox;
    boolean showEdgeMatches;

    public PreferenceChangeListenerImpl(SeqMapView seqMapView) {
        this.seqMapView = seqMapView;
        this.seqmap = seqMapView.getSeqMap();
        this.xzoombox = seqMapView.getXzoombox();
        this.yzoombox = seqMapView.getYzoombox();
        showEdgeMatches = seqMapView.isShowEdgeMatches();
    }

    private void cleanMouseAdapter(AffyTieredMap labelMap) {
        labelMap.removeMouseListener(TierResizer.class);
        labelMap.removeMouseListener(AccordionTierResizer.class);
        labelMap.removeMouseMotionListener(TierResizer.class);
        labelMap.removeMouseMotionListener(AccordionTierResizer.class);

        labelMap.addMouseListener(resizer);
        labelMap.addMouseMotionListener(resizer);
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent pce) {
        if (!pce.getNode().equals(PreferenceUtils.getTopNode())) {
            return;
        }

        if (pce.getKey().equals(PREF_TRACK_RESIZING_BEHAVIOR)) {
            String behavior = PreferenceUtils.getStringParam(PREF_TRACK_RESIZING_BEHAVIOR, TierResizer.class.getSimpleName());

            if (behavior.equals(TierResizer.class.getSimpleName())) {
                resizer = new TierResizer((AffyLabelledTierMap) seqmap);
            } else {
                resizer = new AccordionTierResizer((AffyLabelledTierMap) seqmap);
            }

            // Remove previous instances of mouse adapters.
            cleanMouseAdapter(((AffyLabelledTierMap) seqmap).getLabelMap());

            return;
        }

        switch (pce.getKey()) {
            case CoordinateStyle.PREF_COORDINATE_LABEL_FORMAT:
                AxisGlyph ag = seqmap.getAxis();
                if (ag != null) {
                    setAxisFormatFromPrefs(ag);
                }
                seqmap.updateWidget();
                break;
            case PreferenceUtils.SHOW_EDGEMATCH_OPTION:
                seqMapView.setEdgeMatching(PreferenceUtils.getTopNode().getBoolean(PreferenceUtils.SHOW_EDGEMATCH_OPTION, PreferenceUtils.default_show_edge_match));
                seqMapView.getSeqMap().updateWidget();
                break;
            case PREF_EDGE_MATCH_COLOR:
            case PREF_EDGE_MATCH_FUZZY_COLOR:
                if (showEdgeMatches) {
                    seqMapView.doEdgeMatching(seqmap.getSelected(), true);
                }
                break;
            case PREF_X_ZOOMER_ABOVE: {
                boolean b = PreferenceUtils.getBooleanParam(PREF_X_ZOOMER_ABOVE, seqMapView.defaultXZoomerAbove);
                seqMapView.remove(xzoombox);
                if (b) {
                    seqMapView.add(BorderLayout.NORTH, xzoombox);
                } else {
                    seqMapView.add(BorderLayout.SOUTH, xzoombox);
                }
                seqMapView.invalidate();
                break;
            }
            case PREF_Y_ZOOMER_LEFT: {
                boolean b = PreferenceUtils.getBooleanParam(PREF_Y_ZOOMER_LEFT, seqMapView.defaultYZoomerLeft);
                seqMapView.remove(yzoombox);
                if (b) {
                    seqMapView.add(BorderLayout.WEST, yzoombox);
                } else {
                    seqMapView.add(BorderLayout.EAST, yzoombox);
                }
                seqMapView.invalidate();
                break;
            }
            case PREF_SHOW_TOOLTIP:
                seqMapView.setShowPropertiesTooltip(PreferenceUtils.getTopNode().getBoolean(PREF_SHOW_TOOLTIP, default_show_prop_tooltip));
                break;
        }
    }
}
