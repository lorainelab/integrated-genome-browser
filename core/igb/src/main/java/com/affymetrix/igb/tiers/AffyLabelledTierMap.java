/**
 * Copyright (c) 2001-2006 Affymetrix, Inc.
 * 
* Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 * 
* The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.tiers;

import com.affymetrix.genoviz.awt.NeoCanvas;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.util.ComponentPagePrinter;
import com.affymetrix.genoviz.util.NeoConstants;
import com.lorainelab.igb.genoviz.extensions.api.TierGlyph;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.swing.DefaultListSelectionModel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.EventListenerList;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;

/**
 * Wraps a AffyTieredMap and another map that has tier labels which track
 * changes in tiers (size, placement) of AffyTieredMap.
 */
public class AffyLabelledTierMap extends AffyTieredMap {

    private static final long serialVersionUID = 1L;
    static final double FUDGE_FACTOR = 0.2;

    AffyTieredMap labelmap;
    JSplitPane mapsplitter;
    final List<TierLabelGlyph> label_glyphs = new ArrayList<TierLabelGlyph>();
    List<TierLabelGlyph> ordered_glyphs = null;
    JPanel can_panel;
    NeoCanvas ncan;
    AffyLabelledTierMap.TierSelectionModel selectionModel = new AffyLabelledTierMap.TierSelectionModel();
    protected EventListenerList listenerList = new EventListenerList();

    public AffyLabelledTierMap(boolean hscroll_show, boolean vscroll_show) {
        super(hscroll_show, vscroll_show, NeoConstants.HORIZONTAL);
    }

    /**
     * Overriding initComponenetLayout from NeoMap (called in NeoMap
     * constructor...).
     */
    @Override
    public void initComponentLayout() {
        labelmap = new AffyTieredMap(false, false, scroller[Y]) {
            private static final long serialVersionUID = 1L;

            @Override
            public void select(GlyphI glyph) {
                super.select(glyph);
                selectionModel.actionPerformed(new ActionEvent(glyph, ActionEvent.ACTION_FIRST, "Select Tier"));
            }

            @Override
            public void deselect(GlyphI glyph) {
                super.deselect(glyph);
                selectionModel.actionPerformed(new ActionEvent(glyph, ActionEvent.ACTION_FIRST, "Deselect Tier"));
            }
        };
        labelmap.setRubberBandBehavior(false);
        this.setBackground(Color.blue);
        labelmap.setBackground(Color.lightGray);

        mapsplitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mapsplitter.setDividerSize(8);
        mapsplitter.setDividerLocation(100);
        ncan = this.getNeoCanvas();
        mapsplitter.setLeftComponent(labelmap);

        can_panel = new JPanel();
        can_panel.setLayout(new BorderLayout());
        can_panel.add("Center", ncan);
        mapsplitter.setRightComponent(can_panel);

        this.setLayout(new BorderLayout());
        add("Center", mapsplitter);

        if (hscroll_show) {
            add(hscroll_loc, scroller[X]);
        }
        if (vscroll_show) {
            add(vscroll_loc, scroller[Y]);
        }
    }

    @Override
    public void setMapColor(Color c) {
        super.setMapColor(c);
        labelmap.setMapColor(c);
    }

    @Override
    public void setBackground(Color c) {
        super.setBackground(c);
        labelmap.setBackground(c);
    }

    @Override
    public void clearWidget() {
        super.clearWidget();
        labelmap.clearWidget();
        label_glyphs.clear();
        ordered_glyphs = null;
    }

    public List<TierLabelGlyph> getTierLabels() {
        return label_glyphs;
    }

    private List<TierLabelGlyph> getOrderedGlyphs() {
        List<TierLabelGlyph> orderedGlyphs = new ArrayList<TierLabelGlyph>();
        for (TierLabelGlyph tierLabelGlyph : label_glyphs) {
            if (tierLabelGlyph.isVisible()) {
                orderedGlyphs.add(tierLabelGlyph);
            }
        }
        Collections.sort(orderedGlyphs, new Comparator<TierLabelGlyph>() {
            @Override
            public int compare(TierLabelGlyph o1, TierLabelGlyph o2) {
                if (o1.getCoordBox().getY() + o1.getCoordBox().getHeight() < o2
                        .getCoordBox().getY() + FUDGE_FACTOR) {
                    return -1;
                }
                if (o1.getCoordBox().getY() + FUDGE_FACTOR > o2.getCoordBox()
                        .getY() + o2.getCoordBox().getHeight()) {
                    return 1;
                }
                return 0;
            }
        });
        return orderedGlyphs;
    }

