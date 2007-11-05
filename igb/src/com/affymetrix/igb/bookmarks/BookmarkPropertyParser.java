/**
 *   Copyright (c) 2006 Affymetrix, Inc.
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

package com.affymetrix.igb.bookmarks;

import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.style.*;
import com.affymetrix.igb.bookmarks.UnibrowControlServlet;
import com.affymetrix.igb.util.ErrorHandler;
import java.awt.Color;
import java.util.*;

public class BookmarkPropertyParser {
  static boolean DEBUG = false;
  
  static double default_ypos = 30;
  static double default_yheight = 60;
  static Color default_col = Color.lightGray;
  static boolean default_float = true;
  static boolean default_show_label = true;
  static boolean default_show_axis = false;
  static double default_minvis = Double.NEGATIVE_INFINITY;
  static double default_maxvis = Double.POSITIVE_INFINITY;
  static double default_score_thresh = 0;
  static int default_minrun_thresh = 30;
  static int default_maxgap_thresh = 100;
  static boolean default_show_thresh = false;
  static int default_thresh_direction = GraphStateI.THRESHOLD_DIRECTION_GREATER;

    
  public static void applyGraphProperties(Collection grafs, Map map) {
    Collection states = new ArrayList(grafs.size());
    Iterator graf_iter = grafs.iterator();
    while (graf_iter.hasNext()) {
      GraphSym graf = (GraphSym) graf_iter.next();
      GraphStateI gstate = graf.getGraphState();
      states.add(gstate);
    }
    applyGraphPropertiesToStates(states, map);
  }

  public static void applyGraphPropertiesToStates(Collection states, Map map) {
    if (states != null) {
      Map combos = new HashMap();
      Iterator states_iter = states.iterator();
      int i = -1;

      while (states_iter.hasNext()) {
        GraphStateI gstate = (GraphStateI) states_iter.next();
        i++;

        String graph_path = UnibrowControlServlet.getStringParameter(map, "graph_source_url_" + i);
        // for some parameters, testing more than one parameter name because how some params used to have
        //    slightly different names, and we need to support legacy bookmarks
        String graph_name = UnibrowControlServlet.getStringParameter(map, "graph_name_" + i);
        if (graph_name == null || graph_name.trim().length()==0) {
          graph_name = graph_path;
        }

        // Skip any that don't have a graph_source_url_### or graph_name_### defined.
        if (graph_name == null || graph_name.trim().length()==0) {
          continue;
        }

        String graph_ypos = UnibrowControlServlet.getStringParameter(map, "graph_ypos_" + i);
        if (graph_ypos == null)  { graph_ypos = UnibrowControlServlet.getStringParameter(map, "graphypos" + i); }
        
        String graph_height = UnibrowControlServlet.getStringParameter(map, "graph_yheight_" + i);
        if (graph_height == null) { graph_height = UnibrowControlServlet.getStringParameter(map, "graphyheight" + i); }
        // graph_col is String rep of RGB integer
        String graph_col = UnibrowControlServlet.getStringParameter(map, "graph_col_" + i);
        if (graph_col == null)  { graph_col = UnibrowControlServlet.getStringParameter(map, "graphcol" + i); }

        String graph_bg_col = UnibrowControlServlet.getStringParameter(map, "graph_bg_" + i);
        // graph_bg_col will often be null
        
        String graph_float = UnibrowControlServlet.getStringParameter(map, "graph_float_" + i);
        if (graph_float == null)  { graph_float = UnibrowControlServlet.getStringParameter(map, "graphfloat" + i); }
        
        String show_labelstr = UnibrowControlServlet.getStringParameter(map, "graph_show_label_" + i);
        String show_axisstr = UnibrowControlServlet.getStringParameter(map, "graph_show_axis_" + i);
        String minvis_str = UnibrowControlServlet.getStringParameter(map, "graph_minvis_" + i);
        String maxvis_str = UnibrowControlServlet.getStringParameter(map, "graph_maxvis_" + i);
        String score_threshstr = UnibrowControlServlet.getStringParameter(map, "graph_score_thresh_" + i);
        String maxgap_threshstr = UnibrowControlServlet.getStringParameter(map, "graph_maxgap_thresh_" + i);
        String minrun_threshstr = UnibrowControlServlet.getStringParameter(map, "graph_minrun_thresh_" + i);
        String show_threshstr = UnibrowControlServlet.getStringParameter(map, "graph_show_thresh_" + i);
        String thresh_directionstr = UnibrowControlServlet.getStringParameter(map, "graph_thresh_direction_" + i);
        
        //        int graph_min = (graph_visible_min == null) ?
        String graph_style = UnibrowControlServlet.getStringParameter(map, "graph_style_" + i);
        String heatmap_name = UnibrowControlServlet.getStringParameter(map, "graph_heatmap_" + i);
        Integer graph_style_num = null;
        if (graph_style != null) {
          //	  graph_style_num = (Integer)gstyle2num.get(graph_style);
          graph_style_num = GraphState.getStyleNumber(graph_style);
        }
                
        String combo_name = UnibrowControlServlet.getStringParameter(map, "graph_combo_" + i);
        
        double ypos = (graph_ypos == null) ? default_ypos : Double.parseDouble(graph_ypos);
        double yheight = (graph_height == null)  ? default_yheight : Double.parseDouble(graph_height);
        Color col = default_col;
        Color bg_col = Color.BLACK;
        if (graph_col != null) try {
          // Color.decode() can handle colors in plain integer format
          // as well as hex format: "-20561" == "#FFAFAF" == "0xFFAFAF" == "16756655"
          // We now write in the hex format, but can still read the older int format.
          col = Color.decode(graph_col);
        } catch (NumberFormatException nfe) {
          ErrorHandler.errorPanel("Couldn't parse graph color from '"+graph_col+"'\n"+
              "Please use a hexidecimal RGB format,\n e.g. red = '0xFF0000', blue = '0x0000FF'.");
        }
        if (graph_bg_col != null) try {
          bg_col = Color.decode(graph_bg_col);
        } catch (NumberFormatException nfe) {
          ErrorHandler.errorPanel("Couldn't parse graph background color from '"+graph_bg_col+"'\n"+
              "Please use a hexidecimal RGB format,\n e.g. red = '0xFF0000', blue = '0x0000FF'.");
        }
        boolean use_floating_graphs =
            (graph_float == null) ? default_float : (graph_float.equals("true"));
        boolean show_label =
            (show_labelstr == null) ? default_show_label : (show_labelstr.equals("true"));
        boolean show_axis =
            (show_axisstr == null) ? default_show_axis : (show_axisstr.equals("true"));
        double minvis = (minvis_str == null) ? default_minvis : Double.parseDouble(minvis_str);
        double maxvis = (maxvis_str == null) ? default_maxvis : Double.parseDouble(maxvis_str);
        double score_thresh =
            (score_threshstr == null) ? default_score_thresh : Double.parseDouble(score_threshstr);
        int maxgap_thresh =
            (maxgap_threshstr == null) ? default_maxgap_thresh : Integer.parseInt(maxgap_threshstr);
        
        int minrun_thresh =
            (minrun_threshstr == null) ? default_minrun_thresh : Integer.parseInt(minrun_threshstr);
        boolean show_thresh =
            (show_threshstr == null) ? default_show_thresh : (show_threshstr.equals("true"));
        int thresh_direction =
            (thresh_directionstr == null) ? default_thresh_direction : Integer.parseInt(thresh_directionstr);
                
        if (DEBUG) {
          System.out.println("graph name: " + graph_name);
          System.out.println(col+", "+ypos+", "+ yheight
              +", "+use_floating_graphs+", "+show_label+", "+ show_axis
              +", "+minvis+", "+maxvis+", "
              + score_thresh+", "+maxgap_thresh+", "
              + show_thresh + ", " + thresh_direction);
        }
        
        gstate.getTierStyle().setHumanName(graph_name);
        if (graph_style_num != null)  {
          gstate.setGraphStyle(graph_style_num.intValue());
        }
        if (heatmap_name != null) {
          HeatMap heat_map = HeatMap.getStandardHeatMap(heatmap_name);
          if (heat_map != null) {
            gstate.setHeatMap(heat_map);
          }
        }
        IAnnotStyle tier_style = gstate.getTierStyle();
        tier_style.setColor(col);
        tier_style.setBackground(bg_col);
        tier_style.setY(ypos);
        tier_style.setHeight(yheight);
        gstate.setFloatGraph(use_floating_graphs);
        gstate.setShowLabel(show_label);
        gstate.setShowAxis(show_axis);
        gstate.setVisibleMinY((float) minvis);
        gstate.setVisibleMaxY((float) maxvis);
        gstate.setMinScoreThreshold((float) score_thresh);
        gstate.setMinRunThreshold(minrun_thresh);
        gstate.setMaxGapThreshold(maxgap_thresh);
        gstate.setShowThreshold(show_thresh);
        gstate.setThresholdDirection(thresh_direction);
        
        if (combo_name != null) {
          IAnnotStyle combo_style = (IAnnotStyle) combos.get(combo_name);
          if (combo_style == null) {
            combo_style = new DefaultIAnnotStyle("Joined Graphs", true);
            combo_style.setHumanName("Joined Graphs");
            combo_style.setExpandable(true);
            combo_style.setCollapsed(true);
            combos.put(combo_name, combo_style);
          }
          gstate.setComboStyle(combo_style);
        }
      }
    }
  }
}
