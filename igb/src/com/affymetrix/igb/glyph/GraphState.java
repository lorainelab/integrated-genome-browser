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

package com.affymetrix.igb.glyph;

import com.affymetrix.igb.tiers.AnnotStyle;
import com.affymetrix.igb.tiers.IAnnotStyle;
import com.affymetrix.igb.util.GraphGlyphUtils;
import java.awt.Color;
import java.util.*;

/**
 *  Encapsulates information needed to restore the visual appearance of
 *    a graph stored at a URL.
 */
public class GraphState implements IAnnotStyle {

  String graph_path;
  String graph_label;
  int graph_style = SmartGraphGlyph.MINMAXAVG;
  Color graph_col = Color.pink;

  double graph_ypos = 20;
  double graph_height = 60;

  /**
   *  The minimum score the ycoord of a point must be >= in order to be counted
   *     as passing threshold.
   */
  float min_score_threshold = Float.NEGATIVE_INFINITY;  // NEGATIVE_INFINITY means hasn't been set yet

  /**
   *  The maximum score the ycoord of a point must be <= in order to be counted
   *    as passing threshold
   */
  float max_score_threshold = Float.POSITIVE_INFINITY;  // POSITIVE_INFINITY means hasn't been set yet

  public static int THRESHOLD_DIRECTION_GREATER = 1;
  public static int THRESHOLD_DIRECTION_BETWEEN = 0;
  public static int THRESHOLD_DIRECTION_LESS = -1;

  int threshold_direction = THRESHOLD_DIRECTION_GREATER;
  
  
  /**
   *  The maximum distance (in xcooords) allowed between points that
   *     exceed the min_score_threshold in order for region between points to be painted
   */
  int max_gap_threshold = 100;  // max_gap

  /**
   *  The minumum length (in xcoords) that must be covered by a region that
   *    meets min_score and max_distance thresholds in order for region between
   *    points to be painted
   */
  int min_run_threshold = 30;

  /**  how much to shift xcoords of span start when painting spans that pass thresholds */
  double span_start_shift = 12;  // currently should be 12 for transcriptome graphs
  /**  how much to shift xcoords of span end when painting spans that pass thresholds */
  double span_end_shift = 13;  // currently should be 13 for transcriptome graphs

  /**
   *  visible_max_ycoord is the max ycoord (in graph coords) that is visible (rendered)
   *  this number is set to point_max_ycoord in setPointCoords(), but can be modified via
   *     calls to setVisibleMaxY, and the visual effect is to threhsold the graph drawing
   *     so that any points above visible_max_ycoord render as visible_max_ycoord
   */
  float graph_visible_min = Float.NEGATIVE_INFINITY;  // NEGATIVE_INFINITY means hasn't been set yet
  float graph_visible_max = Float.POSITIVE_INFINITY;  // POSITIVE_INFINITY means hasn't been set yet

  // if float_graph, then graph should float above annotations in tiers
  // if !float_graph, then graph should be in its own tier
  boolean float_graph = true;
  boolean show_threshold = false;
  boolean show_axis = false;
  boolean show = true; // whether to show or hide the graph. (Usually true.)

  boolean show_handle = true;
  boolean show_graph = true;
  boolean show_bounds = false;
  boolean show_label = true;

  HeatMap heat_map = GraphGlyph.default_heatmap;
  
  public static Map gstyle2num = new HashMap();
  public static Map num2gstyle = new HashMap();

  static {
    gstyle2num.put("line", new Integer(SmartGraphGlyph.LINE_GRAPH));
    gstyle2num.put("bar", new Integer(SmartGraphGlyph.BAR_GRAPH));
    gstyle2num.put("dot", new Integer(SmartGraphGlyph.DOT_GRAPH));
    gstyle2num.put("stairstep", new Integer(SmartGraphGlyph.STAIRSTEP_GRAPH));
    gstyle2num.put("heatmap", new Integer(SmartGraphGlyph.HEAT_MAP));
    gstyle2num.put("minmaxavg", new Integer(SmartGraphGlyph.MINMAXAVG));
    //    gstyle2num.put("span", new Integer(SmartGraphGlyph.SPAN_GRAPH));  // SPAN_GRAPH is deprecated
    
    Iterator iter = gstyle2num.entrySet().iterator();
    while (iter.hasNext()) {
      Map.Entry ent = (Map.Entry)iter.next();
      Integer style_num = (Integer)ent.getValue();
      String style_name = (String)ent.getKey();
      num2gstyle.put(style_num, style_name);
    }
  }

