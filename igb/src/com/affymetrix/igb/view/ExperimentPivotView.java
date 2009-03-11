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

package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.GraphSymFloat;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.glyph.*;
import com.affymetrix.genoviz.widget.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.SeqSymMinComparator;
import com.affymetrix.genometryImpl.IndexedSym;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.ScoredContainerSym;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.parsers.ScoredIntervalParser;
import com.affymetrix.genometryImpl.event.*;
import com.affymetrix.genometryImpl.style.HeatMap;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.tiers.*;
import com.affymetrix.igb.glyph.*;
import com.affymetrix.igb.util.UnibrowPrefsUtil;
import java.io.InputStream;
import java.io.StringBufferInputStream;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

public final class ExperimentPivotView extends JComponent
      implements SymSelectionListener, ActionListener, AnnotatedSeqViewer, SeqSelectionListener
{

  static String BLANK = "";

  // styles
  final static String LINE = "Line";
  final static String STAIRSTEP = "Bar";  // really stairstep for now...
  final static String HEATMAP1 = "Violet Heat Map";
  final static String HEATMAP2 = "Blue/Yellow Heat Map";
  final static String HEATMAP3 = "Red/Green Heat Map";
  final static String HEATMAP4 = "Blue/Yellow Heat Map2";

  /** Maps style strings to objects, sometimes Integer's, sometimes HeatMap's. */
  static Map string2style;

  // scalings
  final static String TOTAL_MIN_MAX = "Total Min/Max";
  final static String ROW_MIN_MAX = "Row Min/Max";
  final static String COLUMN_MIN_MAX = "Column Min/Max";

  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

  int numscores = 0;
  int score_spacing = 10;
  int graph_xoffset = 2;
  float overall_score_min = Float.POSITIVE_INFINITY;
  float overall_score_max = Float.NEGATIVE_INFINITY;
  HeatMap current_heatmap = HeatMap.getStandardHeatMap(HeatMap.HEATMAP_1);

  AffyTieredMultiMap map;
  TierLabelManager tier_manager;  
  AnnotatedBioSeq currentSeq;  // current annotated seq
  List experiment_graphs = new ArrayList();
  
  JComboBox styleCB = new JComboBox();
  JComboBox scaleCB = new JComboBox();
  
  Preferences pref_node = UnibrowPrefsUtil.getTopNode().node("Pivot View");
  
  static final String PREF_BG_COLOR = "Pivot View BG Color";
  static final String PREF_FG_COLOR = "Pivot View FG Color";
  static final String PREF_STYLE = "Pivot View Graph Style";
  static final String PREF_SCALE = "Pivot View Graph Scale";

  static final Color def_bg_color = Color.BLACK;
  static final Color def_fg_color = Color.YELLOW;
  static final String def_style = HEATMAP1;
  static final String def_scale = TOTAL_MIN_MAX;
  
  int experiment_style_int; // should correspond to def_style String value
  String experiment_scaling;
  
  List current_syms = Collections.EMPTY_LIST;
  
  static {
    string2style = new HashMap();
    string2style.put(LINE, new Integer(SmartGraphGlyph.LINE_GRAPH));
    string2style.put(STAIRSTEP, new Integer(SmartGraphGlyph.STAIRSTEP_GRAPH));

    string2style.put(HEATMAP1, HeatMap.getStandardHeatMap(HeatMap.HEATMAP_1));
    string2style.put(HEATMAP2, HeatMap.getStandardHeatMap(HeatMap.HEATMAP_2));
    string2style.put(HEATMAP4, HeatMap.getStandardHeatMap(HeatMap.HEATMAP_4));
    string2style.put(HEATMAP3, HeatMap.getStandardHeatMap(HeatMap.HEATMAP_3));
  }


  public ExperimentPivotView() {
    super();
    styleCB.addItem(HEATMAP1);
    styleCB.addItem(HEATMAP2);
    styleCB.addItem(HEATMAP4);
    styleCB.addItem(HEATMAP3);
    styleCB.addItem(STAIRSTEP);
    styleCB.addItem(LINE);
    if (styleCB.getItemCount() > 0) {
      styleCB.setSelectedItem(pref_node.get(PREF_STYLE, (String) styleCB.getItemAt(0)));
    }
    experiment_style_int = experimentStyleToInt((String) styleCB.getSelectedItem());
    
    Box style_pan = Box.createHorizontalBox();
    style_pan.add(new JLabel("Style:", JLabel.RIGHT));
    style_pan.add(Box.createHorizontalStrut(6));
    style_pan.add(styleCB);

    scaleCB.addItem(TOTAL_MIN_MAX);
    scaleCB.addItem(ROW_MIN_MAX);
//    scaleCB.addItem(COLUMN_MIN_MAX);    NOT YET IMPLEMENTED

    Box scale_pan = Box.createHorizontalBox();
    scale_pan.add(new JLabel("Scaling:", JLabel.RIGHT));
    scale_pan.add(Box.createHorizontalStrut(6));
    scale_pan.add(scaleCB);
    
    experiment_scaling = pref_node.get(PREF_SCALE, (String) scaleCB.getItemAt(0));
    if (scaleCB.getItemCount() > 0) {
      scaleCB.setSelectedItem(experiment_scaling);
    }

    JButton fg_button = UnibrowPrefsUtil.createColorButton("fg", pref_node, PREF_FG_COLOR, def_fg_color);
    JButton bg_button = UnibrowPrefsUtil.createColorButton("bg", pref_node, PREF_BG_COLOR, def_bg_color);
    PreferenceChangeListener pcl = new PreferenceChangeListener() {
      public void preferenceChange(PreferenceChangeEvent evt) {
        if (evt.getNode() == pref_node) {
          if (PREF_FG_COLOR.equals(evt.getKey()) || PREF_BG_COLOR.equals(evt.getKey())) {
            resetThisWidget(current_syms);
          }
        }
      }
    };
    pref_node.addPreferenceChangeListener(pcl);
    
    Box colors_pan = Box.createHorizontalBox();
    colors_pan.add(fg_button);
    colors_pan.add(Box.createHorizontalStrut(5));
    colors_pan.add(bg_button);

    JButton export_b = new JButton( new PivotViewExporter( this ) );
    Box optionsP = Box.createHorizontalBox();
    //optionsP.add(Box.createHorizontalGlue());
    optionsP.add(style_pan);
    optionsP.add(Box.createRigidArea(new Dimension(10,0)));
    optionsP.add(scale_pan);
    optionsP.add(Box.createRigidArea(new Dimension(10,0)));
    optionsP.add(export_b);
    optionsP.add(Box.createRigidArea(new Dimension(10,0)));
    optionsP.add(colors_pan);
    optionsP.add(Box.createHorizontalGlue());
    optionsP.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));    

    map = new AffyTieredMultiMap();
    //AffyTieredMap expmap = map.getExtraMap();
    //NeoMap labelmap = map.getLabelMap();
    //map.addScroller( map.VERTICAL, map.NORTH ); // for testing...
    map.addScroller( map.HORIZONTAL, map.EAST );
        
    tier_manager = new TierLabelManager(map);
    ExperimentPivotView.PivotViewPopup pvp = new ExperimentPivotView.PivotViewPopup();
    tier_manager.addPopupListener(pvp);

    this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    
    JPanel main_panel = new JPanel();
    main_panel.setLayout(new BorderLayout());
    main_panel.add("Center", map);
    main_panel.add("North", optionsP);
    main_panel.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));

    this.add(main_panel);
    
    styleCB.addActionListener(this);
    scaleCB.addActionListener(this);
    // setView(Unibrow.getSingletonUnibrow().getMapView());
    gmodel.addSeqSelectionListener(this);
    gmodel.addSymSelectionListener(this);
    
    Color pivot_bg = UnibrowPrefsUtil.getColor(pref_node, PREF_BG_COLOR, def_bg_color);
    map.setBackground(pivot_bg);    
  }
  
  int experimentStyleToInt(String selection) {
    int style = -1;
    Object obj = string2style.get(selection);
    if (obj instanceof Integer) {
      style = ((Integer)string2style.get(selection)).intValue();
    } else if (obj instanceof HeatMap) {
      style = GraphGlyph.HEAT_MAP;
    }
    return style;
  }
  
  void setExperimentStyle(String selection) {
    pref_node.put(PREF_STYLE, selection);
    int ee = experimentStyleToInt(selection);
    if (ee == GraphGlyph.HEAT_MAP) {
      setHeatMap((HeatMap) string2style.get(selection), false);
    }
    setExperimentStyle(ee, true);
  }
    
  void setExperimentStyle(int style, boolean update_widget) {
    experiment_style_int = style;
    int graph_count = experiment_graphs.size();
    for (int i=0; i<graph_count; i++) {
      GraphGlyph gl = (GraphGlyph)experiment_graphs.get(i);
      gl.setGraphStyle(experiment_style_int);
      Color pivot_fg = UnibrowPrefsUtil.getColor(pref_node, PREF_FG_COLOR, def_fg_color);
      gl.setColor(pivot_fg);
    }
    // need to adjust "extra map" insets (which in turn determine size of experiment graphs
    //    relative to size of extramap tiers), because for rendering graph as a heat map
    //    want the graph to fill the tier it's contained in, but for rendering graph as
    //    a line graph or bar graph then want some spacing between the graphs, so need
    //    graphs to be smaller than the tiers they are contained in
    if (experiment_style_int == GraphGlyph.HEAT_MAP) {
      map.setExtraMapInset(0);
    }
    else {
      map.setExtraMapInset(1);
    }
    if (update_widget) {
      map.repack();
      map.stretchToFit(false, true);
      map.updateWidget();
    }
  }

  /**
   * Set sequence without preserving selection or view.
   * @param seq to use.
   */
  public void setAnnotatedSeq(AnnotatedBioSeq seq) {
    setAnnotatedSeq(seq, false, false);
  }
  /**
   * Set the current sequence.
   * This is the context for the pivot view.
   * @param seq the new annotated sequence.
   * @param preserve_selection ignored.
   * @param preserve_view ignored.
   */
  public void setAnnotatedSeq(AnnotatedBioSeq seq,
			      boolean preserve_selection,
			      boolean preserve_view) {
    this.currentSeq = seq;
  }
  public AnnotatedBioSeq getAnnotatedSeq() {
    return this.currentSeq;
  }

