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

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;
import javax.swing.border.*;
import java.util.*;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.widget.*;
import com.affymetrix.genoviz.util.Timer;
import com.affymetrix.genometry.AnnotatedBioSeq;
import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.igb.menuitem.FileTracker;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.event.*;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.glyph.GraphGlyph;
import com.affymetrix.igb.glyph.GraphScoreThreshSetter;
import com.affymetrix.igb.glyph.GraphState;
import com.affymetrix.igb.glyph.GraphVisibleBoundsSetter;
import com.affymetrix.igb.glyph.SmartGraphGlyph;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.tiers.AnnotStyle;
import com.affymetrix.igb.tiers.TierGlyph;
import com.affymetrix.igb.util.FloatTransformer;
import com.affymetrix.igb.util.GraphGlyphUtils;
import com.affymetrix.igb.util.GraphSymUtils;

public class GraphAdjusterView extends JComponent
     implements SymSelectionListener, ActionListener, SeqSelectionListener  {
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  static boolean TEST_GRAPH_TRANSFORM = true;
  static boolean TEST_GRAPH_EVAL = false;
  static String BLANK = "";
  static String MINMAXAVG = "MinMaxAvg";
  static String LINE = "Line";
  static String BAR = "Bar";
  static String DOT = "Dot";
  static String STAIRSTEP = "StairStep";
  static String INTERVAL = "Interval";
  static String HIDE = "Hide";
  static String HEATMAP = "Heat Map";

  static Map string2style;
  static FileTracker load_dir_tracker = FileTracker.DATA_DIR_TRACKER;

  GraphVisibleBoundsSetter vis_bounds_adjuster;
  JTextField transformTF = new JTextField(20);
  GraphScoreThreshSetter score_thresh_adjuster;

  NeoWidgetI nwidg = null;
  SeqMapView gviewer = null;
  AnnotatedBioSeq current_seq = null;

  // settings to apply to currently selected graphs
  JCheckBox labelCB = new JCheckBox("Label");
  JCheckBox yaxisCB = new JCheckBox("Y Axis");
  JCheckBox handleCB = new JCheckBox("Handle");
  JCheckBox boundsCB = new JCheckBox("Bounds");

  JButton colorB = new JButton("Change Color");
  JComboBox styleCB = new JComboBox();

  JButton boundsB = new JButton("Toggle Bounds");
  JRadioButton floatB = new JRadioButton("Floating");
  JRadioButton attachB = new JRadioButton("Attached");
  JRadioButton not_attached_or_floatingB = new JRadioButton("Neither"); // hidden button
  JButton cloneB = new JButton("Clone Graphs");
  JButton deleteB = new JButton("Delete Graph");
  JButton saveB = new JButton("Save Graph");
  JButton selectAllB = new JButton("Select All Graphs");
  JButton setDefaultsB = new JButton("Set Defaults");
  JButton groupGraphsB = new JButton("Group Graphs");
  ButtonGroup pgroup = new ButtonGroup();

  JComboBox scaleCB = new JComboBox();

  static String LOG_10 = "Log10";
  static String LOG_2 = "Log2";
  static String LOG_NATURAL = "Natural Log";
  static String INVERSE_LOG_10 = "Inverse Log10";
  static String INVERSE_LOG_2 = "Inverse Log2";
  static String INVERSE_LOG_NATURAL = "Inverse Natural Log";
  static Map name2transform;

  int tf_max_ypix = 25;
  int max_xpix_per_char = 6;

  java.util.List grafs = new ArrayList();
  java.util.List glyphs = new ArrayList();

  static {
    string2style = new HashMap();
    string2style.put(MINMAXAVG, new Integer(SmartGraphGlyph.MINMAXAVG));
    string2style.put(LINE, new Integer(SmartGraphGlyph.LINE_GRAPH));
    string2style.put(BAR, new Integer(SmartGraphGlyph.BAR_GRAPH));
    string2style.put(DOT, new Integer(SmartGraphGlyph.DOT_GRAPH));
    string2style.put(STAIRSTEP, new Integer(SmartGraphGlyph.STAIRSTEP_GRAPH));
    string2style.put(INTERVAL, new Integer(SmartGraphGlyph.SPAN_GRAPH));
    string2style.put(HEATMAP, new Integer(SmartGraphGlyph.HEAT_MAP));

    name2transform = new LinkedHashMap();
    name2transform.put(LOG_10, new LogTransform(10));
    name2transform.put(LOG_2, new LogTransform(2));
    name2transform.put(LOG_NATURAL, new LogTransform(Math.E));
    name2transform.put(INVERSE_LOG_10, new InverseLogTransform(10));
    name2transform.put(INVERSE_LOG_2, new InverseLogTransform(2));
    name2transform.put(INVERSE_LOG_NATURAL, new InverseLogTransform(Math.E));
  }


  public GraphAdjusterView() {
    this(IGB.getSingletonIGB().getMapView());
  }

  public GraphAdjusterView(SeqMapView gviewer) {
    super();

    //  scaleCB.addItem(BLANK);
    Iterator iter = name2transform.keySet().iterator();
    while (iter.hasNext()) {
      String name = (String)iter.next();
      scaleCB.addItem(name);
    }

    this.gviewer = gviewer;
    nwidg = gviewer.getSeqMap();
    vis_bounds_adjuster = new GraphVisibleBoundsSetter(nwidg);
    score_thresh_adjuster = new GraphScoreThreshSetter(gviewer, vis_bounds_adjuster);

    score_thresh_adjuster.setBorder(new TitledBorder("Thresholding"));
    vis_bounds_adjuster.setBorder(new TitledBorder("Visible Bounds"));

    JPanel style_pan = new JPanel();
    style_pan.setLayout(new BoxLayout(style_pan, BoxLayout.X_AXIS));
    style_pan.add(new JLabel("Style"));
    style_pan.add(Box.createHorizontalStrut(10));
    style_pan.add(styleCB);
    styleCB.addItem(BLANK);
    styleCB.addItem(MINMAXAVG);
    styleCB.addItem(LINE);
    styleCB.addItem(BAR);
    styleCB.addItem(DOT);
    styleCB.addItem(STAIRSTEP);
    styleCB.addItem(HEATMAP);
    // styleCB.addItem(INTERVAL);

    JPanel decorP = new JPanel();
    //decorP.setBorder(new TitledBorder("Decorations"));
    decorP.setBorder(new EtchedBorder());
    decorP.setLayout(new GridLayout(1, 3));
    decorP.add(new JLabel("Decorations: "));
    decorP.add(labelCB);
    decorP.add(yaxisCB);
    //    decorP.add(boundsCB);
    //    decorP.add(handleCB);

    JPanel placementP = new JPanel();
    placementP.setLayout(new GridLayout(1, 3));
    placementP.setBorder(new EtchedBorder());
    placementP.add(new JLabel("Placement: "));
    placementP.add(attachB);
    placementP.add(floatB);
    pgroup = new ButtonGroup();
    pgroup.add(attachB);
    pgroup.add(floatB);
    pgroup.add(not_attached_or_floatingB);
    
    JPanel save_deleteP = new JPanel();
    save_deleteP.setBorder(new EmptyBorder(3,3,3,3));
    save_deleteP.setLayout(new BoxLayout(save_deleteP, BoxLayout.X_AXIS));
    save_deleteP.add(saveB);
    save_deleteP.add(Box.createHorizontalStrut(10));
    save_deleteP.add(deleteB);

    JPanel colorP = new JPanel();
    colorP.setLayout(new BoxLayout(colorP, BoxLayout.X_AXIS));
    colorP.setBorder(new EmptyBorder(3,3,3,3));
    colorP.add(cloneB);
    colorP.add(Box.createHorizontalStrut(10));
    colorP.add(colorB);

    JPanel button_row_1 = new JPanel();
    button_row_1.setBorder(new EmptyBorder(3,3,3,3));
    button_row_1.setLayout(new BoxLayout(button_row_1, BoxLayout.X_AXIS));
    button_row_1.add(selectAllB);
    button_row_1.add(Box.createHorizontalStrut(10));
    button_row_1.add(setDefaultsB);
    
    //JPanel defaults_pan = new JPanel();
    JPanel options_pan = new JPanel();

    //    options_pan.setBorder(new TitledBorder("Options"));
    options_pan.setLayout(new BoxLayout(options_pan, BoxLayout.Y_AXIS));
    //options_pan.add(setDefaultsB);
    //options_pan.add(selectAllB);
    options_pan.add(button_row_1);
    options_pan.add(placementP);
    options_pan.add(decorP);
    options_pan.add(colorP);
    options_pan.add(style_pan);
    options_pan.add(save_deleteP);

    /*
    //    JPanel options_holder = new JPanel();
    JTabbedPane options_holder = new JTabbedPane();
    //    options_holder.add(options_pan);
    options_holder.addTab("Selection", null, options_pan, null);
    //options_holder.addTab("Defaults", null, defaults_pan, null);
    options_holder.setBorder(new TitledBorder("Options"));
    */
    options_pan.setBorder(new TitledBorder("Options"));

    /*
    JPanel defpan2 = new JPanel();
    defpan2.setLayout(new GridLayout(1, 3));
    JCheckBox use_floating_cbox = UnibrowPrefsUtil.createCheckBox("Float by default", GraphGlyphUtils.getGraphPrefsNode(),
      GraphGlyphUtils.PREF_USE_FLOATING_GRAPHS, GraphGlyphUtils.default_use_floating_graphs);
    defpan2.add(use_floating_cbox);

    defaults_pan.setLayout(new BoxLayout(defaults_pan, BoxLayout.Y_AXIS));

    JPanel defpan3 = new JPanel(new GridLayout(1, 2));
    defpan3.add(new JLabel("If floating, pixel height: "));
    JTextField def_pix_heightTF = UnibrowPrefsUtil.createNumberTextField(
      GraphGlyphUtils.getGraphPrefsNode(), GraphGlyphUtils.PREF_FLOATING_PIXEL_HEIGHT, Integer.toString(GraphGlyphUtils.default_pix_height), Integer.class);
    defpan3.add(def_pix_heightTF);

    JPanel defpan4 = new JPanel(new GridLayout(1, 2));
    defpan4.add(new JLabel("If tiered, coord height: "));
    JTextField def_coord_heightTF = UnibrowPrefsUtil.createNumberTextField(
      GraphGlyphUtils.getGraphPrefsNode(), GraphGlyphUtils.PREF_ATTACHED_COORD_HEIGHT, Integer.toString(GraphGlyphUtils.default_coord_height), Integer.class);
    defpan4.add(def_coord_heightTF);

    JPanel defpan5 = new JPanel(new GridLayout(1, 2));
    defpan5.add(new JLabel("Floating to Tiered, Use: "));
    String[] combo_options = new String[] {GraphGlyphUtils.USE_CURRENT_HEIGHT, GraphGlyphUtils.USE_DEFAULT_HEIGHT};
    JComboBox float2attachCB = UnibrowPrefsUtil.createComboBox(
      GraphGlyphUtils.getGraphPrefsNode(), GraphGlyphUtils.PREF_ATTACH_HEIGHT_MODE, combo_options, GraphGlyphUtils.default_attach_mode);
    defpan5.add(float2attachCB);

    defaults_pan.add(defpan2);
    defaults_pan.add(defpan3);
    defaults_pan.add(defpan4);
    defaults_pan.add(defpan5);
    */


    JPanel vis_adjusterP = new JPanel();
    vis_adjusterP.setLayout(new BorderLayout());
    vis_adjusterP.add("Center", vis_bounds_adjuster);

    if (TEST_GRAPH_TRANSFORM) {
      JPanel transformP = new JPanel();
      transformP.setLayout(new BoxLayout(transformP, BoxLayout.Y_AXIS));
      if (TEST_GRAPH_EVAL)  {
        transformP.add(transformTF);
      }
      JPanel scaleP = new JPanel();
      scaleP.setLayout(new GridLayout(1, 2));
      scaleP.add(new JLabel("Scale Graph: "));
      scaleP.add(scaleCB);
      transformP.add(scaleP);
      vis_adjusterP.add("South", transformP);
    }

    //this.setLayout(new GridLayout(1, 3));
    this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    this.add(vis_adjusterP);
    this.add(score_thresh_adjuster);
    //this.add(options_holder);
    this.add(options_pan);

    labelCB.addActionListener(this);
    yaxisCB.addActionListener(this);
    boundsCB.addActionListener(this);
    handleCB.addActionListener(this);
    cloneB.addActionListener(this);
    colorB.addActionListener(this);
    styleCB.addActionListener(this);
    boundsB.addActionListener(this);
    floatB.addActionListener(this);
    attachB.addActionListener(this);
    saveB.addActionListener(this);
    deleteB.addActionListener(this);
    selectAllB.addActionListener(this);
    setDefaultsB.addActionListener(this);
    groupGraphsB.addActionListener(this);
    transformTF.addActionListener(this);
    scaleCB.addActionListener(this);
    
    gmodel.addSeqSelectionListener(this);
    gmodel.addSymSelectionListener(this);
  }

  public void symSelectionChanged(SymSelectionEvent evt) {
    //    System.out.println("selected event received by GraphAdjusterView");
    Object src = evt.getSource();
    // if selection event originally came from here, then ignore it...
    if (src == this) { return; }

    java.util.List selected_syms = evt.getSelectedSyms();
    int symcount = selected_syms.size();
    grafs.clear();
    for (int i=0; i<symcount; i++) {
      if (selected_syms.get(i) instanceof GraphSym) {
        grafs.add(selected_syms.get(i));
      }
    }
    
    boolean all_match_attached = true;
    boolean all_match_y_axis = true;
    boolean all_match_label = true;
    
    int grafcount = grafs.size();
    saveB.setEnabled(grafcount <= 1);
    glyphs = new ArrayList();
    GraphGlyph first_glyph = null;
    
    for (int i=0; i<grafcount; i++) {
      GraphSym graf = (GraphSym)grafs.get(i);
      GraphGlyph gl = (GraphGlyph)nwidg.getItem(graf);
      if (gl instanceof SmartGraphGlyph) {
        if (i==0) {
          first_glyph = gl;
        }
        glyphs.add(gl);
        all_match_label &= (first_glyph.getShowLabel() == gl.getShowLabel());
        all_match_y_axis &= (first_glyph.getShowAxis() == gl.getShowAxis());
        all_match_attached &= (first_glyph.getGraphState().getFloatGraph() == gl.getGraphState().getFloatGraph());
      }
    }
    
    vis_bounds_adjuster.setGraphs(glyphs);
    score_thresh_adjuster.setGraphs(glyphs);
    
    if (glyphs.isEmpty()) {
      attachB.setEnabled(false);
      floatB.setEnabled(false);
      yaxisCB.setEnabled(false);
      labelCB.setEnabled(false);
      not_attached_or_floatingB.setSelected(true);
    } else {
      attachB.setEnabled(true);
      floatB.setEnabled(true);
      yaxisCB.setEnabled(true);
      labelCB.setEnabled(true);
      if (all_match_attached) {
        if (first_glyph.getGraphState().getFloatGraph()) {
          floatB.setSelected(true);
        } else {
          attachB.setSelected(true);
        }
      } else {
        not_attached_or_floatingB.setSelected(true);
      }
      yaxisCB.setSelected(all_match_y_axis && first_glyph.getShowAxis());
      labelCB.setSelected(all_match_label && first_glyph.getShowLabel());
    }
    
    boolean b = (! glyphs.isEmpty());
    scaleCB.setEnabled(b);
    handleCB.setEnabled(b);
    boundsCB.setEnabled(b);
    colorB.setEnabled(b);
    cloneB.setEnabled(b);
    saveB.setEnabled(b);
    deleteB.setEnabled(b);
    styleCB.setEnabled(b);
  }
  
  public void actionPerformed(ActionEvent evt) {
    //    System.out.println("GraphAdjusterView heard action event: " + evt);
    Object src = evt.getSource();
    int gcount = grafs.size();
    if (src == labelCB) {
      boolean selected = labelCB.isSelected();
      for (int i=0; i<gcount; i++) {
        GraphSym graf = (GraphSym)grafs.get(i);
        GraphGlyph gl = (GraphGlyph)nwidg.getItem(graf);
        if (gl != null) { gl.setShowLabel(selected); }
      }
      nwidg.updateWidget();
    }
    else if (src == boundsCB) {
      boolean selected = boundsCB.isSelected();
      for (int i=0; i<gcount; i++) {
        GraphSym graf = (GraphSym)grafs.get(i);
        GraphGlyph gl = (GraphGlyph)nwidg.getItem(graf);
        if (gl != null) { gl.setShowBounds(selected); }
      }
      nwidg.updateWidget();
    }
    else if (src == handleCB) {
      boolean selected = handleCB.isSelected();
      for (int i=0; i<gcount; i++) {
        GraphSym graf = (GraphSym)grafs.get(i);
        GraphGlyph gl = (GraphGlyph)nwidg.getItem(graf);
        if (gl != null)  { gl.setShowHandle(selected); }
      }
      nwidg.updateWidget();
    }
    else if (src == yaxisCB) {
      boolean selected = yaxisCB.isSelected();
      for (int i=0; i<gcount; i++) {
        GraphSym graf = (GraphSym)grafs.get(i);
        GraphGlyph gl = (GraphGlyph)nwidg.getItem(graf);
        if (gl != null && gl instanceof SmartGraphGlyph) {
          ((SmartGraphGlyph)gl).setShowAxis(selected);
        }
      }
      nwidg.updateWidget();
    }
    else if (src == colorB) {
      if (gcount > 0) {
        // Set an initial color so that the "reset" button will work.
        GraphSym graf_0 = (GraphSym)grafs.get(0);
        GraphGlyph gl_0 = (GraphGlyph) nwidg.getItem(graf_0);
        Color initial_color = gl_0.getColor();
        Color col = JColorChooser.showDialog((Component)nwidg,
                                             "Graph Color Chooser", initial_color);
        // Note: If the user selects "Cancel", col will be null
        if (col != null) for (int i=0; i<gcount; i++) {
          GraphSym graf = (GraphSym)grafs.get(i);
          AnnotStyle annot_style = AnnotStyle.getInstance(graf.getGraphName(), false);
          annot_style.setColor(col);
          GraphGlyph gl = (GraphGlyph)nwidg.getItem(graf);
          if (gl != null) {
            GraphState state = gl.getGraphState();
            if (state != null) {
              state.setColor(col);
            }
            gl.setColor(col);
            // if graph is in a tier, change foreground color of tier also
            //   (which in turn triggers change in color for TierLabelGlyph...)
            if (gl.getParent() instanceof TierGlyph) {
              gl.getParent().setForegroundColor(col);
            }
          }
        }
        nwidg.updateWidget();
      }
    }
    else if (src == styleCB) {
      String selection = (String)((JComboBox)styleCB).getSelectedItem();
      if (selection == BLANK) { }         // do nothing
      else  {
        int style = ((Integer)string2style.get(selection)).intValue();
        //        System.out.println("style val: " + style);
        for (int i=0; i<gcount; i++) {
          GraphSym graf = (GraphSym)grafs.get(i);
          GraphGlyph gl = (GraphGlyph)nwidg.getItem(graf);
          if (gl != null && gl instanceof SmartGraphGlyph) {
            gl.setShowGraph(true);
            ((SmartGraphGlyph)gl).setGraphStyle(style);
          }
        }
        nwidg.updateWidget();
      }
    }
    else if (src == floatB) {
      if (floatB.isSelected()) {
        for (int i=0; i<gcount; i++) {
          GraphSym graf = (GraphSym)grafs.get(i);
          GraphGlyph gl = (GraphGlyph)nwidg.getItem(graf);
          if (gl != null) {
            boolean is_floating = gl.getGraphState().getFloatGraph();
            if (! is_floating) {
              GraphGlyphUtils.floatGraph(gl, gviewer);
            }
          }
        }
      }
    }
    else if (src == attachB) {
      if (attachB.isSelected()) {
        for (int i=0; i<gcount; i++) {
          GraphSym graf = (GraphSym)grafs.get(i);
          GraphGlyph gl = (GraphGlyph)nwidg.getItem(graf);
          if (gl != null) {
            boolean is_floating = gl.getGraphState().getFloatGraph();
            if (is_floating) {
              GraphGlyphUtils.attachGraph(gl, gviewer);
            }
          }
        }
      }
    }
    else if (src == selectAllB) {
      gviewer.selectAllGraphs();
    }
    else if (src == setDefaultsB) {
      PreferencesPanel pp = PreferencesPanel.getSingleton();
      pp.setTab(PreferencesPanel.TAB_NUM_GRAPHS_VIEW);
      pp.getFrame().show();
    }
    else if (src == groupGraphsB) {
      groupGraphs(grafs);
    }
    else if (src == saveB) {
      saveGraph();
    }
    else if (src == deleteB) {
      deleteGraphs();
    }
    else if (src == cloneB) {
      cloneGraphs();
    }
    else if (src == scaleCB) {
      String selection = (String)((JComboBox)scaleCB).getSelectedItem();
      if (selection != BLANK) {
        System.out.println("selected scaling: " + selection);
        FloatTransformer trans = (FloatTransformer)name2transform.get(selection);
        Timer tim = new Timer();
        tim.start();
        transformGraph(selection, trans);
        System.out.println("time to transform graph: " + tim.read()/1000f);
      }
    }
  }

  int transform_count = 0;
  public void transformGraph(String trans_name, FloatTransformer transformer) {
    int gcount = grafs.size();
    int newgraf_count = 0;
    for (int i=0; i<gcount; i++) {
      GraphSym graf = (GraphSym)grafs.get(i);

      float[] old_ycoords = graf.getGraphYCoords();
      int pcount = old_ycoords.length;
      float[] new_ycoords = new float[pcount];

      for (int k=0; k<pcount; k++) {
        new_ycoords[k] = transformer.transform(old_ycoords[k]);
      }
      String newname = trans_name + " (" + graf.getGraphName() + ") ";
      GraphSym newgraf =
        new GraphSym(graf.getGraphXCoords(), new_ycoords, newname, graf.getGraphSeq());
      //      System.out.println(newgraf);
      ((MutableAnnotatedBioSeq)newgraf.getGraphSeq()).addAnnotation(newgraf);
      newgraf_count++;
      transform_count++;
    }
    if (newgraf_count > 0)  {
      gviewer.setAnnotatedSeq(gviewer.getAnnotatedSeq(), true, true);
    }
  }

  public void cloneGraphs() {
    System.out.println("cloning graphs");
    int gcount = grafs.size();
    try  {
      for (int i=0; i<gcount; i++) {
        GraphSym oldsym = (GraphSym)grafs.get(i);
        GraphSym newsym = (GraphSym)oldsym.clone();
        AnnotatedBioSeq aseq = (AnnotatedBioSeq)newsym.getGraphSeq();
        if (aseq instanceof MutableAnnotatedBioSeq) {
          MutableAnnotatedBioSeq mut = (MutableAnnotatedBioSeq) aseq;
          mut.addAnnotation(newsym);
        }
      }
    }
    catch (Exception ex)  {
      ex.printStackTrace();
    }
    updateViewer();
    //    nwidg.updateWidget();
  }

  protected void updateViewer()  {
    final SeqMapView current_viewer = gviewer;
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        current_viewer.setAnnotatedSeq(gmodel.getSelectedSeq(), true, true);
      }
    });
  }

  public void deleteGraphs() {
    int gcount = grafs.size();
    for (int i=0; i<gcount; i++) {
      GraphSym graf = (GraphSym)grafs.get(i);
      deleteGraph(graf);
    }
    gmodel.clearSelectedSymmetries(this);
    nwidg.updateWidget();
  }

  /**
   *  Removes a GraphSym from the annotated bio seq it is annotating (if any),
   *     and tries to make sure the GraphSym can be garbage collected.
   *  Tries to delete the GraphGlyph representing the GraphSym.  If the GraphSym
   *  happens to be a child of a tier in the widget, and the tier has no children
   *  left after deleting the graph, then delete the tier as well.
   */
  void deleteGraph(GraphSym gsym) {
    //System.out.println("deleting graph: " + gsym);
    gviewer.getGraphFactoryHash().remove(gsym);

    AnnotatedBioSeq aseq = (AnnotatedBioSeq)gsym.getGraphSeq();
    if (aseq instanceof MutableAnnotatedBioSeq) {
      MutableAnnotatedBioSeq mut = (MutableAnnotatedBioSeq) aseq;
      mut.removeAnnotation(gsym);
    }
    GraphGlyph gl = (GraphGlyph)nwidg.getItem(gsym);
    if (gl != null) {
      vis_bounds_adjuster.deleteGraph(gl);  // trying to remove any references to GraphSym
      score_thresh_adjuster.deleteGraph(gl);

      nwidg.removeItem(gl);
      // clean-up references to the graph, allowing garbage-collection, etc.
      gviewer.select(Collections.EMPTY_LIST);

      // if this is not a floating graph, then it's in a tier,
      //    so check tier -- if this graph is only child, then get rid of the tier also
      if (nwidg instanceof AffyLabelledTierMap &&
          (! gl.getGraphState().getFloatGraph()) ) {
        AffyLabelledTierMap map = (AffyLabelledTierMap)nwidg;
        GlyphI parentgl = gl.getParent();
        parentgl.removeChild(gl);
        if (parentgl.getChildCount() == 0) {  // if no children left in tier, then remove it
          if (parentgl instanceof TierGlyph) {
            map.removeTier((TierGlyph)parentgl);
            gviewer.getGraphStateTierHash().remove(gl.getGraphState());
            map.packTiers(false, true, false);
            map.stretchToFit(false, false);
          }
        }
      }
    }
  }


  public void saveGraph() {
    int gcount = grafs.size();
    if (gcount > 1) {
      // actually shouldn't get here, since save button is disabled if more than one graph
      IGB.errorPanel("Can only save one graph at a time");
    }
    else if (gcount == 1) {
      GraphSym gsym = (GraphSym)grafs.get(0);
      FileOutputStream ostr = null;
      try {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(load_dir_tracker.getFile());
        int option = chooser.showSaveDialog(gviewer.getFrame());
        if (option == JFileChooser.APPROVE_OPTION) {
          load_dir_tracker.setFile(chooser.getCurrentDirectory());
          File fil = chooser.getSelectedFile();
          GraphSymUtils.writeGraphFile(gsym, fil.getAbsolutePath());
        }
      }
      catch (Exception ex) {
        IGB.errorPanel("Error saving graph", ex);
      }
    }
  }

  public void groupGraphs(java.util.List grafs) {
    int gcount = grafs.size();
    boolean float_group = false;
    if (gcount > 0) {
      GraphSym sym1 = (GraphSym)grafs.get(0);
      GraphGlyph glyph1 = (GraphGlyph)nwidg.getItem(sym1);
      if (glyph1 != null) {
        float_group = glyph1.getGraphState().getFloatGraph();
        GlyphI parent = glyph1.getParent();
        TierGlyph parent_tier = null;
        if (parent instanceof TierGlyph) {
          parent_tier = (TierGlyph)parent;
        }
        else {
          parent_tier = GraphGlyphUtils.attachGraph(glyph1, gviewer, null);
        }
        for (int i=1; i<gcount; i++) {
          GraphSym sym = (GraphSym)grafs.get(i);
          GraphGlyph gl = (GraphGlyph)nwidg.getItem(sym);
          if (gl != null) {
            GraphGlyphUtils.attachGraph(gl, gviewer, parent_tier);
          }
        }
      }
    }
  }


  public static void main(String[] args) {
    SeqMapView map = new SeqMapView();

    JFrame frm = new JFrame();
    GraphAdjusterView tester = new GraphAdjusterView(map);
    Container cpane = frm.getContentPane();
    cpane.setLayout(new BorderLayout());
    cpane.add("Center", tester);
    frm.pack();
    frm.show();

    frm.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
  }

  public void seqSelectionChanged(SeqSelectionEvent evt)  {
    if (IGB.DEBUG_EVENTS)  {
      System.out.println("GraphAdjusterView received SeqSelectionEvent, selected seq: " + evt.getSelectedSeq());
    }
    AnnotatedBioSeq newseq = evt.getSelectedSeq();
    if (newseq != current_seq) {
      current_seq = newseq;
      java.util.List selected_syms = gviewer.getSelectedSyms();
      SymSelectionEvent newevt = new SymSelectionEvent(gviewer, selected_syms);
      symSelectionChanged(newevt);
    }
  }
  
  /** Parse a String floating-point number that may optionally end with a "%" symbol. */
  public static float parsePercent(String text) throws NumberFormatException {
    if (text.endsWith("%")) { 
      text = text.substring(0, text.length()-1);
    }

    float f = Float.parseFloat(text);
    return f;
  }
}


