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

package com.affymetrix.igb.glyph;

import java.awt.*;

/**
 *  Encapsulates information needed to restore the visual appearance of
 *    a graph stored at a URL.
 */
public class GraphState {

  String graph_path;
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
  double span_start_shift = 0;  // currently should be 12 for transcriptome graphs
  /**  how much to shift xcoords of span end when painting spans that pass thresholds */
  double span_end_shift = 0;  // currently should be 13 for transcriptome graphs


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

  boolean show_handle = true;
  boolean show_graph = true;
  boolean show_bounds = false;
  boolean show_label = true;

  public GraphState() { super(); }
  public GraphState(GraphState ostate) {
    this();
    setUrl(ostate.getUrl());
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
    setMinScoreThreshold(ostate.getMinScoreThreshold());
    setMaxScoreThreshold(ostate.getMaxScoreThreshold());
    setMaxGapThreshold(ostate.getMaxGapThreshold());
    setMinRunThreshold(ostate.getMinRunThreshold());
    setThreshStartShift(ostate.getThreshStartShift());
    setThreshEndShift(ostate.getThreshEndShift());
  }


  public final String getUrl() { return graph_path; }
  public final int getGraphStyle() { return graph_style; }
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

  public float getMinScoreThreshold() { return min_score_threshold; }
  public float getMaxScoreThreshold() { return max_score_threshold; }

  public int getMaxGapThreshold() { return max_gap_threshold; }
  public int getMinRunThreshold() { return min_run_threshold; }
  public double getThreshStartShift() { return span_start_shift; }
  public double getThreshEndShift() { return span_end_shift; }

  public final void setUrl(String url) { graph_path = url; }
  public final void setFloatGraph(boolean b) { float_graph = b; }
  public final void setGraphYPos(double ypos) { graph_ypos = ypos; }
  public final void setGraphHeight(double height) { graph_height = height; }

  public final void setGraphStyle(int style) { graph_style = style; } 
  public final void setColor(Color col) { graph_col = col; }  // check
  public final void setVisibleMinY(float vminy) { graph_visible_min = vminy; }  // check
  public final void setVisibleMaxY(float vmaxy) { graph_visible_max = vmaxy; }  // check
  public final void setShowThreshold(boolean b) { show_threshold = b; } // check
  public final void setShowAxis(boolean b) { show_axis = b; } // check
  public final void setShowHandle(boolean b) { show_handle = b; }  // check
  public final void setShowGraph(boolean b) { show_graph = b; }    // check
  public final void setShowBounds(boolean b) { show_bounds = b; }  // check
  public final void setShowLabel(boolean b) { show_label = b; }    // check

  public void setMinScoreThreshold(float thresh) { min_score_threshold = thresh; }
  public void setMaxScoreThreshold(float thresh) { max_score_threshold = thresh; }
  public void setMaxGapThreshold(int thresh) { max_gap_threshold = thresh; }
  public void setMinRunThreshold(int thresh) { min_run_threshold = thresh; }
  public void setThreshStartShift(double d) { span_start_shift = d; }
  public void setThreshEndShift(double d) { span_end_shift = d; }



}
