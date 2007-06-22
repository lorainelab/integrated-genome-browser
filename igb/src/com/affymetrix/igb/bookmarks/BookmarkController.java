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

package com.affymetrix.igb.bookmarks;

import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.NibbleBioSeq;
import com.affymetrix.genometryImpl.GraphSym;
import java.awt.Color;
import java.util.*;
import java.io.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometryImpl.*;
import com.affymetrix.genoviz.bioviews.Rectangle2D;

import com.affymetrix.igb.Application;
import com.affymetrix.igb.glyph.GraphGlyph;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphStateI;
import com.affymetrix.genometryImpl.style.HeatMap;
import com.affymetrix.igb.glyph.SmartGraphGlyph;
import com.affymetrix.igb.tiers.DefaultIAnnotStyle;
import com.affymetrix.genometryImpl.style.IAnnotStyle;
import com.affymetrix.igb.util.*;
import com.affymetrix.igb.servlets.*;
import com.affymetrix.igb.view.SeqMapView;
import java.net.URL;
import javax.swing.SwingUtilities;

/**
 *  Allows creation of bookmarks based on a SeqSymmetry, and viewing of
 *  a bookmark.
 */
public abstract class BookmarkController {
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  private final static boolean DEBUG= false;
  
  /** Causes a bookmark to be executed.  If this is a Unibrow bookmark,
   *  it will be opened in the viewer using
   *  {@link UnibrowControlServlet#goToBookmark}.  If it is an external
   *  bookmark, it will be opened in an external browser, using
   *  {@link WebBrowserControl#displayURL(String)}.
   */
  public static void viewBookmark(Application app, Bookmark bm) {
    if (bm.isUnibrowControl()) {
      if (DEBUG) System.out.println("****** Viewing internal control bookmark: "+bm.getURL().toExternalForm());
      try {
        Map props = bm.parseParameters(bm.getURL());
        UnibrowControlServlet.goToBookmark(app, props);
      } catch (Exception e) {
        String message = e.getClass().getName() + ": " + e.getMessage();
        ErrorHandler.errorPanel("Error opening bookmark.\n" + message);
      }
    } else {
      if (DEBUG) System.out.println("****** Viewing external bookmark: "+bm.getURL().toExternalForm());
      WebBrowserControl.displayURLEventually(bm.getURL().toExternalForm());
    }
  }
  
  /** Causes a bookmark to be executed.
   *  @param gviewer  a useless, ignored parameter.
   */
  public static void viewBookmark(Application app, SeqMapView gviewer, Bookmark bm) {
    viewBookmark(app, bm);
  }
  
