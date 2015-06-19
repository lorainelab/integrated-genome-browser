/**
 * Copyright (c) 2001-2004 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License").
 * A copy of the license must be included with any distribution of
 * this source code.
 * Distributions from Affymetrix, Inc., place this in the
 * IGB_LICENSE.html file.
 *
 * The license is also available at
 * http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.view;

import com.affymetrix.common.PreferenceUtils;
import com.affymetrix.genoviz.event.NeoViewBoxListener;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.widget.Shadow;
import java.awt.Font;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

public final class UnibrowHairline {

    public static final String PREF_KEEP_HAIRLINE_IN_VIEW = "Keep zoom stripe in view";
    public static final boolean default_keep_hairline_in_view = true;

    /**
     * Name of a boolean preference for whether the hairline label should be on.
     */
    public static final String PREF_HAIRLINE_LABELED = "Zoom Stripe Label On";
    public static final boolean default_show_hairline_label = true;

    static final Font DEFAULT_FONT = new Font("SansSerif", Font.PLAIN, 12);

    // It is common practice to use a VisibleRange with the Shadow as
    // a NeoRangeListener on it, but that seems overly complex for this case.
    //private VisibleRange visible_range;
    private Shadow hairline;
    private NeoMap map;
  //private MouseListener mouse_listener;

    // The NeoViewBoxListener makes sure that the hairline will
    // stay inside the borders of the map, no matter where the user
    // scrolls or zooms the map.
    // (We could use a NeoRangeListener on the NeoMap instead of
    // a pre-draw NeoViewBoxListener on the View.)
    private final NeoViewBoxListener pre_draw_listener;

    // the current location of the hairline
    private double focus = 1;

    PreferenceChangeListener pcl;

    public UnibrowHairline(NeoMap the_map) {
        if (the_map == null) {
            throw new IllegalArgumentException();
        }

        map = the_map;
        pre_draw_listener = e -> {
            if (hairline.getShowHairLine()) {
                double start = e.getCoordBox().x;
                double end = e.getCoordBox().width + start;
                if (focus < start) {
                    setSpot(start);
                } else if (focus > end) {
                    setSpot(end);
                }
            }
        };

        hairline = new Shadow(map);
        hairline.setSelectable(false);
        hairline.setLabeled(false);
        setKeepHairlineInView(PreferenceUtils.getBooleanParam(PREF_KEEP_HAIRLINE_IN_VIEW, default_keep_hairline_in_view));
        setShowHairlineLabel(PreferenceUtils.getBooleanParam(PREF_HAIRLINE_LABELED, default_show_hairline_label));

        map.getView().addPreDrawViewListener(pre_draw_listener);

        pcl = (PreferenceChangeEvent pce) -> {
            if (!pce.getNode().equals(PreferenceUtils.getTopNode())) {
                return;
            }
            if (pce.getKey().equals(PREF_KEEP_HAIRLINE_IN_VIEW)) {
                setKeepHairlineInView(PreferenceUtils.getBooleanParam(PREF_KEEP_HAIRLINE_IN_VIEW, default_keep_hairline_in_view));
            }
            if (pce.getKey().equals(PREF_HAIRLINE_LABELED)) {
                setShowHairlineLabel(PreferenceUtils.getBooleanParam(PREF_HAIRLINE_LABELED, default_show_hairline_label));
            }
        };

        PreferenceUtils.getTopNode().addPreferenceChangeListener(pcl);
    }

    /**
     * Sets the flag determining whether the hairline is constrained
     * to remain inside the visible map boundaries.
     * If b is null, the current value of the flag is not changed.
     */
    public void setKeepHairlineInView(boolean b) {
        hairline.setShowHairline(b);
        map.updateWidget();
    }

    /**
     * Sets the flag determining whether the hairline should show label.
     *
     * @param b
     */
    public void setShowHairlineLabel(boolean b) {
        hairline.setLabeled(b);
        map.updateWidget();
    }

    /**
     * Sets the location of the hairline. This is the only supported
     * way to move the hairline. Does *NOT* call map.updateWidget() and
     * but you will probably want to do that after calling this method.
     */
    public void setSpot(double spot) {
        focus = spot;
        //visible_range.setSpot(focus);
        // instead of using the visible_range, directly call hairline.setRange()
        hairline.setRange((int) focus, (int) focus + 1);
        map.setZoomBehavior(NeoMap.X, NeoMap.CONSTRAIN_COORD, focus);
        //map.updateWidget();
    }

    /**
     * Returns the current location of the hairline.
     */
    public double getSpot() {
        return focus;
    }

    /**
     * Call this method to get rid of circular references, to make
     * garbage collection easier.
     */
    public void destroy() {
        if (map != null && pre_draw_listener != null) {
            map.getView().removePreDrawViewListener(pre_draw_listener);
        }
        if (hairline != null) {
            hairline.destroy();
        }
        hairline = null;
        map = null;
        if (pcl != null) {
            PreferenceUtils.getTopNode().removePreferenceChangeListener(pcl);
        }
        pcl = null;
    }
}
