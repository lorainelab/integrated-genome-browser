/**
*   Copyright (c) 2001-2005 Affymetrix, Inc.
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

package com.affymetrix.igb.util;

import java.awt.*;
import java.util.*;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genometry.*;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.glyph.*;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.tiers.*;
import java.util.prefs.Preferences;

public class GraphGlyphUtils {
  /*
   *  For attaching graphs, three possibilities:
   *   1) fixed coord size for tier
   *   2) fixed pixel size for tier
   *   3) adjust initial coord size of tier in attempt to keep height similar
   *         to floating height
   *
   *   if (use_fixed_coord_height), then #1
   *   else if (use_fixed_pixel_height), then #2,
   *   else #3
   */

  public static final boolean DEBUG = false;

  /** Name of a preference for deciding what height to give a graph when converting
   *  it from a floating graph to an attached graph.
   */
  public static final String PREF_ATTACH_HEIGHT_MODE = "height for attached graphs";

  /** One of the possible values for the preference {@link #PREF_ATTACH_HEIGHT_MODE}. */
  public static final String USE_DEFAULT_HEIGHT = "Default Coord Height";

  /** One of the possible values for the preference {@link #PREF_ATTACH_HEIGHT_MODE}. */
  public static final String USE_CURRENT_HEIGHT = "Current Floating Height";

  public static final String PREF_USE_FLOATING_GRAPHS = "use floating graphs";
  public static final String PREF_ATTACHED_COORD_HEIGHT = "default attached graph coord height";
  public static final String PREF_FLOATING_PIXEL_HEIGHT = "default floating graph pixel height";

  /** Pref for whether newly-constructed graph glyphs should only show a
   *  limited range of values.
   */
  public static final String PREF_APPLY_PERCENTAGE_FILTER = "apply graph percentage filter";
  public static final boolean default_apply_percentage_filter = true;

  /** Whether to use a TransformTierGlyph to maintain a fixed pixel height for attached graphs. */
  static final boolean use_fixed_pixel_height = false;

  /** Default value of {@link #PREF_ATTACH_HEIGHT_MODE}. */
  public static final String default_attach_mode = USE_CURRENT_HEIGHT;  // default mode if not specified in prefs

  public static final int default_pix_height = 60;

  /** Default value for height of attached (non-floating) graphs.  Although
   *  the height will ultimately have to be expressed as a double rather than
   *  an integer, there is no good reason to bother the users with that detail,
   *  so the default should be treated as an integer.
   */
  public static final int default_coord_height = 100;
  public static final boolean default_use_floating_graphs = false;

  //public static final Color[] default_graph_colors =
  //  new Color[] {Color.CYAN, Color.PINK, Color.ORANGE, Color.YELLOW, Color.RED, Color.GREEN};

  /** The names of preferences for storing default graph colors can be
   *  constructed from this prefix by adding "0", "1", etc., up to
   *  default_graph_colors.length - 1.
   */
  public static final String PREF_GRAPH_COLOR_PREFIX = "graph color ";


  public static void toggleFloating(GraphGlyph gl, SeqMapView gviewer) {
    boolean is_floating = hasFloatingAncestor(gl);
    if (is_floating) {
      attachGraph(gl, gviewer);
    }
    else {
      floatGraph(gl, gviewer);
    }
  }

  public static void attachGraph(GraphGlyph gl, SeqMapView gviewer) {
    attachGraph(gl, gviewer, null);
  }

  public static TierGlyph attachGraph(GraphGlyph gl, SeqMapView gviewer, TierGlyph tglyph) {
    String attach2float_mode = getGraphPrefsNode().get(PREF_ATTACH_HEIGHT_MODE, default_attach_mode);

    boolean use_fixed_coord_height = attach2float_mode.equals(USE_DEFAULT_HEIGHT);

    AffyTieredMap map = (AffyTieredMap)gviewer.getSeqMap();
    GlyphI parentgl = gl.getParent();
    GraphSym graf = (GraphSym)gl.getInfo();
    Rectangle2D mapbox = map.getCoordBounds();

      // try to map floating graph coord bounds (which are in pixels) to coords
      //   to keep same size


    Rectangle2D tempbox = gl.getCoordBox();  // pixels, since in PixelFloaterGlyph 1:1 mapping of pixel:coord
    Rectangle pixbox = new Rectangle((int)tempbox.x,
				       (int)tempbox.y,
				       (int)tempbox.width,
				       (int)tempbox.height);
      Rectangle2D coordbox = new Rectangle2D();
      map.getView().transformToCoords(pixbox, coordbox);
      // System.out.println("switching to attached graph");
      // System.out.println("graph pixbox:   " + pixbox);
      // System.out.println("graph coordbox: " + coordbox);
      // actually, setting y won't matter since will get reset when tiers are packed,
      //    but setting height will matter
      double yheight = coordbox.height;
      if (use_fixed_coord_height) {
        yheight = getGraphPrefsNode().getDouble(PREF_ATTACHED_COORD_HEIGHT, (double) default_coord_height);
      }

      gl.setCoords(tempbox.x, coordbox.y, tempbox.width, yheight);

      // remove PixelFloaterGlyph, create new tier for graph, figure out coord size
      // that corresponds to float graph pixel size, resize graph, place graph in tier
      parentgl.removeChild(gl);
      // maybe should do recursive search of ancestors for removal???
      if (parentgl instanceof PixelFloaterGlyph) {
	map.removeItem(parentgl);
      }
      //      TierGlyph tglyph = new TierGlyph();
      boolean new_tier = (tglyph == null);
      if (new_tier) {
	// System.out.println("making new tier");
	if (use_fixed_pixel_height)  {
	  TransformTierGlyph tempgl = new TransformTierGlyph(graf.getGraphName());
	  tempgl.setFixedPixelHeight(true);
          int h = getGraphPrefsNode().getInt(PREF_FLOATING_PIXEL_HEIGHT, default_pix_height);
          tempgl.setFixedPixHeight(h);
	  tglyph = tempgl;
	}
	else { tglyph = new TierGlyph(graf.getGraphName()); }
        tglyph.setFillColor(Color.black);
	tglyph.setForegroundColor(gl.getColor());
      }
      else {
	//	System.out.println("using existing tier");
	GlyphI old_parent = tglyph.getParent();
	if (old_parent != null &&
	    old_parent instanceof TierGlyph &&
	    (old_parent != tglyph)) {
	  //	gviewer.removeTier((TierGlyph)old_parent);
	  map.removeTier((TierGlyph)old_parent);
	}
      }
      tglyph.addChild(gl);
      for (int i=0; i<tglyph.getChildCount(); i++) {
	GlyphI child = tglyph.getChild(i);
	if (child instanceof GraphGlyph) {
	  Rectangle2D childbox = child.getCoordBox();
	  //	  child.setCoords(childbox.x, coordbox.y, childbox.width, coordbox.height);
	  child.setCoords(childbox.x, coordbox.y, childbox.width, yheight);
	  //	  System.out.println("child " + i + ": " + gl.getCoordBox());
	}
      }

      if (graf != null) {
	tglyph.setLabel(graf.getGraphName());
      }
      else {
	tglyph.setLabel("unknown");
      }
      //      tglyph.setCoords(mapbox.x, graph_yloc, mapbox.width, graph_height);
      if (new_tier)  {
	map.addTier(tglyph, true);
	gviewer.getGraphStateTierHash().put(gl.getGraphState(), tglyph);
      }
      tglyph.setState(TierGlyph.COLLAPSED);
      tglyph.pack(map.getView());
      // Hmm, even though map.packTiers() is called in tiered map stretchToFit(),
      //  still seem to need to call it _before_ stretchToFit() to get proper
      //  zoom scroller behavior
      map.packTiers(false, true, false);
      //      map.adjustZoomer(map.Y);
      //      map.updateWidget();
      //      for (int i=0; i<tglyph.getChildCount(); i++) {
      //	GlyphI child = tglyph.getChild(i);
      //	System.out.println("child " + i + ": " + child.getCoordBox());
      //      }

      map.stretchToFit(false, true);
      map.updateWidget();
      gl.getGraphState().setFloatGraph(false);
      return tglyph;
  }

  public static void floatGraph(GraphGlyph gl, SeqMapView gviewer) {
      // remove graph tier, create new PixelFloaterGlyph for graph, figure out pixel
      // size that corresponds to tier graph coord size, resize graph, place graph in floater
    AffyTieredMap map = (AffyTieredMap)gviewer.getSeqMap();
    GlyphI parentgl = gl.getParent();
    GraphSym graf = (GraphSym)gl.getInfo();
    Rectangle2D mapbox = map.getCoordBounds();

      // try to map tiered graph coord bounds to pixels to keep same size
      Rectangle2D coordbox = gl.getCoordBox();
      Rectangle pixbox = new Rectangle();
      map.getView().transformToPixels(coordbox, pixbox);
      //      System.out.println("switching to floating graph");
      //      System.out.println("graph coordbox: " + coordbox);
      //      System.out.println("graph pixbox:   " + pixbox);
      gl.setCoords(coordbox.x, pixbox.y, coordbox.width, pixbox.height);

      parentgl.removeChild(gl);
      if (parentgl instanceof TierGlyph) {
	map.removeTier((TierGlyph)parentgl);
	gviewer.getGraphStateTierHash().remove(gl.getGraphState());
      }

      PixelFloaterGlyph floater = new PixelFloaterGlyph();
      map.addItem(floater);
      floater.addChild(gl);
      gl.getGraphState().setFloatGraph(true);
      floater.setCoords(mapbox.x, 0, mapbox.width, 0);
      //      gl.setCoords(mapbox.x, 0, mapbox.width, 0);
      map.packTiers(false, true, false);
      map.stretchToFit(false, true);
      // make sure graph is still within map's pixel bounds after switch to floating pixel layer
      checkPixelBounds(gl, gviewer);
      map.updateWidget();
    //    map.setDataModel(gl, graf);
  }

  /**
   *  Return true if graph coords were changed, false otherwise.
   *
   *  Assumes that graph glyph is a child of a PixelFloaterGlyph, so that
   *   the glyph's coord box is also it's pixel box.
   */
  public static boolean checkPixelBounds(GraphGlyph gl, SeqMapView gviewer) {
    boolean changed_coords = false;
    AffyTieredMap map = (AffyTieredMap)gviewer.getSeqMap();
    Rectangle mapbox = map.getView().getPixelBox();
    Rectangle2D gbox = gl.getCoordBox();
    //    System.out.println("in checkPixelBounds, mapbox:   " + mapbox);
    //    System.out.println("in checkPixelBounds, graphbox: " + gbox);
    if (gbox.y < mapbox.y) {
      gl.setCoords(gbox.x, mapbox.y, gbox.width, gbox.height);
      //      System.out.println("adjusting graph coords + : " + gl.getCoordBox());
      changed_coords = true;
    }
    else if (gbox.y > (mapbox.y + mapbox.height)) {
      gl.setCoords(gbox.x, mapbox.y+mapbox.height-gbox.height, gbox.width, gbox.height);
      //      System.out.println("adjusting graph coords - : " + gl.getCoordBox());
      changed_coords = true;
    }
    return changed_coords;
  }

  public static boolean hasFloatingAncestor(GlyphI gl) {
    if (gl == null)  { return false; }
    if (gl instanceof PixelFloaterGlyph) { return true; }
    else if (gl.getParent() == null) { return false; }
    else { return hasFloatingAncestor(gl.getParent()); }
  }

  /**
   *  very preliminary start on making MultiGraphs
   */
  public static GraphGlyph displayMultiGraph(java.util.List grafs,
					     AnnotatedBioSeq aseq, AffyTieredMap map,
					     java.util.List cols, double graph_yloc, double graph_height,
					     boolean use_floater) {
    System.out.println("trying to make SmartGraphGlyph for sliding window stats");
    MultiGraph multi_graph_glyph = new MultiGraph();
    multi_graph_glyph.setBackgroundColor(Color.white);
    multi_graph_glyph.setForegroundColor(Color.white);
    multi_graph_glyph.setColor(Color.white);

    Rectangle2D mapbox = map.getCoordBounds();
    multi_graph_glyph.setCoords(mapbox.x, graph_yloc, mapbox.width, graph_height);
    float maxy = Float.NEGATIVE_INFINITY;
    float miny = Float.POSITIVE_INFINITY;

    for (int i=0; i<grafs.size(); i++) {
      GraphSym graf = (GraphSym)grafs.get(i);
      SmartGraphGlyph graph_glyph = new SmartGraphGlyph(graf.getGraphXCoords(), graf.getGraphYCoords());
      // graph_glyph.setFasterDraw(true);
      // graph_glyph.setCalcCache(true);
      graph_glyph.setSelectable(false);
      graph_glyph.setLabel(graf.getGraphName());
      graph_glyph.setXPixelOffset(i);

      BioSeq graph_seq = graf.getGraphSeq();
      // graph_glyph.setPointCoords(graf.getGraphXCoords(), graf.getGraphYCoords());

      System.out.println("graf name: " + graf.getGraphName());
      graph_glyph.setGraphStyle(SmartGraphGlyph.MINMAXAVG);

      //    Color col = Color.yellow;
      Color col = (Color)cols.get(i);
      graph_glyph.setColor(col);
      map.setDataModel(graph_glyph, graf);

      System.out.println("Map Bounds: " + mapbox);
      graph_glyph.setCoords(mapbox.x, graph_yloc, mapbox.width, graph_height);
      maxy = Math.max(graph_glyph.getVisibleMaxY(), maxy);
      miny = Math.min(graph_glyph.getVisibleMinY(), miny);
      System.out.println("Graph Bounds: " + graph_glyph.getCoordBox());
      multi_graph_glyph.addGraph(graph_glyph);
    }

    java.util.List glyphs = multi_graph_glyph.getGraphs();
    multi_graph_glyph.setVisibleMinY(0);
    //    multi_graph_glyph.setVisibleMinY(miny);
    multi_graph_glyph.setVisibleMaxY(maxy);

    PixelFloaterGlyph floater = new PixelFloaterGlyph();
    floater.addChild(multi_graph_glyph);
    map.addItem(floater);

    map.updateWidget();
    return multi_graph_glyph;
  }

  public static Preferences getGraphPrefsNode() {
    return UnibrowPrefsUtil.getTopNode().node("graphs");
  }

//  public static Color getDefaultGraphColor(int i) {
//    int index = (i % default_graph_colors.length);
//    String color_pref_name = PREF_GRAPH_COLOR_PREFIX + index;
//    Color col = UnibrowPrefsUtil.getColor(getGraphPrefsNode(), color_pref_name, default_graph_colors[index]);
//    return col;
//  }
}
