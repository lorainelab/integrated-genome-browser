/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
*    
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*   Distributions from Affymetrix, Inc., place this in the
*   IGB_LICENSE.html file.  
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/

package com.affymetrix.igb.tiers;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import com.affymetrix.genoviz.awt.*;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.*;
import com.affymetrix.genoviz.widget.*;
import com.affymetrix.genoviz.awt.AdjustableJSlider;
import com.affymetrix.genoviz.util.ComponentPagePrinter;

/**
 *  Wraps a AffyTieredMap and another map that has tier labels which 
 *    track changes in tiers (size, placement) of AffyTieredMap.
 */
public class AffyLabelledTierMap extends AffyTieredMap  {
  
  AffyTieredMap labelmap;
  JSplitPane mapsplitter;
  java.util.List label_glyphs = new ArrayList();
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
  public void initComponentLayout() {
    labelmap = new AffyTieredMap(false, false);    
    labelmap.setRubberBandBehavior(false);
    this.setBackground(Color.blue);
    labelmap.setBackground(Color.lightGray);

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

    if (hscroll_show && scroller[X] instanceof NeoScrollbar)  {
      add(hscroll_loc, (NeoScrollbar)scroller[X]);
    }
    if (vscroll_show && scroller[Y] instanceof NeoScrollbar)  {
      add(vscroll_loc, (NeoScrollbar)scroller[Y]);
    }
  }

  public void clearWidget() {
    super.clearWidget();
    labelmap.clearWidget();
    label_glyphs = new ArrayList();
  }

  public java.util.List getTierLabels() {
    return label_glyphs;
  }

  public AffyTieredMap getLabelMap() {
    return labelmap;
  }

  public void packTiers(boolean full_repack, boolean stretch_map, boolean extra_for_now) { 
    super.packTiers(full_repack, stretch_map, extra_for_now);
    //Rectangle2D bbox = this.getCoordBounds();
    //    labelmap.setMapOffset((int)bbox.y, (int)(bbox.y + bbox.height));
    // this should actually get dealt with in AffyTieredMap, since packTiers() calls 
    //     this.setFloatBounds(), which in turn calls labelmap.setFloatOffset()
    //    labelmap.setFloatBounds(bbox.y, bbox.y + bbox.height);
    Rectangle2D lbox = labelmap.getCoordBounds();
    for (int i=0; i<label_glyphs.size(); i++) {
      GlyphI label_glyph = (GlyphI)label_glyphs.get(i);
      TierGlyph tier_glyph = (TierGlyph)label_glyph.getInfo();
      Rectangle2D tbox = tier_glyph.getCoordBox();
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
  public void addTier(TierGlyph mtg, int tier_index) {
    super.addTier(mtg, tier_index);
    TierLabelGlyph label_glyph = new TierLabelGlyph();
    if (mtg.getLabel() != null) { label_glyph.setString(mtg.getLabel()); }
    else { label_glyph.setString("......."); }
    if (mtg.getFillColor() != null) {
      label_glyph.setBackgroundColor(mtg.getFillColor());
    }
    label_glyph.setForegroundColor(mtg.getForegroundColor());
    label_glyph.setShowBackground(true);
    label_glyph.setShowOutline(true);
    labelmap.addItem(label_glyph);
    // set info for string glyph to point to tier glyph
    //   (which also sets value returned by label_glyph.getInfo())
    labelmap.setDataModel(label_glyph, mtg);  
    label_glyphs.add(label_glyph);
  }

  public void removeTier(TierGlyph toRemove) {
    super.removeTier(toRemove);
    GlyphI label_glyph = (GlyphI)labelmap.getItem(toRemove);
    if (label_glyph != null) {
      labelmap.removeItem(label_glyph);
      label_glyphs.remove(label_glyph);
    }
  }

  public void setFloatBounds(int axis, double start, double end) {
    super.setFloatBounds(axis, start, end);
    if (axis == Y && labelmap != null) { 
      labelmap.setFloatBounds(axis, start, end);
    }
  }

  public void setBounds(int axis, int start, int end) {
    super.setBounds(axis, start, end);
    if (axis == Y && labelmap != null) { 
      labelmap.setBounds(axis, start, end);
    }
  }

  public void zoom(int axisid, double zoom_scale) { 
    super.zoom(axisid, zoom_scale);
    if (axisid == Y && labelmap != null) {
      labelmap.zoom(axisid, zoom_scale);
    }
  }

  public void scroll(int axisid, double value) {
    super.scroll(axisid, value);
    if (axisid == Y && labelmap != null) {
      labelmap.scroll(axisid, value);
    }
  }

  public void setZoomBehavior(int axisid, int constraint, double coord) {
    super.setZoomBehavior(axisid, constraint, coord);
    labelmap.setZoomBehavior(axisid, constraint, coord);
  }

  public void updateWidget() {
    super.updateWidget();
    labelmap.updateWidget();
  }

  public void stretchToFit(boolean fitx, boolean fity) {
    super.stretchToFit(fitx, fity);
    labelmap.stretchToFit(fitx, fity);
  }

  /** Prints this component, including the label map. */
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
  
  /**
   *  main for testing AffyLabelledTierMap
   */
  public static void main(String[] args) {
    AffyLabelledTierMap map = new AffyLabelledTierMap();

    AdjustableJSlider xzoomer = new AdjustableJSlider(Adjustable.HORIZONTAL);
    AdjustableJSlider yzoomer = new AdjustableJSlider(Adjustable.VERTICAL);
    map.setZoomer(NeoMap.X, xzoomer);
    map.setZoomer(NeoMap.Y, yzoomer);

    map.setMapRange(0, 10000);
    map.setMapOffset(0, 1000);
    map.addAxis(500);

    TierGlyph mtg = new TierGlyph();
    mtg.setCoords(0, 0, 1000, 200);
    mtg.setFillColor(Color.red);
    map.addTier(mtg);
    mtg = new TierGlyph();
    mtg.setCoords(0, 0, 1000, 400);
    mtg.setFillColor(Color.orange);
    map.addTier(mtg);
    map.repack();
    
    JFrame frm = new JFrame("AffyLabelledTierMap.main() test");
    Container cpane = frm.getContentPane();
    cpane.setLayout(new BorderLayout());
    cpane.add("Center", map);
    cpane.add("North", xzoomer);
    cpane.add("West", yzoomer);
    frm.setSize(600, 400);
    frm.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent evt) {
	System.exit(0);
      }
    } );
    frm.show();
  }

}
