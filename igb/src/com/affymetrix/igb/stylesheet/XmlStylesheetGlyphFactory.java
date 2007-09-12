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
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.TypeContainerAnnot;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.IAnnotStyleExtended;
import com.affymetrix.igb.glyph.MapViewGlyphFactoryI;
import com.affymetrix.igb.glyph.GenericAnnotGlyphFactory;
import com.affymetrix.igb.tiers.TierGlyph;
import com.affymetrix.igb.view.SeqMapView;
import java.util.*;

/**
 *  A very glyph factory that can draw glyphs based on a stylesheet.
 *  Most of the drawing work is handled by other classes, such as
 *  GlyphElement.
 */
public class XmlStylesheetGlyphFactory implements MapViewGlyphFactoryI {
  static boolean DEBUG = false;

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
    if (sym instanceof TypeContainerAnnot)  { return true; }  // faster than checking for CONTAINER_PROP
    else if (sym instanceof SymWithProps) {
      SymWithProps swp = (SymWithProps) sym;
      if (Boolean.TRUE.equals(swp.getProperty(SimpleSymWithProps.CONTAINER_PROP))) {
        return true;
      }
    }
    return false;
  }

  Map annot_factories = new LinkedHashMap();
  public void createGlyph(SeqSymmetry sym, SeqMapView gviewer, boolean next_to_axis) {
      // fixing bug encountered when sym doesn't have span on sequence it is annotating --
      //   currently should only see these as "dummy" placeholder syms that are
      //   children of Das2FeatureRequestSyms, in which case they have _no_ spans.
      //   So for now skipping any sym with no spans...
      if (sym.getSpanCount() == 0)  { return; }
    // I'm assuming that for container glyphs, the container method is the
    // same as the contained items method
    String meth = SeqMapView.determineMethod(sym);

    // shortcut for delegating to other factories
    //    (or at least GenericAnnotGlyphFactory via AssociationElement...)
    MapViewGlyphFactoryI subfactory = (MapViewGlyphFactoryI)annot_factories.get(meth);

    if (subfactory != null) {
      subfactory.createGlyph(sym, gviewer);
    }
    else {
      DrawableElement drawable = stylesheet.getDrawableForSym(sym);
      // if possible, extract a subfactory from drawable and add to annot_factories list...
      // for now only do this if factory is a GenericAnnotGlyphFactory -- not sure how shortcut would
      //    affect other factories...

      if (drawable instanceof AssociationElement) {
	AssociationElement assel = (AssociationElement)drawable;
	MapViewGlyphFactoryI assfac = assel.getGlyphFactory();
	if (assfac != null) {
	  if (assfac instanceof GenericAnnotGlyphFactory) {
            if (DEBUG)  { System.out.println("in XmlStylesheetGlyphFactory.createGlyph(), adding factory to this.annot_factories (and initing): " + meth); }
	    annot_factories.put(meth, assfac);
	    // need to make sure factory is initialized!
	    assfac.init( assel.getPropertyMap());
            // deal with tiers, or let factory handle it?
	    assfac.createGlyph(sym, gviewer);
	    return;
	  }
	}
      }

      if (isContainer(sym)) {
	for (int i=0; i<sym.getChildCount(); i++) {
	  createGlyph(sym.getChild(i), gviewer, next_to_axis);
	}
	return;
      }
      IAnnotStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(meth);
      TierGlyph[] tiers = gviewer.getTiers(meth, next_to_axis, style, false);
      int tier_index = (sym.getSpan(0).isForward()) ? 0 : 1;
      TierGlyph the_tier = tiers[tier_index];

      context.clear();

      // properties set in this top-level context will be used as defaults,
      // the stylesheet may over-ride them.

      // Allow StyleElement access to the AnnotStyle if it needs it.
      context.put(IAnnotStyleExtended.class.getName(), style);
      context.put(TierGlyph.class.getName(), the_tier);

      drawable.symToGlyph(gviewer, sym, the_tier, stylesheet, context);
    }
  }

}