//  private SeqMapView originalView = null;
//  private SeqMapView bigPicture;

//  /**
//   * Points to the big picture.
//   */
//  protected void setView(SeqMapView theView) {
//    assert null != theView;
//    this.originalView = theView;
//    this.bigPicture = new SeqMapView(false);
//    this.bigPicture.setFrame( this.originalView.getFrame() );
//    //    this.bigPicture.setGraphStateHash( this.originalView.getGraphStateHash() );
//    AffyTieredMap m = this.bigPicture.getSeqMap();
//    this.map.setNorthMap( m );
//    this.map.updateWidget();
//  }

  public void seqSelectionChanged(SeqSelectionEvent evt)  {
    if (Application.DEBUG_EVENTS)  {
      System.out.println("ExperimentPivotView received SeqSelectionEvent, selected seq: " + evt.getSelectedSeq());
    }
    AnnotatedBioSeq newseq = evt.getSelectedSeq();
    if (gmodel.getSelectedSeq() != newseq ) {
      throw new IllegalStateException( "Event source is not the same as genometry model selected sequence." );
    }
    setAnnotatedSeq(newseq);
  }
  
  private void resetThisWidget( List theSyms ) {
    String style_string = pref_node.get(PREF_STYLE, def_style);
    this.experiment_scaling = pref_node.get(PREF_SCALE, def_scale);
    
    if (this.currentSeq == null) {
      System.err.println("ERROR: ExperimentPivotView.resetThisWidget() called, "  +
			 "but no current annotated seq: " + this.currentSeq );
      current_syms = Collections.EMPTY_LIST;
      return;
    }
    this.current_syms = theSyms;
    int symcount = theSyms.size();
    map.clearWidget();
    // assume that all syms are on same seq...
    //      SeqSymmetry sym1 = (SeqSymmetry)syms.get(0);

    experiment_graphs = new ArrayList();
    map.setMapRange(0, this.currentSeq.getLength());

    ScoredContainerSym parent = null;  // holds IndexedSyms
    for (int i=0; i<symcount; i++) {
      SeqSymmetry sym = (SeqSymmetry)theSyms.get(i);
      if (sym instanceof IndexedSym) {
        IndexedSym isym = (IndexedSym)sym;
        numscores = isym.getParent().getScoreCount();
        parent = isym.getParent();
        if (numscores < 0) { numscores = 0; }
        break;
      }
    }

    Color pivot_bg = UnibrowPrefsUtil.getColor(pref_node, PREF_BG_COLOR, def_bg_color);
    Color pivot_fg = UnibrowPrefsUtil.getColor(pref_node, PREF_FG_COLOR, def_fg_color);
    map.setBackground(pivot_bg);

    //AffyTieredMap extramap = map.getExtraMap();
    //extramap.setMapRange(0, (numscores * score_spacing));
    map.setExtraMapRange(0, (numscores * score_spacing));
    VerticalGraphLabels extra_labels = new VerticalGraphLabels();
    extra_labels.setForegroundColor(pivot_fg);
    extra_labels.setBackgroundColor(pivot_bg);
    TierGlyph headerTier = new TierGlyph();
    headerTier.setBackgroundColor(pivot_bg);
    headerTier.setForegroundColor(pivot_fg);
    headerTier.setLabel( "score names" );
    headerTier.addChild( extra_labels );
    map.addNorthEastTier( headerTier );
    String[] scoreNames = new String[numscores];

    if (parent != null) {
      for (int i=0; i<numscores; i++) {
        scoreNames[i] = parent.getScoreName(i);
        extra_labels.addLabel(scoreNames[i]);
      }
      extra_labels.setInfo( scoreNames );
    }
    headerTier.setCoords( 0, 0, ( numscores * score_spacing /* + 1 */ ), 11 );
    extra_labels.setCoords( 0, 0, ( numscores * score_spacing ), 10 ); // does something, but what?
    
    overall_score_min = Float.POSITIVE_INFINITY;
    overall_score_max = Float.NEGATIVE_INFINITY;
    double xmin = Double.POSITIVE_INFINITY;
    double xmax = Double.NEGATIVE_INFINITY;

    for (int i=0; i<symcount; i++) {
      TierGlyph mtg = new TierGlyph();
      mtg.setFillColor(pivot_bg);
      mtg.setForegroundColor(pivot_fg);
      SeqSymmetry sym = (SeqSymmetry)theSyms.get(i);
      SeqSpan span = sym.getSpan(this.currentSeq);
      if ( null != span ) {
        xmin = Math.min(xmin, span.getMin());
        xmax = Math.max(xmax, span.getMax());
      }
      String id = null;
      if (sym instanceof SymWithProps) {
        id = (String)((SymWithProps)sym).getProperty("id");
      }
      if (id==null || id.length() == 0 && span != null) {
        id = SeqUtils.spanToString(span);
      }
      if (id != null) {
        mtg.setLabel(id);
      }
      map.addTier(mtg);
      GlyphI gl = addAnnotGlyph(sym, this.currentSeq, mtg, pivot_fg);
      GraphGlyph gr = addGraph(sym, mtg);
      if (gr != null) {
        overall_score_min = Math.min(overall_score_min, gr.getGraphMinY());
        overall_score_max = Math.max(overall_score_max, gr.getGraphMaxY());
      }
    }

    setExperimentStyle(style_string);
    setExperimentScaling(experiment_scaling, false);
    // This is done in clampToSpan: map.stretchToFit();
    //SeqSpan select_span = new SimpleSeqSpan((int)xmin, (int)xmax, this.currentSeq);
    int length = (int)(xmax - xmin);
    SeqSpan zoomto_span = new SimpleSeqSpan((int)(xmin - (0.02 * length)),
                                            (int)(xmax + (0.02 * length)), this.currentSeq);
    
    //AxisGlyph the_axis = map.addHeaderAxis();
    //the_axis.setForegroundColor(pivot_fg);
    //the_axis.setBackgroundColor(pivot_bg);
    
    zoomTo(zoomto_span);
    clampToSpan(zoomto_span);
    map.updateWidget();
  }

  public void symSelectionChanged( SymSelectionEvent theEvent ) {
    List syms = theEvent.getSelectedSyms();
    if ( ! syms.isEmpty() ) {
      resetThisWidget( syms );
    }
  }
  
  void setExperimentScaling(final String scaling, boolean update_widget) {
    experiment_scaling = scaling;
    pref_node.put(PREF_SCALE, experiment_scaling);
        
    int graph_count = experiment_graphs.size();
    if (TOTAL_MIN_MAX.equals(scaling)) {
      for (int i=0; i<graph_count; i++) {
        GraphGlyph gr = (GraphGlyph)experiment_graphs.get(i);
        gr.setVisibleMinY(overall_score_min);
        gr.setVisibleMaxY(overall_score_max);
      }
    } else if (ROW_MIN_MAX.equals(scaling)) {
      for (int i=0; i<graph_count; i++) {
        GraphGlyph gr = (GraphGlyph)experiment_graphs.get(i);
        gr.setVisibleMinY(gr.getGraphMinY());
        gr.setVisibleMaxY(gr.getGraphMaxY());
      }
    } else if (COLUMN_MIN_MAX.equals(scaling)) {
      // NOT YET IMPLEMENTED
    }
    if (update_widget) {
      map.updateWidget();
    }
  }

  public void setHeatMap(HeatMap heat_map, boolean update_widget) {
    int graph_count = experiment_graphs.size();
    for (int i=0; i<graph_count; i++) {
      GraphGlyph gr = (GraphGlyph)experiment_graphs.get(i);
      gr.setHeatMap(heat_map);
    }
    current_heatmap = heat_map;
    if (update_widget) {
      map.updateWidget();
    }
  }

  public GraphGlyph addGraph(SeqSymmetry sym, TierGlyph mtg) {
    GraphGlyph gl = null;
    if (sym instanceof IndexedSym) {
      IndexedSym isym = (IndexedSym)sym;

      AffyTieredMap extramap = map.getExtraMap();
      TierGlyph egl = (TierGlyph)extramap.getItem(mtg);
      egl.setFillColor(mtg.getFillColor());

      float[] ifloats = isym.getParent().getChildScores(isym);
      int point_count = ifloats.length;
      int[] xcoords = new int[point_count+1];
      float[] ycoords = new float[point_count+1];
      for (int i=0; i<point_count; i++) {
	xcoords[i] = i*score_spacing + graph_xoffset;
	ycoords[i] = ifloats[i];
      }
      xcoords[point_count] = point_count * score_spacing;
      ycoords[point_count] = 0;
      GraphSymFloat graf = new GraphSymFloat(xcoords, ycoords, null, null);
      
      // Each graph has its own, un-named, GraphState object
      // (so they can have different minima and maxima)
      gl = new GraphGlyph(graf, GraphState.getTemporaryGraphState());
      gl.setHeatMap(current_heatmap);
      gl.setGraphStyle(experiment_style_int);
      gl.setShowHandle(false);
      gl.setShowBounds(false);
      gl.setShowLabel(false);
      gl.setShowAxis(false);
      // gl.setPointCoords(xcoords, ycoords);
      gl.setColor(new Color(255, 0, 255));
      gl.setCoords(0, 0, 100, 100);
      gl.setInfo( ifloats );
      egl.addChild(gl);
      experiment_graphs.add(gl);
    }
    return gl;
  }

  GlyphI addAnnotGlyph(SeqSymmetry sym, BioSeq theSeq, GlyphI parent, Color c)  {
    int child_count = sym.getChildCount();
    SeqSpan span = sym.getSpan(theSeq);
    if (span == null)  { return null; }
    GlyphI gl = null;
    if (child_count <= 0) {
      gl = new FillRectGlyph();
      gl.setColor(c);
      gl.setCoords(span.getMin(), 0, span.getMax() - span.getMin(), 20);
      parent.addChild(gl);
    }
    else if (span != null) {
      gl = new EfficientLineContGlyph();
      gl.setColor(c);
      gl.setCoords(span.getMin(), 0, span.getMax() - span.getMin(), 20);
      parent.addChild(gl);
      for (int i=0; i<child_count; i++) {
	SeqSymmetry child = sym.getChild(i);
	addAnnotGlyph(child, theSeq, gl, c);
      }
    }
    return gl;
  }
  
  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    if (src == styleCB) {
      String selection = (String) styleCB.getSelectedItem();
      if (selection != BLANK) {
        setExperimentStyle(selection);
      }
    }
    else if (src == scaleCB) {
      String selection = (String) scaleCB.getSelectedItem();
      if (selection != BLANK) {
        setExperimentScaling(selection, true);
      }
    }
  }

  /**
   * Produce a table for export.
   * The table will have the following columns:
   * label, span start, span end,
   * and a column for each column of graph data (if any).
   * There will be one row for each visible tier, and the rows will be
   * in order based on the order of the tiers.  Hidden tiers are not included. 
   */
  public TableModel getTable() {

    AffyTieredMap atm = this.map.getNorthEastMap();
    TierGlyph tg = atm.getTier( "score names" );
    String[] scoreName;
    if (tg == null) {
      scoreName = new String[0];
      return new DefaultTableModel(0, 0);
    } else {
      GlyphI ggg = tg.getChild( 0 ); // assume only one.
      VerticalGraphLabels vgl = ( VerticalGraphLabels ) ggg;
      scoreName = ( String[] ) ggg.getInfo();
    }
    if (scoreName == null) {
      // even if there are no scores, you can still export the start and end data
      scoreName = new String[0];
    }

    Object[] cols = new String[3 + scoreName.length];
    cols[0] = "";
    cols[1] = "start";
    cols[2] = "end";
    for ( int i = 3; i < cols.length; i++ ) {
      cols[i] = scoreName[i-3];
    }

    Object[][] data;
    List tiers = this.map.getTiers();
    
    int non_hidden_tiers = 0;
    Iterator iter = map.getTiers().iterator();
    while (iter.hasNext()) {
      if (((TierGlyph) iter.next()).getState() != TierGlyph.HIDDEN) {
        non_hidden_tiers++;
      }
    }
    data = new Object[non_hidden_tiers][cols.length];
    
    // Note that the two iterators must be parallel.
    Iterator it = tiers.iterator();
    Iterator it2 = map.getExtraMap().getTiers().iterator();
    int i = -1;
    while ( it.hasNext() ) {
      if ( ! it2.hasNext() ) {
        throw new RuntimeException("Maps in Pivot View are out-of-sync");
      }
      TierGlyph tg1 = (TierGlyph) it.next();
      TierGlyph tg2 = (TierGlyph) it2.next();
      
      if (tg1.getState() != TierGlyph.HIDDEN) {
        i++;
      
        data[i][0] = tg1.getLabel();
        // Here we assume only one child glyph:
        GlyphI g = tg1.getChild( 0 );
        if ( null != g ) {
          Rectangle2D box = g.getCoordBox();
          data[i][1] = new Double( box.x );
          data[i][2] = new Double( box.x + box.width );
        }
        if ((tg2 != null) && (tg2.getChildCount() > 0))  {
          GlyphI g2 = tg2.getChild(0);
          if ( (null != g2) && (g2.getInfo() instanceof float[])) {
            float[] f = (float[]) g2.getInfo();
            for (int j = 0; j < f.length; j++) {
              data[i][j + 3] = new Float(f[j]);
            }
          }
        }
      }
    }
    AbstractTableModel answer = new DefaultTableModel( data, cols );
    return answer;
  }
  
  static final String test_data_sin1 = 
      "one\tseq1\t1000\t1020\t+\t100\t200\n"+
      "two\tseq1\t2000\t2020\t+\t200\t800\n"+
      "three\tseq1\t3000\t3020\t+\t300\t300\n"+
      "four\tseq1\t4000\t4020\t+\t400\t600\n"+
      "five\tseq1\t5000\t5020\t+\t500\t500\n"+
      "six\tseq1\t6000\t6020\t+\t600\t200\n";
  
  
  /**
   *  main for testing.
   */
  public static void main(String[] args) throws Exception {
    //String filename = "test_files/bed_01.bed";
    //InputStream istr = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);    
    
    InputStream istr = new StringBufferInputStream(test_data_sin1);

    SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
    AnnotatedSeqGroup seq_group = gmodel.addSeqGroup("Test Group");
    gmodel.setSelectedSeqGroup(seq_group);
    
    ScoredIntervalParser parser = new ScoredIntervalParser();
    parser.parse(istr, "foo.sin", seq_group);
    
    AnnotatedBioSeq seq = seq_group.getSeq(0);
    SeqSymmetry first_sym = seq.getAnnotation(0);
    List symlist = new ArrayList();
    for (int i=0; i<first_sym.getChildCount(); i++) {
      symlist.add(first_sym.getChild(i));
    }
    
    MutableAnnotatedBioSeq testseq = (MutableAnnotatedBioSeq) first_sym.getSpan(0).getBioSeq();

    JFrame frm = new JFrame( "ExperimentPivotView Test" );
    frm.setDefaultCloseOperation( frm.EXIT_ON_CLOSE );

    Container cpane = frm.getContentPane();
    //cpane.setLayout(new BorderLayout());
    ExperimentPivotView epview = new ExperimentPivotView();
    cpane.add(epview);

    frm.setSize(600, 400);
    frm.setVisible(true);

    Collections.sort(symlist, new SeqSymMinComparator(testseq, true));
    
    gmodel.setSelectedSeq( testseq, epview );
    gmodel.setSelectedSymmetries(symlist, "");    
  }


  public void clampToSpan(SeqSpan span) {
    map.setMapRange((int)span.getMin(), (int)span.getMax());
    map.stretchToFit(true, true);
    map.updateWidget();
  }


  public void zoomTo(SeqSpan span) {
    int smin = span.getMin();
    int smax = span.getMax();
    float coord_width = smax - smin;
    float pixel_width = map.getView().getPixelBox().width;
    float pixels_per_coord = pixel_width / coord_width;
    map.zoom(NeoWidgetI.X, pixels_per_coord);
    map.scroll(NeoWidgetI.X, smin);
    map.setZoomBehavior(map.X, map.CONSTRAIN_COORD, (smin + smax)/2);
    map.updateWidget();
  }
  
  class PivotViewPopup implements TierLabelManager.PopupListener {

    TierLabelManager tlh = null;
    
    Action hide_action = new AbstractAction("Hide") {
      public void actionPerformed(ActionEvent e) {
        hideSelectedTiers();
      }
    };
    
    Action show_all_action = new AbstractAction("Show All") {
      public void actionPerformed(ActionEvent e) {
        showAllTiers();
      }
    };
    
    Action sort_action = new AbstractAction("Sort") {
      public void actionPerformed(ActionEvent e) {
        sortTiers();
      }
    };

    PivotViewPopup() {
    }
    
    void hideSelectedTiers() {
      tlh.hideTiers(tlh.getSelectedTierLabels(), false, false);
      tlh = null;
    }
    
    void showAllTiers() {
      tlh.showTiers(tlh.getAllTierLabels(), false, true);
      tlh = null;
    }
    
    void sortTiers() {
      List all_tiers = tlh.getAllTierLabels();
      Collections.sort(all_tiers, the_comparator);
      tlh.orderTiersByLabels(all_tiers);
      tlh.repackTheTiers(false, true);
      tlh = null;
    }
    
    public void popupNotify(JPopupMenu popup, TierLabelManager handler) {
      this.tlh = handler;
      if (tlh == null) {
        return;
      }
      
      List selected_labels = handler.getSelectedTierLabels();
      List all_labels = handler.getAllTierLabels();
      hide_action.setEnabled(! selected_labels.isEmpty());
      show_all_action.setEnabled( ! all_labels.isEmpty() );
      sort_action.setEnabled( all_labels.size() > 1 );
      
      if (popup.getComponentCount() > 0) {
        popup.add(new JSeparator());
      }
      popup.add(hide_action);
      popup.add(show_all_action);
      popup.add(sort_action);
    }

    // A comparator for sorting based on the x-position of the first glyph in a tier
    Comparator the_comparator = new Comparator() {
      public int compare(Object obj1, Object obj2) {
        TierGlyph tg1 = ((TierLabelGlyph) obj1).getReferenceTier();
        TierGlyph tg2 = ((TierLabelGlyph) obj2).getReferenceTier();
        
        if (tg1.getChildCount() == 0 || tg2.getChildCount() == 0) {
          // No tier should be empty, but if so sort by number of items in tiers
          if (tg1.getChildCount() == tg2.getChildCount()) return 0;
          else return (tg1.getChildCount() < tg2.getChildCount() ? -1 : 1);
        }
                
        Rectangle2D box1 = tg1.getChild(0).getCoordBox();
        Rectangle2D box2 = tg2.getChild(0).getCoordBox();        
        if (box1.x < box2.x) { return -1; }
        else if (box1.x > box2.x) { return 1; }
        else { return 0; }
      }
    };
    
  }

}