    /**
     * @return a list of tier labels in order from top to bottom.
     */
    public List<TierLabelGlyph> getOrderedTierLabels() {
        if (ordered_glyphs == null) {
            ordered_glyphs = getOrderedGlyphs();
        }
        return ordered_glyphs;
    }

    public AffyTieredMap getLabelMap() {
        return labelmap;
    }

    @Override
    public void packTiers(boolean full_repack, boolean stretch_map, boolean fire_tier_position_changed) {
        super.packTiers(full_repack, stretch_map, fire_tier_position_changed);
        Rectangle2D.Double lbox = labelmap.getCoordBounds();
        for (TierLabelGlyph label_glyph : label_glyphs) {
            TierGlyph tier_glyph = label_glyph.getReferenceTier();
            Rectangle2D.Double tbox = tier_glyph.getCoordBox();
            label_glyph.setCoords(lbox.x, tbox.y, lbox.width, tbox.height);
            label_glyph.setVisibility(tier_glyph.isVisible());
        }
        labelmap.fixed_coord_height = fixed_coord_height;
        labelmap.fixed_pixel_height = fixed_pixel_height;
        ordered_glyphs = null;
        if (fire_tier_position_changed) {
            fireTierOrderChanged();
        }
    }

    public void addListSelectionListener(ListSelectionListener theListener) {
        this.selectionModel.addListSelectionListener(theListener);
    }

    public void removeListSelectionListener(ListSelectionListener theListener) {
        this.selectionModel.removeListSelectionListener(theListener);
    }

    /**
     * Adds a tier to the map and generates a label for it.
     */
    @Override
    public void addTier(TierGlyph mtg, int tier_index) {
        super.addTier(mtg, tier_index);
        createTierLabel(mtg, tier_index);
    }

    /**
     * Creates a TierLabelGlyph for the given TierGlyph. Called by addTier()
     * methods. Override this to add additional settings to the glyph.
     */
    private void createTierLabel(TierGlyph mtg, int tier_index) {
        TierLabelGlyph label_glyph = new TierLabelGlyph(mtg, tier_index);
        // No need to set the TierLabelGlyph colors or label:
        // it reads that information dynamically from the given TierGlyph

        labelmap.addItem(label_glyph);
        // set info for string glyph to point to tier glyph
        //   (which also sets value returned by label_glyph.getInfo())
        labelmap.setDataModel(label_glyph, mtg);
        label_glyphs.add(label_glyph);
        ordered_glyphs = null;
    }

    @Override
    public void removeTier(TierGlyph toRemove) {
        super.removeTier(toRemove);
        TierLabelGlyph label_glyph = labelmap.<TierLabelGlyph>getItem(toRemove);
        if (label_glyph != null) {
            labelmap.removeItem(label_glyph);
            label_glyphs.remove(label_glyph);
            ordered_glyphs = null;
        }
    }

    @Override
    public void setFloatBounds(int axis, double start, double end) {
        super.setFloatBounds(axis, start, end);
        if (axis == Y && labelmap != null) {
            labelmap.setFloatBounds(axis, start, end);
        }
    }

    @Override
    public void setBounds(int axis, int start, int end) {
        super.setBounds(axis, start, end);
        if (axis == Y && labelmap != null) {
            labelmap.setBounds(axis, start, end);
        }
    }

    @Override
    public void zoom(int axisid, double zoom_scale) {
        super.zoom(axisid, zoom_scale);
        if (axisid == Y && labelmap != null) {
            labelmap.zoom(axisid, zoom_scale);
        }
    }

    @Override
    public void setZoomBehavior(int axisid, int constraint, double coord) {
        super.setZoomBehavior(axisid, constraint, coord);
        labelmap.setZoomBehavior(axisid, constraint, coord);
    }

    @Override
    public void updateWidget() {
        super.updateWidget();
        labelmap.updateWidget();
    }