  public static void loadGraphsEventually(final SeqMapView gviewer, final Map props) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          loadGraphs(gviewer, props);
        } catch (Exception e) {
          ErrorHandler.errorPanel(gviewer.getFrame(), "ERROR", "Error while loading graphs", e);
        }
      }
    });
  }
  
  public static void loadGraphs(SeqMapView gviewer, Map map) {
    double default_ypos = 30;
    double default_yheight = 60;
    Color default_col = Color.lightGray;
    boolean default_float = true;
    boolean default_show_label = true;
    boolean default_show_axis = false;
    double default_minvis = Double.NEGATIVE_INFINITY;
    double default_maxvis = Double.POSITIVE_INFINITY;
    double default_score_thresh = 0;
    int default_minrun_thresh = 30;
    int default_maxgap_thresh = 100;
    boolean default_show_thresh = false;
    int default_thresh_direction = GraphStateI.THRESHOLD_DIRECTION_GREATER;
    Map combos = new HashMap();
    
    //System.err.println("I am in loadGraphs!!!!");
    
    // Figure out the "source_url" paths of all currently-loaded graphs
    java.util.List loaded_graphs = Collections.EMPTY_LIST;
    if (gviewer != null) {
      loaded_graphs = gviewer.collectGraphs();
    }
    Iterator iter = loaded_graphs.iterator();
    java.util.List loaded_graph_paths = new Vector(loaded_graphs.size());
    while (iter.hasNext()) {
      GraphGlyph gr = (GraphGlyph) iter.next();
      GraphSym graf_info = (GraphSym) gr.getInfo();
      String source_url = (String) graf_info.getProperty("source_url");
      loaded_graph_paths.add(source_url);
    }
    
    InputStream istr = null;
    try {
      //      while (map.get("graph"+i) != null) {
      for (int i=0; map.get("graph_source_url_"+i) != null; i++) {
        //        String graph_path = UnibrowControlServlet.getStringParameter(map, "graph" + i);
        String graph_path = UnibrowControlServlet.getStringParameter(map, "graph_source_url_" + i);
        
        // Don't load any graph we already have loaded
        if (loaded_graph_paths.contains(graph_path)) {
          continue;
        }
        
        // for some parameters, testing more than one parameter name because how some params used to have
        //    slightly different names, and we need to support legacy bookmarks
        String graph_name = UnibrowControlServlet.getStringParameter(map, "graph_name_" + i);
        if (DEBUG) {
          System.out.println("loading from bookmark, graph name: " + graph_name + ", url: " + graph_path);
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
          System.out.println("graph path: " + graph_path);
          System.out.println("red = " + col.getRed() +
              ", green = " + col.getGreen() + ", blue = " + col.getBlue());
          System.out.println("ypos = " + ypos);
        }
        if (DEBUG) {
          System.out.println(gmodel.getSelectedSeq());
          if (gviewer != null) System.out.println(gviewer.getSeqMap());
          System.out.println(col+", "+ypos+", "+ yheight
              +", "+use_floating_graphs+", "+show_label+", "+ show_axis
              +", "+minvis+", "+maxvis+", "
              + score_thresh+", "+maxgap_thresh+", "
              + show_thresh + ", " + thresh_direction);
        }
        
        if (graph_name == null || graph_name.trim().length()==0) {
          graph_name = graph_path;
        }
        
        if (Application.CACHE_GRAPHS)  {
          istr = LocalUrlCacher.getInputStream(graph_path);
        } else {
          URL graphurl = new URL(graph_path);
          istr = graphurl.openStream();
        }
        List grafs = GraphSymUtils.readGraphs(istr, graph_path, gmodel.getSelectedSeqGroup(), gmodel.getSelectedSeq());
        istr.close();
        //        displayGraph(graf, col, ypos, 60, true);
        //        GenericGraphGlyphFactory.displayGraph(graf, gmodel.getSelectedSeq(), gviewer.getSeqMap(),
        //                                     col, ypos, yheight, use_floating_graphs
        Integer graph_style_num = null;
        if (graph_style != null) {
          //	  graph_style_num = (Integer)gstyle2num.get(graph_style);
          graph_style_num = GraphState.getStyleNumber(graph_style);
        }
        if (grafs != null) {
          Iterator graf_iter = grafs.iterator();
          while (graf_iter.hasNext()) {
            GraphSym graf = (GraphSym) graf_iter.next();
            GraphStateI gstate = graf.getGraphState();
            graf.setGraphName(graph_name);
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

//            gviewer.getGenericGraphGlyphFactory().displayGraph(graf, gviewer, false);
          }
        }
      }
//      gviewer.getSeqMap().packTiers(false, true, false);
//      gviewer.getSeqMap().stretchToFit(false, false);
//      gviewer.getSeqMap().updateWidget(true);
      
      // Because of combo graphs, have to completely re-draw the display
      // Don't bother trying to preserve_view in y-direction.  It usually doesn't work well,
      // especially if the graphs are attached graphs.
      if (gviewer != null) {
        gviewer.setAnnotatedSeq(gviewer.getAnnotatedSeq(), true, true, false);
      }
      
    } catch (Exception ex) {
      ErrorHandler.errorPanel("ERROR", "Error while loading graphs", ex);
    } catch (Error er) {
      ErrorHandler.errorPanel("ERROR", "Error while loading graphs", er);
    } finally {
      if (istr != null) try {istr.close();} catch (IOException ioe) {}
    }
  }
  
  public static void addGraphProperties(SymWithProps mark_sym, java.util.List graphs) {
    if (DEBUG) {
      System.out.println("in addGraphProperties, graph count = " + graphs.size());
    }
    int max = graphs.size();
    Map combo_styles = new HashMap();
    
    // Holds a list of labels of graphs for which no url could be found.
    Set unfound_labels = new LinkedHashSet();
    
    // "j" loops throug all graphs, while "i" counts only the ones
    // that are actually book-markable (thus i <= j)
    int i = -1;
    for (int j=0; j<max; j++) {
      Object graph_object = graphs.get(j);
      if (!(graph_object instanceof SmartGraphGlyph)) {
        System.out.println("Cannot bookmark graphs that do not implement SmartGraphGlyph.");
        continue;
      }
      SmartGraphGlyph gr = (SmartGraphGlyph) graph_object;
      GraphSym graf = (GraphSym)gr.getInfo();
      if (DEBUG) {
        System.out.println("graph sym, points = " + graf.getPointCount() + ": " + graf);
      }
      String source_url = (String)graf.getProperty("source_url");
      if (source_url == null)  {
        String label = gr.getLabel();
        if (label != null && label.length() > 0) {
          unfound_labels.add(gr.getLabel());
        }
      } else {
        i++;
        Rectangle2D gbox = gr.getCoordBox();
        
        boolean is_floating = GraphGlyphUtils.hasFloatingAncestor(gr);
        mark_sym.setProperty("graph_source_url_" + i, source_url);
        mark_sym.setProperty("graph_ypos_" + i, Integer.toString((int)gbox.y));
        mark_sym.setProperty("graph_yheight_" + i, Integer.toString((int)gbox.height));
        mark_sym.setProperty("graph_col_" + i, sixDigitHex(gr.getGraphState().getTierStyle().getColor()));
        mark_sym.setProperty("graph_bg_" + i, sixDigitHex(gr.getGraphState().getTierStyle().getBackground()));
        if (is_floating) { mark_sym.setProperty("graph_float_" + i, "true"); } else  {mark_sym.setProperty("graph_float_" + i, "false"); }
        
        if (DEBUG) {
          System.out.println("setting bookmark prop graph_name_" + i + ": " + graf.getGraphName());
        }
        mark_sym.setProperty("graph_name_" + i, graf.getGraphName());
        mark_sym.setProperty("graph_show_label_" + i, (gr.getShowLabel()?"true":"false"));
        mark_sym.setProperty("graph_show_axis_" + i, (gr.getShowAxis()?"true":"false"));
        mark_sym.setProperty("graph_minvis_" + i, Double.toString(gr.getVisibleMinY()));
        mark_sym.setProperty("graph_maxvis_" + i, Double.toString(gr.getVisibleMaxY()));
        mark_sym.setProperty("graph_score_thresh_" + i, Double.toString(gr.getMinScoreThreshold()));
        mark_sym.setProperty("graph_maxgap_thresh_" + i, Integer.toString((int)gr.getMaxGapThreshold()));
        mark_sym.setProperty("graph_minrun_thresh_" + i, Integer.toString((int)gr.getMinRunThreshold()));
        mark_sym.setProperty("graph_show_thresh_" + i, (gr.getShowThreshold()?"true":"false"));
        mark_sym.setProperty("graph_style_" + i, (GraphState.getStyleName(gr.getGraphStyle())) );
        mark_sym.setProperty("graph_thresh_direction_" + i, Integer.toString(gr.getThresholdDirection()));
        if (gr.getGraphStyle() == SmartGraphGlyph.HEAT_MAP && gr.getGraphState().getHeatMap() != null) {
          mark_sym.setProperty("graph_heatmap_" + i, gr.getGraphState().getHeatMap().getName());
        }

        IAnnotStyle combo_style = gr.getGraphState().getComboStyle();
        if (combo_style != null) {
          Integer combo_style_num = (Integer) combo_styles.get(combo_style);
          if (combo_style_num == null) {
            combo_style_num = new Integer(combo_styles.size() + 1);
            combo_styles.put(combo_style, combo_style_num);
          }
          mark_sym.setProperty("graph_combo_"+i, combo_style_num.toString());
        }
        
        // if graphs are in tiers, need to deal with tier ordering in here somewhere!
        // (the graph_ypos variable can be used for this)
      }
    }

    // TODO: Now save the colors and such of the combo graphs!
    
    if (! unfound_labels.isEmpty()) {
      ErrorHandler.errorPanel("WARNING: Cannot bookmark some graphs",
          "Warning: could not bookmark some graphs.\n" +
          "No source URL was available for: " + unfound_labels.toString());
    }
    
  }
  
  /**
   *  Creates a Map containing bookmark properties.
   *  All keys and values are Strings.
   *  Assumes correct span is the first span in the sym.
   */
  public static Map constructBookmarkProperties(SeqSymmetry sym) {
    SeqSpan span = sym.getSpan(0);
    BioSeq seq = span.getBioSeq();
    Map props = new LinkedHashMap();
    props.put("seqid", seq.getID());
    if (seq instanceof NibbleBioSeq) {
      props.put("version", ((NibbleBioSeq)seq).getVersion());
    } else {
      props.put("version", "unknown");
    }
    props.put("start", Integer.toString(span.getMin()));
    props.put("end", Integer.toString(span.getMax()));
    return props;
  }
  
  static public Bookmark makeBookmark(Map props, String name) throws java.net.MalformedURLException {
    String url = Bookmark.constructURL(props);
    return new Bookmark(name, url);
  }
  
  
  /**
   *  Constructs a bookmark from a SeqSymmetry.
   *  Assumes correct span is the first span in the sym.
   *  Passes through to makeBookmark(Map props, String name).
   */
  static public Bookmark makeBookmark(SeqSymmetry sym, String name) throws java.net.MalformedURLException {
    Map props = constructBookmarkProperties(sym);
    return makeBookmark(props, name);
  }
  
  
  /** Returns a hexidecimal representation of the color with
   *  "0x" plus exactly 6 digits.  Example  Color.BLUE -> "0x0000FF".
   */
  static String sixDigitHex(Color c) {
    int i = c.getRGB() & 0xFFFFFF;
    String s = Integer.toHexString(i).toUpperCase();
    while (s.length() < 6) {
      s = "0"+s;
    }
    s = "0x"+s;
    return s;
  }
}
