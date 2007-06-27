/**
 *   Copyright (c) 2007 Affymetrix, Inc.
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

package com.affymetrix.igb.stylesheet;

import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.genometry.SimpleSymWithProps;
import com.affymetrix.igb.genometry.SymWithProps;
import com.affymetrix.igb.glyph.MapViewGlyphFactoryI;
import com.affymetrix.igb.tiers.AnnotStyle;
import com.affymetrix.igb.tiers.IAnnotStyleExtended;
import com.affymetrix.igb.tiers.TierGlyph;
import com.affymetrix.igb.view.SeqMapView;
import java.util.*;

/**
 *  A very glyph factory that can draw glyphs based on a stylesheet.
 *  Most of the drawing work is handled by other classes, such as
 *  GlyphElement.
 */
public class XmlStylesheetGlyphFactory implements MapViewGlyphFactoryI {

  static final Class STYLE_PROPERTY_CLASS = IAnnotStyleExtended.class;
  static final Class TIER_PROPERTY_CLASS = TierGlyph.class;

  Stylesheet stylesheet = null;
  PropertyMap context = new PropertyMap();

  public XmlStylesheetGlyphFactory() {
  }

  public void setStylesheet(Stylesheet ss) {
    this.stylesheet = ss;
  }

  // does nothing
  public void init(Map options) {
  }

  public void createGlyph(SeqSymmetry sym, SeqMapView smv) {
    createGlyph(sym, smv, false);
  }

  boolean isContainer(SeqSymmetry sym) {
    if (sym instanceof SymWithProps) {
      SymWithProps swp = (SymWithProps) sym;
      if (Boolean.TRUE.equals(swp.getProperty(SimpleSymWithProps.CONTAINER_PROP))) {
        return true;
      }
    }
    return false;
  }

  public void createGlyph(SeqSymmetry sym, SeqMapView gviewer, boolean next_to_axis) {
      // fixing bug encountered when sym doesn't have span on sequence it is annotating --
      //   currently should only see these as "dummy" placeholder syms that are
      //   children of Das2FeatureRequestSyms, in which case they have _no_ spans.
      //   So for now skipping any sym with no spans...
      if (sym.getSpanCount() == 0)  { return; }
    // I'm assuming that for container glyphs, the container method is the
    // same as the contained items method
    String meth = SeqMapView.determineMethod(sym);
    //    if (meth == null)  { SeqUtils.printSymmetry(sym, "   ", true); }
    IAnnotStyleExtended style = Application.getSingleton().getStyleForMethod(meth, false);

   if (isContainer(sym)) {
      for (int i=0; i<sym.getChildCount(); i++) {
        createGlyph(sym.getChild(i), gviewer, next_to_axis);
      }
      return;
    }

    DrawableElement drawable = stylesheet.getDrawableForSym(sym);

    TierGlyph[] tiers = gviewer.getTiers(meth, next_to_axis, style, false);
    int tier_index = (sym.getSpan(0).isForward()) ? 0 : 1;
    TierGlyph the_tier = tiers[tier_index];

    context.clear();

    // properties set in this top-level context will be used as defaults,
    // the stylesheet may over-ride them.

    // Allow StyleElement access to the AnnotStyle if it needs it.
    context.put(STYLE_PROPERTY_CLASS.getName(), style);
    context.put(TIER_PROPERTY_CLASS.getName(), the_tier);

    drawable.symToGlyph(gviewer, sym, the_tier, stylesheet, context);
  }
}