  public static Integer getStyleNumber(String style_name) {
    return (Integer)gstyle2num.get(style_name.toLowerCase());
  }
  
  public static int getStyleInt(String style_name) {
    Integer num = getStyleNumber(style_name);
    if (num == null) { return -1; }
    else { return num.intValue(); }
  }

  public static String getStyleName(int style_int) {
    return (String)num2gstyle.get(new Integer(style_int));
  }


  public GraphState() { 
    super();
    
    // Now set some defaults.
    // (This used to be done in the GenericGraphGlyphFactory.)

    boolean use_floating_graphs = GraphGlyphUtils.getGraphPrefsNode().getBoolean(
      GraphGlyphUtils.PREF_USE_FLOATING_GRAPHS, GraphGlyphUtils.default_use_floating_graphs);
      setFloatGraph(use_floating_graphs);

    // graph height is in coords if graph is attached, and in pixels if graph is floating
    if (getFloatGraph()) {
      int pix_height = GraphGlyphUtils.getGraphPrefsNode().getInt(
        GraphGlyphUtils.PREF_FLOATING_PIXEL_HEIGHT, GraphGlyphUtils.default_pix_height);
      setGraphHeight(pix_height);
    }
    else {
      double coord_height = GraphGlyphUtils.getGraphPrefsNode().getDouble(
        GraphGlyphUtils.PREF_ATTACHED_COORD_HEIGHT, GraphGlyphUtils.default_coord_height);
      setGraphHeight(coord_height);
    }

    setColor(AnnotStyle.getDefaultInstance().getColor());    
  }

  public GraphState(GraphState ostate) {
    this();
    setUrl(ostate.getUrl());
    setLabel(ostate.getLabel());
    setGraphStyle(ostate.getGraphStyle());
    setColor(ostate.getColor());
    setGraphYPos(ostate.getGraphYPos());
    setGraphHeight(ostate.getGraphHeight());
    setVisibleMinY(ostate.getVisibleMinY());
    setVisibleMaxY(ostate.getVisibleMaxY());
    setFloatGraph(ostate.getFloatGraph());
    setShowThreshold(ostate.getShowThreshold());
    setShowAxis(ostate.getShowAxis());
    setShowHandle(ostate.getShowHandle());
    setShowGraph(ostate.getShowGraph());
    setShowBounds(ostate.getShowBounds());
    setShowLabel(ostate.getShowLabel());
    setThresholdDirection(ostate.getThresholdDirection());
    setMinScoreThreshold(ostate.getMinScoreThreshold());
    setMaxScoreThreshold(ostate.getMaxScoreThreshold());
    setMaxGapThreshold(ostate.getMaxGapThreshold());
    setMinRunThreshold(ostate.getMinRunThreshold());
    setThreshStartShift(ostate.getThreshStartShift());
    setThreshEndShift(ostate.getThreshEndShift());
    setHeatMap(ostate.getHeatMap());
  }


  public final String getUrl() { return graph_path; }
  public final String getLabel() { return graph_label; }
  public final int getGraphStyle() { return graph_style; }
  public final HeatMap getHeatMap() { return heat_map; }
  public final Color getColor() { return graph_col; }

  public final double getGraphYPos() { return graph_ypos; }
  public final double getGraphHeight() { return graph_height; }
  public final float getVisibleMinY() { return graph_visible_min; }
  public final float getVisibleMaxY() { return graph_visible_max; }

  public final boolean getFloatGraph() { return float_graph; }
  public final boolean getShowThreshold() { return show_threshold; }
  public final boolean getShowAxis() { return show_axis; }