/*
 * Logarithm base change: log_base_b(x) = log_base_a(x)/log_base_a(b)
 * For example:
 *     log10(x) = ln(x)/ln(10) = ln(x)/2.30258 = 0.4343 * ln(x)
 *
 *  use Math.ln(x) for ln(x)
 *  use Math.exp(x) for e^x (inverse of ln(x))
 *  use Math.pow(y, x) for y^x (inverse of log_base_y(x)
 *
 */
class LogNatural implements FloatTransformer {
  static float LN1 = (float)Math.log(1); // should be 0...
  public float transform(float x) {
    // could pick any threshold > 0 to cut off low end at,
    // but thresholding at 1 for similarity to GTRANS
    return (x <= 1) ? LN1 : (float)Math.log(x);
  }
  public float inverseTransform(float y) {
    throw new RuntimeException("LogNatural.inverseTransform called, " +
                               "but LogNatural is not an invertible function");
  }
  /** not invertible because values < 1 before transform cannot be recovered... */
  public boolean isInvertible()  { return false; }
}


class LogBase10 implements FloatTransformer {
  static double LN10 = Math.log(10);
  static float LOG10_1 = (float)(Math.log(1)/LN10);
  public float transform(float x) {
    // return (float)(Math.log(x)/LN10);
    return (x <= 1) ? LOG10_1 : (float)(Math.log(x)/LN10);
  }
  public float inverseTransform(float x) {
    throw new RuntimeException("LogBase10.inverseTransform called, " +
                               "but LogBase10 is not an invertible function");
  }
  public boolean isInvertible()  { return false; }
}