    @Override
    public void updateWidget(boolean full_update) {
        super.updateWidget(full_update);
        labelmap.updateWidget(full_update);
    }

    @Override
    public void stretchToFit(boolean fitx, boolean fity) {
        super.stretchToFit(fitx, fity);
        labelmap.stretchToFit(fitx, fity);
    }

    @Override
    public void repackTheTiers(boolean full_repack, boolean stretch_vertically) {
        super.repackTheTiers(full_repack, stretch_vertically);
        labelmap.repackTheTiers(full_repack, stretch_vertically);
    }

    @Override
    protected void stretchToFit(boolean fitx, boolean fity, boolean packTiers) {
        super.stretchToFit(fitx, fity, packTiers);
        labelmap.stretchToFit(fitx, fity, packTiers);
    }

    /**
     * Prints this component, including the label map.
     */
    @Override
    public void print() throws java.awt.print.PrinterException {
        print(true);
    }

    /**
     * Prints this component.
     *
     * @param print_labels whether or not to print the label map along with the
     * map
     */
    private void print(boolean print_labels) throws java.awt.print.PrinterException {
        ComponentPagePrinter cpp = null;
        if (print_labels) {
            cpp = new ComponentPagePrinter(mapsplitter);
        } else {
            cpp = new ComponentPagePrinter(can_panel);
        }
        cpp.print();
        cpp = null; // for garbage collection
    }

    /**
     * Returns the JSplitPane that contains the label map and the tier map. This
     * is mostly useful for printing.
     */
    public JSplitPane getSplitPane() {
        return mapsplitter;
    }

    @Override
    public void componentResized(java.awt.event.ComponentEvent evt) {
        if (evt.getSource() == canvas) {
            if (trans.getScaleX() == 0 || trans.getScaleY() == 0) {
                stretchToFit(true, true);
            } else {
                int[] offset = getVisibleOffset();
                int[] range = getVisibleRange();
                stretchToFit(true, true);
                restoreView(Y, offset, getView().getPixelBox().height);
                restoreView(X, range, getView().getPixelBox().width);
            }
            updateWidget();
        }
    }

    private void restoreView(int axis, int[] range, double pixels) {
        int coord_height = range[1] - range[0];
        double ppc = pixels / coord_height;
        ppc = Math.min(ppc, getMaxZoom(axis));
        ppc = Math.max(ppc, getMinZoom(axis));
        zoom(axis, ppc);
        scroll(axis, range[0]);
        setZoomBehavior(axis, CONSTRAIN_COORD, (range[0] + range[1]) / 2);
    }

    public void addTierOrderListener(TableModelListener l) {
        listenerList.add(TableModelListener.class, l);
    }

    public void removeTierOrderListener(TableModelListener l) {
        listenerList.remove(TableModelListener.class, l);
    }

    public void fireTierOrderChanged() {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == TableModelListener.class) {
                ((TableModelListener) listeners[i + 1]).tableChanged(null);
            }
        }
    }

    /**
     * An incomplete selection list model to support resizing and coloring. This
     * is just a hack at this point. A more complete implementation might help.
     * It was added to support (vertical) resizing of tiers taking selection
     * into account. It also supports color scheme choices.
     */
    protected class TierSelectionModel extends DefaultListSelectionModel implements ActionListener {

        private static final long serialVersionUID = 1L;

        @Override
        public void actionPerformed(ActionEvent e) {
            List<TierLabelGlyph> l = AffyLabelledTierMap.this.getOrderedTierLabels();
            this.setSelectionInterval(0, l.size() - 1);
            // Perhaps, here we should find out what the selection really is
            // instead of leaving it up to the listeners.
            // Soon...
            this.fireValueChanged(0, 1, false);
        }

        @Override
        public boolean isSelectedIndex(int theIndex) {
            List<TierLabelGlyph> l = AffyLabelledTierMap.this.getOrderedTierLabels();
            TierLabelGlyph tlg = l.get(theIndex);
            return tlg.isSelected();
        }

        @Override
        public void setSelectionMode(int theMode) {
            if (ListSelectionModel.MULTIPLE_INTERVAL_SELECTION != theMode) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
            super.setSelectionMode(theMode);
        }
    }
}