  public final boolean getShowHandle() { return show_handle; }
  public final boolean getShowGraph() { return show_graph; }
  public final boolean getShowBounds() { return show_bounds; }
  public final boolean getShowLabel() { return show_label; }
  public final boolean getShow() { return show; }

  public float getMinScoreThreshold() { return min_score_threshold; }
  public float getMaxScoreThreshold() { return max_score_threshold; }

  public int getMaxGapThreshold() { return max_gap_threshold; }
  public int getMinRunThreshold() { return min_run_threshold; }
  public double getThreshStartShift() { return span_start_shift; }
  public double getThreshEndShift() { return span_end_shift; }
  
  /**
   *  Returns either {@link #THRESHOLD_DIRECTION_GREATER} or
   *  {@link #THRESHOLD_DIRECTION_LESS} or {@link #THRESHOLD_DIRECTION_BETWEEN}.
   */
  public int getThresholdDirection() { return threshold_direction; }

  public final void setUrl(String url) { graph_path = url; }
  public final void setLabel(String name) { graph_label = name; }
  public final void setFloatGraph(boolean b) { float_graph = b; }
  public final void setGraphYPos(double ypos) { graph_ypos = ypos; }
  public final void setGraphHeight(double height) { graph_height = height; }

  public final void setGraphStyle(int style) { graph_style = style; } 
  public final void setHeatMap(HeatMap hmap) { heat_map = hmap; }
  public final void setColor(Color col) { graph_col = col; }  // check
  public final void setVisibleMinY(float vminy) { graph_visible_min = vminy; }  // check
  public final void setVisibleMaxY(float vmaxy) { graph_visible_max = vmaxy; }  // check
  public final void setShowThreshold(boolean b) { show_threshold = b; } // check
  public final void setShowAxis(boolean b) { show_axis = b; } // check
  public final void setShowHandle(boolean b) { show_handle = b; }  // check
  public final void setShowGraph(boolean b) { show_graph = b; }    // check
  public final void setShowBounds(boolean b) { show_bounds = b; }  // check
  public final void setShowLabel(boolean b) { show_label = b; }    // check
  public final void setShow(boolean b) { show = b; }

  public void setMinScoreThreshold(float thresh) { min_score_threshold = thresh; }
  public void setMaxScoreThreshold(float thresh) { max_score_threshold = thresh; }
  public void setMaxGapThreshold(int thresh) { max_gap_threshold = thresh; }
  public void setMinRunThreshold(int thresh) { min_run_threshold = thresh; }
  public void setThreshStartShift(double d) { span_start_shift = d; }
  public void setThreshEndShift(double d) { span_end_shift = d; }
  
  /**
   *  Set to either {@link #THRESHOLD_DIRECTION_GREATER} or
   *  {@link #THRESHOLD_DIRECTION_LESS} or {@link #THRESHOLD_DIRECTION_BETWEEN}.
   */
  public void setThresholdDirection(int d) {
    if (d != THRESHOLD_DIRECTION_GREATER && d != THRESHOLD_DIRECTION_LESS
        && d != THRESHOLD_DIRECTION_BETWEEN) {
      throw new IllegalArgumentException();
    }
    threshold_direction = d;
  }

  /** Simply returns the background color of the default AnnotStyle. */
  public Color getBackground() {
    return AnnotStyle.getDefaultInstance().getBackground();
  }
  
  /** Has no effect. */
  public void setBackground(Color c) {
    return;
  }

  // Always returns false, since graphs can't be collapsed.
  /**
   *  Always returns true, to allow for multiple graphs overlaid on same tier?
   */
  public boolean getCollapsed() {
    //    return false;
    return true;
  }

  /** Returns the same thing as getLabel(). */
  public String getHumanName() {
    return getLabel();
  }
  
  /** Same as setLabel(String). */
  public void setHumanName(String s) {
    setLabel(s);
  }

  /** Always returns zero, since graphs have no idea of max depth. */
  public int getMaxDepth() {
    return 0;
  }
}