class LogBase2 implements FloatTransformer {
  static double LN2 = Math.log(2);
  static float LOG2_1 = (float)(Math.log(1)/LN2);
  public float transform(float x) {
    return (x <= 1) ? LOG2_1 : (float)(Math.log(x)/LN2);
  }
  public float inverseTransform(float x) {
    throw new RuntimeException("LogBase2.inverseTransform called, " +
                               "but LogBase2 is not an invertible function");
  }
  public boolean isInvertible()  { return false; }
}

class LogTransform implements FloatTransformer {
  double base;
  double LN_BASE;
  float LOG_1;
  public LogTransform(double base) {
    this.base = base;
    LN_BASE = Math.log(base);
    LOG_1 = (float)(Math.log(1)/LN_BASE);
  }
  public float transform(float x) {
    return (x <= 1) ? LOG_1 : (float)(Math.log(x)/LN_BASE);
  }
  public float inverseTransform(float x) {
    return (float)(Math.pow(base, x));
  }
  public boolean isInvertible() { return true; }
}

/**
 *   Generalized replacement for LogNatural, LogBase2, LogBase10, etc.
 *    transforms x to base raised to the x (base^x)
 */
class PowTransform implements FloatTransformer {
  double base;
  double LN_BASE;
  float LOG_1;
  public PowTransform(double base) {
    this.base = base;
    // if base == Math.E, then LN_BASE will be 1
    LN_BASE = Math.log(base);
    LOG_1 = (float)(Math.log(1)/LN_BASE);
  }
  public float transform(float x) {
    return (float)Math.pow(base, x);
  }
  public float inverseTransform(float x) {
    //    throw new RuntimeException("LogBase2.inverseTransform called, " +
    //                               "but LogBase2 is not an invertible function");
    return (x <= 1) ? LOG_1 : (float)(Math.log(x)/LN_BASE);
  }
  public boolean isInvertible() { return true; }
}

/**
 *  alternative implementation of PowTransform.
 *  since raising to a power is inverse of taking logarithm,
 *     should be able to implement as inverse of LogTransform
 *     (transform() calls LogTransform.inverseTransform(),
 *      inverseTransform() calls LogTransform.transform())
 */
class InverseLogTransform implements FloatTransformer {
  LogTransform inner_trans;
  public InverseLogTransform(double base) {
    inner_trans = new LogTransform(base);
  }
  public float transform(float x) { return inner_trans.inverseTransform(x); }
  public float inverseTransform(float x) { return inner_trans.transform(x); }
  public boolean isInvertible() { return true; }
}
