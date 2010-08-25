/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
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

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericVersion;
import java.awt.Color;
import java.util.List;
import java.io.*;
import com.affymetrix.genometryImpl.style.DefaultTrackStyle;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.style.HeatMap;
import com.affymetrix.genometryImpl.style.ITrackStyle;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.glyph.GraphGlyph;
import com.affymetrix.igb.util.GraphGlyphUtils;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.igb.bookmarks.Bookmark.GRAPH;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.geom.Rectangle2D;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.SwingUtilities;

/**
 *  Allows creation of bookmarks based on a SeqSymmetry, and viewing of
 *  a bookmark.
 *
 * @version $Id$
 */
public abstract class BookmarkController {
  private final static boolean DEBUG= false;

  /** Causes a bookmark to be executed.  If this is a Unibrow bookmark,
   *  it will be opened in the viewer using
   *  {@link UnibrowControlServlet#goToBookmark}.  If it is an external
   *  bookmark, it will be opened in an external browser.
   */
  public static void viewBookmark(Application app, Bookmark bm) {
    if (bm.isUnibrowControl()) {
      if (DEBUG) System.out.println("****** Viewing internal control bookmark: "+bm.getURL().toExternalForm());
      try {
        Map<String, String[]> props = Bookmark.parseParameters(bm.getURL());
        UnibrowControlServlet.goToBookmark(app, props);
      } catch (Exception e) {
        String message = e.getClass().getName() + ": " + e.getMessage();
        ErrorHandler.errorPanel("Error opening bookmark.\n" + message);
      }
    } else {
      if (DEBUG) System.out.println("****** Viewing external bookmark: "+bm.getURL().toExternalForm());
      GeneralUtils.browse(bm.getURL().toExternalForm());
    }
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

  private static void loadGraphs(SeqMapView gviewer, Map map) {
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
    int default_thresh_direction = GraphState.THRESHOLD_DIRECTION_GREATER;
    Map<String,ITrackStyle> combos = new HashMap<String,ITrackStyle>();

    // Figure out the "source_url" paths of all currently-loaded graphs
    List<GlyphI> loaded_graphs = Collections.<GlyphI>emptyList();
    if (gviewer != null) {
      loaded_graphs = gviewer.collectGraphs();
    }
    Iterator<GlyphI> iter = loaded_graphs.iterator();
    List<String> loaded_graph_paths = new ArrayList<String>(loaded_graphs.size());
    while (iter.hasNext()) {
      GraphGlyph gr = (GraphGlyph) iter.next();
      GraphSym graf_info = (GraphSym) gr.getInfo();
      String source_url = (String) graf_info.getProperty("source_url");
      loaded_graph_paths.add(source_url);
    }

	GenometryModel gmodel = GenometryModel.getGenometryModel();
    InputStream istr = null;
    try {
      for (int i=0; map.get(GRAPH.SOURCE_URL.toString()+i) != null; i++) {
        String graph_path = UnibrowControlServlet.getStringParameter(map, GRAPH.SOURCE_URL.toString() + i);

        // Don't load any graph we already have loaded
        if (loaded_graph_paths.contains(graph_path)) {
          continue;
        }

        // for some parameters, testing more than one parameter name because how some params used to have
        //    slightly different names, and we need to support legacy bookmarks
        String graph_name = UnibrowControlServlet.getStringParameter(map, GRAPH.NAME.toString() + i);
        if (DEBUG) {
          System.out.println("loading from bookmark, graph name: " + graph_name + ", url: " + graph_path);
        }
        String graph_ypos = UnibrowControlServlet.getStringParameter(map, GRAPH.YPOS.toString() + i);
        if (graph_ypos == null)  { graph_ypos = UnibrowControlServlet.getStringParameter(map, "graphypos" + i); }

        String graph_height = UnibrowControlServlet.getStringParameter(map, GRAPH.YHEIGHT.toString() + i);
        if (graph_height == null) { graph_height = UnibrowControlServlet.getStringParameter(map, "graphyheight" + i); }
        // graph_col is String rep of RGB integer
        String graph_col = UnibrowControlServlet.getStringParameter(map, GRAPH.COL.toString() + i);
        if (graph_col == null)  { graph_col = UnibrowControlServlet.getStringParameter(map, "graphcol" + i); }

        String graph_bg_col = UnibrowControlServlet.getStringParameter(map, GRAPH.BG.toString() + i);
        // graph_bg_col will often be null

        String graph_float = UnibrowControlServlet.getStringParameter(map, GRAPH.FLOAT.toString() + i);
        if (graph_float == null)  { graph_float = UnibrowControlServlet.getStringParameter(map, "graphfloat" + i); }

        String show_labelstr = UnibrowControlServlet.getStringParameter(map, GRAPH.SHOW_LABEL.toString() + i);
        String show_axisstr = UnibrowControlServlet.getStringParameter(map, GRAPH.SHOW_AXIS.toString() + i);
        String minvis_str = UnibrowControlServlet.getStringParameter(map, GRAPH.MINVIS.toString() + i);
        String maxvis_str = UnibrowControlServlet.getStringParameter(map, GRAPH.MINVIS.toString() + i);
        String score_threshstr = UnibrowControlServlet.getStringParameter(map, GRAPH.SCORE_THRESH.toString() + i);
        String maxgap_threshstr = UnibrowControlServlet.getStringParameter(map, GRAPH.MAXGAP_THRESH.toString() + i);
        String minrun_threshstr = UnibrowControlServlet.getStringParameter(map, GRAPH.MINRUN_THRESH.toString() + i);
        String show_threshstr = UnibrowControlServlet.getStringParameter(map, GRAPH.SHOW_THRESH.toString() + i);
        String thresh_directionstr = UnibrowControlServlet.getStringParameter(map, GRAPH.THRESH_DIRECTION.toString() + i);

        //        int graph_min = (graph_visible_min == null) ?
        String graph_style = UnibrowControlServlet.getStringParameter(map, GRAPH.STYLE.toString() + i);
        String heatmap_name = UnibrowControlServlet.getStringParameter(map, GRAPH.HEATMAP.toString() + i);

        String combo_name = UnibrowControlServlet.getStringParameter(map, GRAPH.COMBO.toString() + i);

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

		  istr = LocalUrlCacher.getInputStream(graph_path);

		  List<GraphSym> grafs = GraphSymUtils.readGraphs(istr, graph_path, gmodel, gmodel.getSelectedSeqGroup(), gmodel.getSelectedSeq());
		  GraphSymUtils.processGraphSyms(grafs, graph_path);

		  GraphType graph_style_num = null;
		  if (graph_style != null) {
			  graph_style_num = GraphState.getStyleNumber(graph_style);
		  }
		  if (grafs != null) {
			  loopOverGraphs(grafs, graph_name, graph_style_num, heatmap_name, col, bg_col, ypos, yheight, use_floating_graphs, show_label, show_axis, minvis, maxvis, score_thresh, minrun_thresh, maxgap_thresh, show_thresh, thresh_direction, combo_name, combos);
		  }
		}

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
		GeneralUtils.safeClose(istr);
    }
  }

  private static void loopOverGraphs(List<GraphSym> grafs, String graph_name, GraphType graph_style_num, String heatmap_name, Color col, Color bg_col, double ypos, double yheight, boolean use_floating_graphs, boolean show_label, boolean show_axis, double minvis, double maxvis, double score_thresh, int minrun_thresh, int maxgap_thresh, boolean show_thresh, int thresh_direction, String combo_name, Map<String, ITrackStyle> combos) {
		for (GraphSym graf : grafs) {
			GraphState gstate = graf.getGraphState();
			graf.setGraphName(graph_name);
			if (graph_style_num != null) {
				gstate.setGraphStyle(graph_style_num);
			}
			if (heatmap_name != null) {
				HeatMap heat_map = HeatMap.getStandardHeatMap(heatmap_name);
				if (heat_map != null) {
					gstate.setHeatMap(heat_map);
				}
			}
			ITrackStyle tier_style = gstate.getTierStyle();
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
				ITrackStyle combo_style = combos.get(combo_name);
				if (combo_style == null) {
					combo_style = new DefaultTrackStyle("Joined Graphs", true);
					combo_style.setHumanName("Joined Graphs");
					combo_style.setExpandable(true);
					combo_style.setCollapsed(true);
					combos.put(combo_name, combo_style);
				}
				gstate.setComboStyle(combo_style);
			}
		}
	}

