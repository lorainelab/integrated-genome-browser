/**
*   Copyright (c) 2001-2006 Affymetrix, Inc.
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

import com.affymetrix.genometryImpl.GraphSymFloat;
import java.awt.Color;
import java.awt.Rectangle;
//import java.util.List;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genometry.*;
import com.affymetrix.genometryImpl.GraphIntervalSym;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.glyph.*;
import com.affymetrix.igb.tiers.*;
import java.util.prefs.Preferences;

public class GraphGlyphUtils {
  public static final boolean DEBUG = false;

  public static final String PREF_USE_FLOATING_GRAPHS = "use floating graphs";
  public static final String PREF_ATTACHED_COORD_HEIGHT = "default attached graph coord height";

  /** Pref for whether newly-constructed graph glyphs should only show a
   *  limited range of values.
   */
  public static final String PREF_APPLY_PERCENTAGE_FILTER = "apply graph percentage filter";
  public static final boolean default_apply_percentage_filter = true;

  public static final String PREF_USE_URL_AS_NAME = "Use complete URL as graph name";
  public static final boolean default_use_url_as_name = false;
  
  /** Whether to use a TransformTierGlyph to maintain a fixed pixel height for attached graphs. */
  static final boolean use_fixed_pixel_height = false;

  /** Default value for height of attached (non-floating) graphs.  Although
   *  the height will ultimately have to be expressed as a double rather than
   *  an integer, there is no good reason to bother the users with that detail,
   *  so the default should be treated as an integer.
   */
  public static final int default_coord_height = 100;
  public static final boolean default_use_floating_graphs = false;

  public static final Color[] default_graph_colors =
      new Color[] {Color.CYAN, Color.PINK, Color.ORANGE, Color.YELLOW, Color.RED, Color.GREEN};

  /** The names of preferences for storing default graph colors can be
   *  constructed from this prefix by adding "0", "1", etc., up to
   *  default_graph_colors.length - 1.
   */
  public static final String PREF_GRAPH_COLOR_PREFIX = "graph color ";



  /**
   *  Checks to make sure the the boundaries of a floating glyph are
   *  inside the map view.
   *  See {@link #checkPixelBounds(GraphGlyph, AffyTieredMap)}.
   */
  public static boolean checkPixelBounds(GraphGlyph gl, SeqMapView gviewer) {
    AffyTieredMap map = gviewer.getSeqMap();
    return checkPixelBounds(gl, map);
  }

  /**
   *  Checks to make sure the the boundaries of a floating glyph are
   *  inside the map view.
   *  Return true if graph coords were changed, false otherwise.
   *  If the glyph is not a floating glyph, this will have no effect on it
   *  and will return false.
   *  Assumes that graph glyph is a child of a PixelFloaterGlyph, so that
   *   the glyph's coord box is also it's pixel box.
   */
  public static boolean checkPixelBounds(GraphGlyph gl, AffyTieredMap map) {
    boolean changed_coords = false;
    if (gl.getGraphState().getFloatGraph() == true) {
      Rectangle mapbox = map.getView().getPixelBox();
      Rectangle2D gbox = gl.getCoordBox();
      if (gbox.y < mapbox.y) {
        gl.setCoords(gbox.x, mapbox.y, gbox.width, gbox.height);
        //      System.out.println("adjusting graph coords + : " + gl.getCoordBox());
        changed_coords = true;
      }
      else if (gbox.y > (mapbox.y + mapbox.height - 10)) {
        gl.setCoords(gbox.x, mapbox.y + mapbox.height - 10, gbox.width, gbox.height);
        //      System.out.println("adjusting graph coords - : " + gl.getCoordBox());
        changed_coords = true;
      }
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
  /*
  public static GraphGlyph displayMultiGraph(List grafs,
					     AnnotatedBioSeq aseq, AffyTieredMap map,
					     List cols, double graph_yloc, double graph_height,
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
      GraphState gstate = GraphState.getTemporaryGraphState();
      SmartGraphGlyph graph_glyph = new SmartGraphGlyph(graf, gstate);
      // graph_glyph.setFasterDraw(true);
      // graph_glyph.setCalcCache(true);
      graph_glyph.setSelectable(false);
      gstate.getTierStyle().setHumanName(graf.getGraphName());
      graph_glyph.setXPixelOffset(i);

      //BioSeq graph_seq = graf.getGraphSeq();
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

    //List glyphs = multi_graph_glyph.getGraphs();
    multi_graph_glyph.setVisibleMinY(0);
    //    multi_graph_glyph.setVisibleMinY(miny);
    multi_graph_glyph.setVisibleMaxY(maxy);

    PixelFloaterGlyph floater = new PixelFloaterGlyph();
    floater.addChild(multi_graph_glyph);
    map.addItem(floater);

    map.updateWidget();
    return multi_graph_glyph;
  }*/

  public static Preferences getGraphPrefsNode() {
    return UnibrowPrefsUtil.getTopNode().node("graphs");
  }

  /**
   *  Checks to make sure that two graphs can be compared with one
   *  another for operations like diff, ratio, etc.
   *  (Graphs must have exact same x positions, and if one has width coords,
   *   the other must also.)
   *  @return null if the graphs are comparable, or an explanation string if they are not.
   */
  public static String graphsAreComparable(GraphGlyph graphA, GraphGlyph graphB) {
    // checking that both graphs are non-null
    if (graphA == null || graphB == null) {
      return "Must select exactly two graphs";
    }
    int numpoints = graphA.getPointCount();
    // checking that both graph have same number of points
    if (numpoints != graphB.getPointCount()) {
      return "Graphs must have the same X points";
    }
    if ((graphA.getWCoords() == null) != (graphB.getWCoords() == null)) {
      // one has width coords, the other doesn't.
      return "Must select two graphs of the same type";
    }

    int[] xcoordsA = graphA.getXCoords();
    int[] xcoordsB = graphB.getXCoords();
    // checking that both graphs have same x points
    for (int i=0; i<numpoints; i++) {
      if (xcoordsA[i] != xcoordsB[i]) {
        return "Graphs must have the same X points";
      }
    }

    return null;
  }
  
  public static final String MATH_SUM = "sum";
  public static final String MATH_DIFFERENCE = "diff";
  public static final String MATH_PRODUCT = "product";
  public static final String MATH_RATIO = "ratio";
  public static final String[] math = new String[] {MATH_SUM, MATH_DIFFERENCE, MATH_PRODUCT, MATH_RATIO};
  
  /**
   *  Combines two graphs by the given arithmetical operation.
   *  Returns null if the two graphs are not comparable via {@link #graphsAreComparable(GraphGlyph,GraphGlyph)}.
   *  During division, indefinite values are replaced by zero.
   */
  public static GraphSymFloat graphArithmetic(GraphGlyph graphA, GraphGlyph graphB, String operation) {
    String error = GraphGlyphUtils.graphsAreComparable(graphA, graphB);
    
    if (error != null) {
      ErrorHandler.errorPanel("ERROR", error);
      return null;
    }
    
    int numpoints = graphA.getPointCount();
    //float[] yA = graphA.getYCoords();
    //float[] yB = graphB.getYCoords();
    float newY[] = new float[numpoints];
    
    String symbol = ",";
    if (MATH_SUM.equals(operation)) {
      for (int i=0; i<numpoints; i++) {
        newY[i] = graphA.getYCoord(i) + graphB.getYCoord(i);
      }
      symbol = "+";
    } else if (MATH_DIFFERENCE.equals(operation)) {
      for (int i=0; i<numpoints; i++) {
        newY[i] = graphA.getYCoord(i) - graphB.getYCoord(i);
      }
      symbol = "-";
    } else if (MATH_PRODUCT.equals(operation)) {
      for (int i=0; i<numpoints; i++) {
        newY[i] = graphA.getYCoord(i) * graphB.getYCoord(i);
      }
      symbol = "*";
    } else if (MATH_RATIO.equals(operation)) {
      for (int i=0; i<numpoints; i++) {
        if (graphB.getYCoord(i)== 0) {
          newY[i] = 0; // hack to avoid infinities
        } else {
          newY[i] = graphA.getYCoord(i) / graphB.getYCoord(i);
        }
        if (Float.isInfinite(newY[i]) || Float.isNaN(newY[i])) {
          newY[i] = 0.0f;
        }
      }
      symbol = "/";
    }
    
    String newname = operation + ": (" + graphA.getLabel() + ") " +
        symbol + " (" + graphB.getLabel() + ")";
    
    MutableAnnotatedBioSeq aseq =
        (MutableAnnotatedBioSeq)((GraphSym)graphA.getInfo()).getGraphSeq();
    newname = GraphSymUtils.getUniqueGraphID(newname, aseq);
    GraphSymFloat newsym;
    if (graphA.getWCoords() == null) {
      newsym = new GraphSymFloat(graphA.getXCoords(), newY, newname, aseq);
    } else {
      newsym = new GraphIntervalSym(graphA.getXCoords(), graphA.getWCoords(), newY, newname, aseq);
    }
    
    //newsym.getGraphState().copyProperties(graphA.getGraphState());
    
    newsym.setGraphName(newname);
    newsym.getGraphState().setGraphStyle(graphA.getGraphState().getGraphStyle());
    newsym.getGraphState().setHeatMap(graphA.getGraphState().getHeatMap());
    return newsym;
  }

  /**
   * @deprecated
   */
  public static Color getDefaultGraphColor(int i) {
    int index = (i % default_graph_colors.length);
    String color_pref_name = PREF_GRAPH_COLOR_PREFIX + index;
    Color col = UnibrowPrefsUtil.getColor(getGraphPrefsNode(), color_pref_name, default_graph_colors[index]);
    return col;
  }

}
