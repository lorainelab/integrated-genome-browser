/**
 *   Copyright (c) 2001-2008 Affymetrix, Inc.
 *    
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.genoviz.tiers;

import java.awt.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

import com.affymetrix.genoviz.awt.*;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.util.ComponentPagePrinter;
import java.awt.geom.Rectangle2D;

/**
 *  Wraps a AffyTieredMap and another map that has tier labels which 
 *    track changes in tiers (size, placement) of AffyTieredMap.
 */
public class AffyLabelledTierMap extends AffyTieredMap {

  AffyTieredMap labelmap;
  JSplitPane mapsplitter;
  List<TierLabelGlyph> label_glyphs = new ArrayList<TierLabelGlyph>();
  JPanel can_panel;

  public AffyLabelledTierMap() {
    super();
  }

  public AffyLabelledTierMap(boolean hscroll_show, boolean vscroll_show) {
    super(hscroll_show, vscroll_show);
  }

  /**
   *  Overriding initComponenetLayout from NeoMap
   *    (called in NeoMap constructor...).
   */
  @Override
  public void initComponentLayout() {
    labelmap = new AffyTieredMap(false, false);
    labelmap.setRubberBandBehavior(false);
    this.setBackground(Color.blue);
    labelmap.setBackground(Color.lightGray);
    // setMapColor() controls what I normally think of as the background.

    mapsplitter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    mapsplitter.setOneTouchExpandable(true);
    mapsplitter.setDividerSize(8);
    mapsplitter.setDividerLocation(100);
    NeoCanvas ncan = this.getNeoCanvas();
    mapsplitter.setLeftComponent(labelmap);

    can_panel = new JPanel();
    can_panel.setLayout(new BorderLayout());
    can_panel.add("Center", ncan);
    mapsplitter.setRightComponent(can_panel);

    this.setLayout(new BorderLayout());
    add("Center", mapsplitter);

    if (hscroll_show) {
      add(hscroll_loc, scroller[Xint]);
    }
    if (vscroll_show) {
      add(vscroll_loc, scroller[Yint]);
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
    label_glyphs = new ArrayList<TierLabelGlyph>();
  }

  public List<TierLabelGlyph> getTierLabels() {
    return label_glyphs;
  }

  public AffyTieredMap getLabelMap() {
    return labelmap;
  }

  @Override
  public void packTiers(boolean full_repack, boolean stretch_map) {
    super.packTiers(full_repack, stretch_map);
    //Rectangle2D bbox = this.getCoordBounds();
    //    labelmap.setMapOffset((int)bbox.y, (int)(bbox.y + bbox.height));
    // this should actually get dealt with in AffyTieredMap, since packTiers() calls 
    //     this.setFloatBounds(), which in turn calls labelmap.setFloatOffset()
    //    labelmap.setFloatBounds(bbox.y, bbox.y + bbox.height);
    Rectangle2D.Double lbox = labelmap.getCoordBounds();
    for (int i = 0; i < label_glyphs.size(); i++) {
      GlyphI label_glyph = (GlyphI) label_glyphs.get(i);
      TierGlyph tier_glyph = (TierGlyph) label_glyph.getInfo();
      Rectangle2D.Double tbox = tier_glyph.getCoordBox();
      //      label_glyph.setCoords(lbox.x, tbox.y, lbox.width, tbox.height);
      label_glyph.setCoords(lbox.x, tbox.y, lbox.width, tbox.height);
      //      System.out.println(label_glyph.getCoordBox());
      label_glyph.setVisibility(tier_glyph.isVisible());
    }
  }

  /**
   * Adds a tier to the map and generates a label for it.
   * <p>We don't need to override {@link AffyTieredMap#addTier(TierGlyph)}
   * because it calls {@link AffyTieredMap#addTier(TierGlyph, boolean)},
   * which, in turn calls {@link AffyTieredMap#addTier(TierGlyph,int)}
   * which we override here.
   */
  @Override
  public void addTier(TierGlyph mtg, int tier_index) {
    super.addTier(mtg, tier_index);
    createTierLabel(mtg);
  }

  /** Creates a TierLabelGlyph for the given TierGlyph.  
   *  Called by addTier() methods.  Override this to 
   *  add additional settings to the glyph.
   */
  public TierLabelGlyph createTierLabel(TierGlyph mtg) {
    TierLabelGlyph label_glyph = new TierLabelGlyph(mtg);
    // No need to set the TierLabelGlyph colors or label:
    // it reads that information dynamically from the given TierGlyph

    label_glyph.setShowBackground(true);
    label_glyph.setShowOutline(true);

    labelmap.addItem(label_glyph);
    // set info for string glyph to point to tier glyph
    //   (which also sets value returned by label_glyph.getInfo())
    labelmap.setDataModel(label_glyph, mtg);
    label_glyphs.add(label_glyph);
    return label_glyph;
  }

  @Override
  public void removeTier(TierGlyph toRemove) {
    super.removeTier(toRemove);
    GlyphI label_glyph = labelmap.getItem(toRemove);
    if (label_glyph != null) {
      labelmap.removeItem(label_glyph);
      label_glyphs.remove(label_glyph);
    }
  }

  @Override
  public void setFloatBounds(WidgetAxis axis, double start, double end) {
    super.setFloatBounds(axis, start, end);
    if (axis == WidgetAxis.Secondary && labelmap != null) {
      labelmap.setFloatBounds(axis, start, end);
    }
  }

  @Override
  public void setBounds(WidgetAxis axis, int start, int end) {
    super.setBounds(axis, start, end);
    if (axis == WidgetAxis.Secondary && labelmap != null) {
      labelmap.setBounds(axis, start, end);
    }
  }

  @Override
  public void zoom(WidgetAxis dim, double zoom_scale) {
    super.zoom(dim, zoom_scale);
    if (dim == WidgetAxis.Secondary && labelmap != null) {
      labelmap.zoom(dim, zoom_scale);
    }
  }

  @Override
  public void scroll(WidgetAxis axisid, double value) {
    super.scroll(axisid, value);
    if (axisid == WidgetAxis.Secondary && labelmap != null) {
      labelmap.scroll(axisid, value);
    }
  }

  @Override
  public void setZoomBehavior(WidgetAxis axis, ZoomConstraint constraint, double coord) {
    super.setZoomBehavior(axis, constraint, coord);
    labelmap.setZoomBehavior(axis, constraint, coord);
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

  /** Prints this component, including the label map. */
  @Override
  public void print() throws java.awt.print.PrinterException {
    print(true);
  }

  /** Prints this component.
   *  @param print_labels whether or not to print the label map along with the map
   */
  public void print(boolean print_labels) throws java.awt.print.PrinterException {
    ComponentPagePrinter cpp = null;
    if (print_labels) {
      cpp = new ComponentPagePrinter(mapsplitter);
    } else {
      cpp = new ComponentPagePrinter(can_panel);
    }
    cpp.print();
    cpp = null; // for garbage collection
  }

  /** Returns the JSplitPane that contains the label map and the tier map.
   *  This is mostly useful for printing.
   */
  public JSplitPane getSplitPane() {
    return mapsplitter;
  }
}