  public static void addSymmetries(Bookmarks bookmark){
	BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();
	AnnotatedSeqGroup  group = seq.getSeqGroup();

	for(GenericVersion version : group.getEnabledVersions()){
			for( GenericFeature feature : version.getFeatures()){
				if(feature.loadStrategy != LoadStrategy.NO_LOAD){
					bookmark.add(feature);
				}
			}
		}
  }

  public static void addGraphProperties(SymWithProps mark_sym, List<GlyphI> graphs, Bookmarks bookmark) {
    if (DEBUG) {
      System.out.println("in addGraphProperties, graph count = " + graphs.size());
    }
    int max = graphs.size();
    Map<ITrackStyle,Integer> combo_styles = new HashMap<ITrackStyle,Integer>();

    // Holds a list of labels of graphs for which no url could be found.
    Set<String> unfound_labels = new LinkedHashSet<String>();

    // "j" loops throug all graphs, while "i" counts only the ones
    // that are actually book-markable (thus i <= j)
    int i = -1;
    
    for (int j=0; j<max; j++) {
      GlyphI graph_object = graphs.get(j);
      if (!(graph_object instanceof GraphGlyph)) {
        System.out.println("Cannot bookmark graphs that do not implement GraphGlyph.");
        continue;
      }
      GraphGlyph gr = (GraphGlyph) graph_object;
      GraphSym graph = (GraphSym)gr.getInfo();
      
      if (DEBUG) {
        System.out.println("graph sym, points = " + graph.getPointCount() + ": " + graph);
      }
      String source_url = (String)graph.getProperty("source_url");
      bookmark.addGraph(graph);
      source_url = bookmark.getSource();
      if (source_url == null)  {
        String label = gr.getLabel();
        if (label != null && label.length() > 0) {
          unfound_labels.add(gr.getLabel());
        }
      } else {
        i++;
        Rectangle2D.Double gbox = gr.getCoordBox();

        boolean is_floating = GraphGlyphUtils.hasFloatingAncestor(gr);
        if(!bookmark.isValid()){
            mark_sym.setProperty(GRAPH.SOURCE_URL.toString() + i, source_url);
        }
        mark_sym.setProperty(GRAPH.YPOS.toString() + i, Integer.toString((int)gbox.y));
        mark_sym.setProperty(GRAPH.YHEIGHT.toString() + i, Integer.toString((int)gbox.height));
        mark_sym.setProperty(GRAPH.COL.toString() + i, sixDigitHex(gr.getGraphState().getTierStyle().getColor()));
        mark_sym.setProperty(GRAPH.BG.toString() + i, sixDigitHex(gr.getGraphState().getTierStyle().getBackground()));
        if (is_floating) { mark_sym.setProperty(GRAPH.FLOAT.toString() + i, "true"); } else  {mark_sym.setProperty(GRAPH.FLOAT.toString() + i, "false"); }

        if (DEBUG) {
          System.out.println("setting bookmark prop graph_name_" + i + ": " + graph.getGraphName());
        }
        mark_sym.setProperty(GRAPH.NAME.toString() + i, graph.getGraphName());
        mark_sym.setProperty(GRAPH.SHOW_LABEL.toString() + i, (gr.getShowLabel()?"true":"false"));
        mark_sym.setProperty(GRAPH.SHOW_AXIS.toString() + i, (gr.getShowAxis()?"true":"false"));
        mark_sym.setProperty(GRAPH.MINVIS.toString() + i, Double.toString(gr.getVisibleMinY()));
        mark_sym.setProperty(GRAPH.MAXVIS.toString() + i, Double.toString(gr.getVisibleMaxY()));
        mark_sym.setProperty(GRAPH.SCORE_THRESH.toString() + i, Double.toString(gr.getMinScoreThreshold()));
        mark_sym.setProperty(GRAPH.MAXGAP_THRESH.toString() + i, Integer.toString((int)gr.getMaxGapThreshold()));
        mark_sym.setProperty(GRAPH.MINRUN_THRESH.toString() + i, Integer.toString((int)gr.getMinRunThreshold()));
        mark_sym.setProperty(GRAPH.SHOW_THRESH.toString() + i, (gr.getShowThreshold()?"true":"false"));
        mark_sym.setProperty(GRAPH.STYLE.toString() + i, gr.getGraphStyle().name().toLowerCase());
        mark_sym.setProperty(GRAPH.THRESH_DIRECTION.toString() + i, Integer.toString(gr.getThresholdDirection()));
        if (gr.getGraphStyle() == GraphType.HEAT_MAP && gr.getGraphState().getHeatMap() != null) {
          mark_sym.setProperty(GRAPH.HEATMAP.toString() + i, gr.getGraphState().getHeatMap().getName());
        }

        ITrackStyle combo_style = gr.getGraphState().getComboStyle();
        if (combo_style != null) {
          Integer combo_style_num = combo_styles.get(combo_style);
          if (combo_style_num == null) {
            combo_style_num = new Integer(combo_styles.size() + 1);
            combo_styles.put(combo_style, combo_style_num);
          }
          mark_sym.setProperty(GRAPH.COMBO.toString()+i, combo_style_num.toString());
        }
        
       
        // if graphs are in tiers, need to deal with tier ordering in here somewhere!
        // (the graph_ypos variable can be used for this)
      }
    }
   // bookmark.set(mark_sym);
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
   *
   * @param sym The symmetry to generate the bookmark properties from
   * @return a map of bookmark properties
   */
  private static Map<String, String[]> constructBookmarkProperties(SeqSymmetry sym) {
    SeqSpan span = sym.getSpan(0);
    BioSeq seq = span.getBioSeq();
    Map<String,String[]> props = new LinkedHashMap<String,String[]>();
    props.put(Bookmark.SEQID, new String[] {seq.getID()});
	props.put(Bookmark.VERSION, new String[] {seq.getVersion()});
    props.put(Bookmark.START, new String[] {Integer.toString(span.getMin())});
    props.put(Bookmark.END, new String[] {Integer.toString(span.getMax())});
    return props;
  }

  /**
   *  Constructs a bookmark from a SeqSymmetry.
   *  Assumes correct span is the first span in the sym.
   *  Passes through to makeBookmark(Map props, String name).
   *
   * @param sym The symmetry to bookmark
   * @param name name of the bookmark
   * @return the bookmark for the symmetry
   * @throws MalformedURLException if the URL specifies an unknown protocol
   */
  public static Bookmark makeBookmark(SeqSymmetry sym, String name) throws MalformedURLException {
    Map<String, String[]> props = constructBookmarkProperties(sym);
    String url = Bookmark.constructURL(props);
    return new Bookmark(name, url);
  }


  /** Returns a hexidecimal representation of the color with
   *  "0x" plus exactly 6 digits.  Example  Color.BLUE -> "0x0000FF".
   */
  private static String sixDigitHex(Color c) {
    int i = c.getRGB() & 0xFFFFFF;
    String s = Integer.toHexString(i).toUpperCase();
    while (s.length() < 6) {
      s = "0"+s;
    }
    s = "0x"+s;
    return s;
  }
}
