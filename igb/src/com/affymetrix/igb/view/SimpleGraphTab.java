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

package com.affymetrix.igb.view;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.util.Timer;
import com.affymetrix.genometry.AnnotatedBioSeq;
import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.igb.Application;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.style.*;
import com.affymetrix.igb.glyph.GraphGlyph;
import com.affymetrix.igb.glyph.GraphScoreThreshSetter;
import com.affymetrix.genometryImpl.style.GraphStateI;
import com.affymetrix.igb.glyph.GraphVisibleBoundsSetter;
import com.affymetrix.genometryImpl.style.HeatMap;
import com.affymetrix.igb.glyph.SmartGraphGlyph;
import com.affymetrix.igb.tiers.TierGlyph;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.FloatTransformer;
import com.affymetrix.genometryImpl.util.FloatTransformer.IdentityTransform;
import com.affymetrix.genometryImpl.util.FloatTransformer.InverseLogTransform;
import com.affymetrix.genometryImpl.util.FloatTransformer.LogTransform;
import com.affymetrix.igb.util.GraphGlyphUtils;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;


public final class SimpleGraphTab extends JPanel
implements SeqSelectionListener, SymSelectionListener {

  SeqMapView gviewer = null;
  
  AnnotatedBioSeq current_seq;
  SingletonGenometryModel gmodel;

  boolean is_listening = true; // used to turn on and off listening to GUI events

  GraphScoreThreshSetter score_thresh_adjuster;
  GraphVisibleBoundsSetter vis_bounds_setter;

  // Whether to use this tab or not
  public static boolean USE_SIMPLE_GRAPH_TAB = true;

  boolean DEBUG_EVENTS = false;

  JLabel selected_graphs_label = new JLabel("No Graphs Selected");
  JRadioButton mmavgB = new JRadioButton("Min/Max/Avg");
  JRadioButton lineB = new JRadioButton("Line");
  JRadioButton barB = new JRadioButton("Bar");
  JRadioButton dotB = new JRadioButton("Dot");
  JRadioButton sstepB = new JRadioButton("Stairstep");
  JRadioButton hmapB = new JRadioButton("Heat Map");
  JRadioButton hidden_styleB = new JRadioButton("No Selection"); // this button will not be displayed
  ButtonGroup stylegroup = new ButtonGroup();

  JButton colorB = new JButton("Color");
  JSlider height_slider = new JSlider(JSlider.HORIZONTAL, 10, 500, 50);

  public Action select_all_graphs_action =  new AbstractAction("Select All Graphs") {
    public void actionPerformed(ActionEvent e) {
        if (gviewer != null) { 
          gviewer.selectAllGraphs();
        }
      }
   };

  public Action delete_selected_graphs_action =  new AbstractAction("Delete Selected Graphs") {
    public void actionPerformed(ActionEvent e) {
        GraphAdjusterView.deleteGraphs(gmodel, gviewer, grafs);
      }
   };
   
  public Action save_selected_graphs_action =  new AbstractAction("Save Selected Graphs...") {
    public void actionPerformed(ActionEvent e) {
        GraphAdjusterView.saveGraphs(gviewer, gmodel, grafs);
      }
   };
   
  public Action graph_threshold_action =  new AbstractAction("Graph Thresholding...") {
    public void actionPerformed(ActionEvent e) {
        showGraphScoreThreshSetter();
      }
   };
   
   
  JButton selectAllB = new JButton(select_all_graphs_action);
  JButton saveB = new JButton(save_selected_graphs_action);
  JButton deleteB = new JButton(delete_selected_graphs_action);
  JButton threshB = new JButton(graph_threshold_action);

  JButton combineB = new JButton("Join");
  JButton splitB = new JButton("Split");
  JButton addB;
  JButton subB;
  JButton mulB;
  JButton divB;

  JLabel heat_map_label = new JLabel("Heat Map:");
  JComboBox heat_mapCB;

  JPanel advanced_panel;

  
  public SimpleGraphTab() {
    this(Application.getSingleton());
  }

  public SimpleGraphTab(Application app) {
    if (app == null) {
      this.gviewer = SeqMapView.makeSeqMapView(true, false); // for testing only
    } else {
      this.gviewer = app.getMapView();
    }

    Vector<String> v = new Vector<String>(8);
    v.add(HeatMap.HEATMAP_0);
    v.add(HeatMap.HEATMAP_1);
    v.add(HeatMap.HEATMAP_2);
    v.add(HeatMap.HEATMAP_3);
    v.add(HeatMap.HEATMAP_4);
    v.add(HeatMap.HEATMAP_T_0);
    v.add(HeatMap.HEATMAP_T_2);
    v.add(HeatMap.HEATMAP_T_3);
    v.add(HeatMap.HEATMAP_T_1); // List red and green before blue.  Blue is less useful.
    heat_mapCB = new JComboBox(v);
    heat_mapCB.addItemListener(new HeatMapItemListener());

    // A box to contain the heat-map JComboBox, to help get the alignment right
    Box heat_mapCB_box = Box.createHorizontalBox();
    heat_mapCB_box.add(Box.createHorizontalStrut(16));
    heat_mapCB_box.add(heat_mapCB);
    //heat_mapCB_box.add(Box.createHorizontalGlue());
    heat_mapCB_box.setMaximumSize(heat_mapCB_box.getPreferredSize());

    Box stylebox_radiobox = Box.createHorizontalBox();
    Box stylebox_radiobox_col1 = Box.createVerticalBox();
    Box stylebox_radiobox_col2 = Box.createVerticalBox();
    stylebox_radiobox_col1.add(barB);
    stylebox_radiobox_col1.add(dotB);
    stylebox_radiobox_col1.add(hmapB);
    stylebox_radiobox_col2.add(lineB);
    stylebox_radiobox_col2.add(mmavgB);
    stylebox_radiobox_col2.add(sstepB);
    stylebox_radiobox.add(stylebox_radiobox_col1);
    stylebox_radiobox.add(stylebox_radiobox_col2);

    Box color_button_box = Box.createHorizontalBox();
    color_button_box.add(Box.createRigidArea(new Dimension(16, 1)));
    color_button_box.add(colorB);

    Box stylebox = Box.createVerticalBox();
    color_button_box.setAlignmentX(0.0f);
    stylebox.add(color_button_box);
    stylebox_radiobox.setAlignmentX(0.0f);
    stylebox.add(stylebox_radiobox);
    //stylebox.add(heat_map_label);
    heat_mapCB_box.setAlignmentX(0.0f);
    stylebox.add(heat_mapCB_box);

    barB.addActionListener(new GraphStyleSetter(GraphStateI.BAR_GRAPH));
    dotB.addActionListener(new GraphStyleSetter(GraphStateI.DOT_GRAPH));
    hmapB.addActionListener(new GraphStyleSetter(GraphStateI.MAX_HEAT_MAP));
    lineB.addActionListener(new GraphStyleSetter(GraphStateI.LINE_GRAPH));
    mmavgB.addActionListener(new GraphStyleSetter(GraphStateI.MINMAXAVG));
    sstepB.addActionListener(new GraphStyleSetter(GraphStateI.STAIRSTEP_GRAPH));

    stylegroup.add(barB);
    stylegroup.add(dotB);
    stylegroup.add(hmapB);
    stylegroup.add(lineB);
    stylegroup.add(mmavgB);
    stylegroup.add(sstepB);
    stylegroup.add(hidden_styleB); // invisible button
    stylebox.setBorder(BorderFactory.createTitledBorder("Style"));

    hidden_styleB.setSelected(true); // deselect all visible radio buttons

    if (gviewer == null) {
      vis_bounds_setter = new GraphVisibleBoundsSetter(null);
    } else {
      vis_bounds_setter = new GraphVisibleBoundsSetter(gviewer.getSeqMap());
    }
    score_thresh_adjuster = new GraphScoreThreshSetter(gviewer, vis_bounds_setter);


    //Box height_and_color_box = Box.createHorizontalBox();
    height_slider.setBorder(BorderFactory.createTitledBorder("Height"));
    //height_and_color_box.add(height_slider);

    Box scalebox = Box.createVerticalBox();
    vis_bounds_setter.setAlignmentX(0.0f);
    scalebox.add(vis_bounds_setter);
    height_slider.setAlignmentX(0.0f);
    scalebox.add(height_slider);

    height_slider.addChangeListener(new GraphHeightSetter());

    Box butbox = Box.createHorizontalBox();
    //butbox.add(Box.createHorizontalGlue());
    butbox.add(Box.createRigidArea(new Dimension(5,5)));
    butbox.add(selectAllB);
    butbox.add(Box.createRigidArea(new Dimension(5,5)));
    butbox.add(saveB);
    butbox.add(Box.createRigidArea(new Dimension(5,5)));
    butbox.add(deleteB);
    butbox.add(Box.createRigidArea(new Dimension(5,5)));
    butbox.add(Box.createHorizontalGlue());
    butbox.add(threshB);
    butbox.add(Box.createRigidArea(new Dimension(5,5)));

    Box first_two_columns = Box.createHorizontalBox();
    stylebox.setAlignmentY(0.0f);
    first_two_columns.add(stylebox);
    scalebox.setAlignmentY(0.0f);
    first_two_columns.add(scalebox);
    Box megabox = Box.createVerticalBox();
    butbox.setAlignmentX(0.0f);
    megabox.add(butbox);
    megabox.add(Box.createRigidArea(new Dimension(1,5)));
    first_two_columns.setAlignmentX(0.0f);
    megabox.add(first_two_columns);

    Box label_box = Box.createHorizontalBox();
    label_box.add(selected_graphs_label);
    label_box.add(Box.createHorizontalGlue());

    Box row1 = Box.createHorizontalBox();
//    stylebox.setAlignmentY(0.0f);
//    row1.add(stylebox);
//    scalebox.setAlignmentY(0.0f);
//    row1.add(scalebox);
    megabox.setAlignmentY(0.0f);
    row1.add(megabox);
    advanced_panel = new SimpleGraphTab.AdvancedGraphPanel();
    advanced_panel.setAlignmentY(0.0f);
    row1.add(advanced_panel);

    colorB.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        GraphAdjusterView.changeColor(grafs, gviewer);
      }
    });

    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    //label_box.setAlignmentX(0.0f);
    //this.add(Box.createRigidArea(new Dimension(1,5)));
    //this.add(label_box);
    
    //this.add(Box.createRigidArea(new Dimension(1,5)));
    //this.add(butbox);
    this.add(Box.createRigidArea(new Dimension(1,5)));
    row1.setAlignmentX(0.0f);
    this.add(row1);
    butbox.setAlignmentX(0.0f);
    butbox.setAlignmentY(1.0f);
    this.add(Box.createVerticalGlue());
    
    this.setBorder(BorderFactory.createEtchedBorder());
    
    resetSelectedGraphGlyphs(Collections.EMPTY_LIST);
    
    setSeqMapView(this.gviewer); // called for the side-effects

    gmodel = SingletonGenometryModel.getGenometryModel();
    gmodel.addSeqSelectionListener(this);
    gmodel.addSymSelectionListener(this);
  }

  void showGraphScoreThreshSetter() {
    score_thresh_adjuster.showFrame();
  }

  void setSeqMapView(SeqMapView smv) {
    this.gviewer = smv;
  }

  void enableButtons(ButtonGroup g, boolean b) {
    Enumeration e = g.getElements();
    while (e.hasMoreElements()) {
      AbstractButton but = (AbstractButton) e.nextElement();
      but.setEnabled(b);
    }
  }

  List grafs = new ArrayList();
  List glyphs = new ArrayList();

  public void symSelectionChanged(SymSelectionEvent evt) {
    List selected_syms = evt.getSelectedSyms();
    // Only pay attention to selections from the main SeqMapView or its map.
    // Ignore the splice view as well as events coming from this class itself.
    
    Object src = evt.getSource();
    if (src != gviewer && src != gviewer.getSeqMap()) {
      return;
    }
      
    resetSelectedGraphGlyphs(selected_syms);
  }
  
  public void resetSelectedGraphGlyphs(List selected_syms) {
    int symcount = selected_syms.size();
    is_listening = false; // turn off propagation of events from the GUI while we modify the settings
    if (grafs != selected_syms)   { 
      // in certain cases selected_syms arg and grafs list may be same, for example when method is being
      //     called to catch changes in glyphs representing selected sym, not the syms themselves)
      //     therefore don't want to change grafs list if same as selected_syms (especially don't want to clear it!)
      grafs.clear(); 
    }
    glyphs.clear();

    // First loop through and collect graphs and glyphs
    for (int i=0; i<symcount; i++) {
      if (selected_syms.get(i) instanceof GraphSym) {
        GraphSym graf = (GraphSym) selected_syms.get(i);
	// only add to grafs if list is not identical to selected_syms arg
        if (grafs != selected_syms)  { grafs.add(graf); }
	int gcount = gviewer.getSeqMap().getItemCount(graf);
	if (gcount == 1) {
	  GraphGlyph gl = (GraphGlyph) gviewer.getSeqMap().getItem(graf);
	  glyphs.add(gl);
	}
	// allowing for cases where same graph sym is represented by multiple graphs glyphs...
	else if (gcount > 1) {  
	  List multigl = gviewer.getSeqMap().getItems(graf);
	  // add all graph glyphs representing graph sym
	  //	  System.out.println("found multiple glyphs for graph sym: " + multigl.size());
	  glyphs.addAll(multigl);
	}
      }
    }

    int num_glyphs = glyphs.size();
    //    System.out.println("number of selected graphs: " + num_glyphs);
    double the_height = -1; // -1 indicates unknown height

    boolean all_are_floating = false;
    boolean all_show_axis = false;
    boolean all_show_label = false;
    boolean any_are_combined = false; // are any selections inside a combined tier
    boolean all_are_combined = false; // are all selections inside (a) combined tier(s)
    boolean any_are_not_combined = false; // are any selections not inside a combined tier
    boolean all_are_smart_glyphs = false; // all implement SmartGraphGlyph

    // Take the first glyph in the list as a prototype
    GraphGlyph first_glyph = null;
    int graph_style = -1;
    HeatMap hm = null;
    if (! glyphs.isEmpty()) {
      first_glyph = (GraphGlyph) glyphs.get(0);
      graph_style = first_glyph.getGraphStyle();
      if (graph_style == GraphStateI.HEAT_MAP) {
        hm = first_glyph.getHeatMap();
      }
      the_height = first_glyph.getGraphState().getTierStyle().getHeight();
      all_are_floating = first_glyph.getGraphState().getFloatGraph();
      all_show_axis = first_glyph.getGraphState().getShowAxis();
      all_show_label = first_glyph.getGraphState().getShowLabel();
      boolean this_one_is_combined = (first_glyph.getGraphState().getComboStyle() != null);
      any_are_combined = this_one_is_combined;
      all_are_combined = this_one_is_combined;
      any_are_not_combined = ! this_one_is_combined;
      all_are_smart_glyphs = (first_glyph instanceof SmartGraphGlyph);
    }

    // Now loop through other glyphs if there are more than one
    // and see if the graph_style and heatmap are the same in all selections
    for (int i=1; i < num_glyphs; i++) {
      GraphGlyph gl = (GraphGlyph) glyphs.get(i);

      all_are_floating &= gl.getGraphState().getFloatGraph();
      all_show_axis &= gl.getGraphState().getShowAxis();
      all_show_label &= gl.getGraphState().getShowLabel();
      boolean this_one_is_combined = (gl.getGraphState().getComboStyle() != null);
      any_are_combined |= this_one_is_combined;
      all_are_combined &= this_one_is_combined;
      any_are_not_combined |=  ! this_one_is_combined;
      all_are_smart_glyphs &= (first_glyph instanceof SmartGraphGlyph);

      if (first_glyph.getGraphStyle() != gl.getGraphStyle()) {
        graph_style = -1;
      }
      if (graph_style == GraphStateI.HEAT_MAP) {
        if (first_glyph.getHeatMap() != gl.getHeatMap()) {
          hm = null;
        }
      } else {
        hm = null;
      }
    }

    if (num_glyphs == 0) {
      selected_graphs_label.setText("No graphs selected");
    } else if (num_glyphs == 1) {
      GraphSym graf_0 =(GraphSym) grafs.get(0);
      selected_graphs_label.setText(graf_0.getGraphName());
    } else {
      selected_graphs_label.setText(num_glyphs + " graphs selected");
    }

    switch(graph_style) {
      case GraphStateI.MINMAXAVG:
        mmavgB.setSelected(true);
        break;
      case GraphStateI.LINE_GRAPH:
        lineB.setSelected(true);
        break;
      case GraphStateI.BAR_GRAPH:
        barB.setSelected(true);
        break;
      case GraphStateI.DOT_GRAPH:
        dotB.setSelected(true);
        break;
      case GraphStateI.MAX_HEAT_MAP:
        hmapB.setSelected(true);
        break;
      case GraphStateI.STAIRSTEP_GRAPH:
        sstepB.setSelected(true);
        break;
      default:
        hidden_styleB.setSelected(true);
        break;
    }

    if (graph_style == GraphStateI.HEAT_MAP) {
      heat_mapCB.setEnabled(true);
      if (hm == null) {
        heat_mapCB.setSelectedIndex(-1);
      } else {
        heat_mapCB.setSelectedItem(hm.getName());
      }
    } else {
      heat_mapCB.setEnabled(false);
    }

    if (the_height != -1) {
      height_slider.setValue((int) the_height);
    }
    vis_bounds_setter.setGraphs(glyphs);
    score_thresh_adjuster.setGraphs(glyphs);

    if (! glyphs.isEmpty()) {
      floatCB.setSelected(all_are_floating);
      yaxisCB.setSelected(all_show_axis);
      labelCB.setSelected(all_show_label);
    }

    boolean b = ! (grafs.isEmpty());
    height_slider.setEnabled(b);
    graph_threshold_action.setEnabled(b);
    enableButtons(stylegroup, b);
    mmavgB.setEnabled(all_are_smart_glyphs);
    floatCB.setEnabled(b);
    yaxisCB.setEnabled(b);
    labelCB.setEnabled(b);

    colorB.setEnabled(b);
    save_selected_graphs_action.setEnabled(grafs.size() == 1);
    delete_selected_graphs_action.setEnabled(b);
    cloneB.setEnabled(b);
    scaleCB.setEnabled(cloneB.isEnabled());
    
    //combineB.setSelected(all_are_combined);
    //splitB.setSelected(any_are_not_combined);
    
    combineB.setEnabled(any_are_not_combined && grafs.size() >= 2);
    splitB.setEnabled(any_are_combined);
    addB.setEnabled(grafs.size()==2);
    subB.setEnabled(grafs.size()==2);
    mulB.setEnabled(grafs.size()==2);
    divB.setEnabled(grafs.size()==2);
    
    is_listening = true; // turn back on GUI events
  }

  public void seqSelectionChanged(SeqSelectionEvent evt) {
    if (DEBUG_EVENTS)  {
      System.out.println("SeqSelectionEvent, selected seq: " + evt.getSelectedSeq() + " received by " + this.getClass().getName());
    }
    current_seq = evt.getSelectedSeq();
    resetSelectedGraphGlyphs(gmodel.getSelectedSymmetries(current_seq));
  }

  public static void main(String[] args) {
    SimpleGraphTab graph_tab = new SimpleGraphTab();
    JFrame fr = new JFrame();
    fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    Container cpan = fr.getContentPane();
    cpan.add(graph_tab);
    fr.pack();
    fr.setVisible(true);
  }

  class GraphStyleSetter implements ActionListener {

    int style = 0;

    public GraphStyleSetter(int style) {
      this.style = style;
    }

    public void actionPerformed(ActionEvent event) {
      if (DEBUG_EVENTS) {
        System.out.println(this.getClass().getName() + " got an ActionEvent: " + event);
      }
      if (gviewer == null || glyphs.isEmpty() || ! is_listening) {
        return;
      }

      Runnable r = new Runnable() {
        public void run() {
          GraphGlyph first_glyph = (GraphGlyph) glyphs.get(0);
          if (style == GraphStateI.HEAT_MAP) {
            // set to heat map FIRST so that getHeatMap() below will return default map instead of null
            first_glyph.setGraphStyle(GraphStateI.HEAT_MAP);
          }
          HeatMap hm = ((GraphGlyph) glyphs.get(0)).getHeatMap();
	  //          for (int i=0; i<grafs.size(); i++) {
          for (int i=0; i<glyphs.size(); i++) {
            GraphGlyph sggl = (GraphGlyph) glyphs.get(i);
            sggl.setShowGraph(true);
            sggl.setGraphStyle(style); // leave the heat map whatever it was
            if ((style == GraphStateI.HEAT_MAP) && (hm != sggl.getHeatMap())) {
              hm = null;
            }
          }
          if (style == GraphStateI.HEAT_MAP) {
            heat_mapCB.setEnabled(true);
            if (hm == null) {
              heat_mapCB.setSelectedIndex(-1);
            } else {
              heat_mapCB.setSelectedItem(hm.getName());
            }
          } else {
            heat_mapCB.setEnabled(false);
            // don't bother to change the displayed heat map name
          }
          gviewer.getSeqMap().updateWidget();
        }
      };

      SwingUtilities.invokeLater(r);
    }
  }

  void updateViewer() {
    final SeqMapView current_viewer = gviewer;
    final List previous_graph_syms = new ArrayList(grafs);
    // set selections to empty so that options get turned off
    resetSelectedGraphGlyphs(Collections.EMPTY_LIST);
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        current_viewer.setAnnotatedSeq(gmodel.getSelectedSeq(), true, true);
	resetSelectedGraphGlyphs(previous_graph_syms);
      }
    });
  }


  class HeatMapItemListener implements ItemListener {
    public void itemStateChanged(ItemEvent e) {
      if (gviewer == null || glyphs.isEmpty() || ! is_listening) {
        return;
      }

      if (e.getStateChange() == ItemEvent.SELECTED) {
        String name = (String) e.getItem();
        HeatMap hm = HeatMap.getStandardHeatMap(name);

        if (hm != null) {
          for (int i=0; i<glyphs.size(); i++) {
            GraphGlyph gl = (GraphGlyph) glyphs.get(i);
            gl.setShowGraph(true);
            gl.setGraphStyle(GraphStateI.MAX_HEAT_MAP);
            gl.setHeatMap(hm);
          }
          gviewer.getSeqMap().updateWidget();
        }
      }
    }
  }

  class GraphHeightSetter implements  ChangeListener {
    public void stateChanged(ChangeEvent e) {
      if (gviewer == null || glyphs.isEmpty() || ! is_listening) {
        return;
      }

      if (e.getSource() == height_slider) {
        setTheHeights((double) height_slider.getValue());
      }
    }

    void setTheHeights(double height) {
      if (gviewer == null) {
        return; // for testing
      }

      AffyTieredMap map = (AffyTieredMap)gviewer.getSeqMap();

      for (int i=0; i<glyphs.size(); i++) {
        GraphGlyph gl = (GraphGlyph) glyphs.get(i);
	//        gl.getGraphState().setGraphHeight(height);
	Rectangle2D cbox= gl.getCoordBox();
	gl.setCoords(cbox.x, cbox.y, cbox.width, height);

        // If a graph is joined with others in a combo tier, repack that tier.
	GlyphI parentgl = gl.getParent();
	if (parentgl instanceof TierGlyph) {
	  //	  System.out.println("Glyph: " + gl.getLabel() + ", packer: " + parentgl.getPacker());
	  parentgl.pack(map.getView());
	}
      }
      map.packTiers(false, true, false);
      map.stretchToFit(false, true);
      map.updateWidget();
    }
  }

  static Map name2transform;

  static String BLANK = "";
  static String IDENTITY_TRANSFORM = "Copy";
  static String LOG_10 = "Log10";
  static String LOG_2 = "Log2";
  static String LOG_NATURAL = "Natural Log";
  static String INVERSE_LOG_10 = "Inverse Log10";
  static String INVERSE_LOG_2 = "Inverse Log2";
  static String INVERSE_LOG_NATURAL = "Inverse Natural Log";

  static {
    name2transform = new LinkedHashMap();
    name2transform.put(IDENTITY_TRANSFORM, new IdentityTransform());
    name2transform.put(LOG_10, new LogTransform(10));
    name2transform.put(LOG_2, new LogTransform(2));
    name2transform.put(LOG_NATURAL, new LogTransform(Math.E));
    name2transform.put(INVERSE_LOG_10, new InverseLogTransform(10));
    name2transform.put(INVERSE_LOG_2, new InverseLogTransform(2));
    name2transform.put(INVERSE_LOG_NATURAL, new InverseLogTransform(Math.E));
  }

  JButton cloneB = new JButton("Go");
  JLabel scale_type_label = new JLabel("Transformation:");
  JComboBox scaleCB = new JComboBox();

  JCheckBox labelCB = new JCheckBox("Label");
  JCheckBox yaxisCB = new JCheckBox("Y Axis");
  JCheckBox floatCB = new JCheckBox("Floating");

  class AdvancedGraphPanel extends JPanel {

    public AdvancedGraphPanel() {

      JPanel advanced_panel = this;

      advanced_panel.setLayout(new BoxLayout(advanced_panel, BoxLayout.Y_AXIS));

      //  scaleCB.addItem(BLANK);
      Iterator iter = name2transform.keySet().iterator();
      while (iter.hasNext()) {
        String name = (String)iter.next();
        scaleCB.addItem(name);
      }

//      Box advanced_button_box = Box.createHorizontalBox();
//      advanced_button_box.add(Box.createRigidArea(new Dimension(6,5)));
////      advanced_button_box.add(cloneB);
////      advanced_button_box.add(Box.createRigidArea(new Dimension(5,5)));
//      advanced_button_box.add(threshB);
//      advanced_button_box.add(Box.createRigidArea(new Dimension(5,5)));

      Box grouping_box = Box.createHorizontalBox();
      grouping_box.add(Box.createRigidArea(new Dimension(6,0)));
      grouping_box.add(combineB);
      grouping_box.add(Box.createRigidArea(new Dimension(5,0)));
      grouping_box.add(splitB);
      grouping_box.add(Box.createRigidArea(new Dimension(5,0)));

      Box decoration_row = Box.createHorizontalBox();
      //decoration_row.setBorder(BorderFactory.createEtchedBorder());
      decoration_row.add(Box.createRigidArea(new Dimension(6,5)));
      decoration_row.add(labelCB);
      decoration_row.add(yaxisCB);
      decoration_row.add(floatCB);
      //decoration_row.add(Box.createHorizontalGlue());

      // A box to contain the scaleCB JComboBox, to help get the alignment right
      Box scaleCB_box = Box.createHorizontalBox();
      scaleCB_box.setAlignmentX(0.0f);
      scaleCB_box.add(Box.createRigidArea(new Dimension(6,5)));
      scaleCB_box.add(scaleCB);
      scaleCB_box.add(Box.createRigidArea(new Dimension(5,5)));
      scaleCB_box.add(cloneB);
      scaleCB_box.add(Box.createRigidArea(new Dimension(5,5)));
      //scaleCB_box.add(Box.createHorizontalGlue());
      scaleCB_box.setMaximumSize(scaleCB_box.getPreferredSize());


      advanced_panel.setBorder(BorderFactory.createTitledBorder("Advanced"));
      //advanced_button_box.setAlignmentX(0.0f);
      decoration_row.setAlignmentX(0.0f);
      advanced_panel.add(decoration_row);
      advanced_panel.add(Box.createRigidArea(new Dimension(5,12)));
      //
      //advanced_panel.add(advanced_button_box);
      //advanced_panel.add(Box.createRigidArea(new Dimension(5,12)));

      advanced_panel.add(scale_type_label);
      scaleCB_box.setAlignmentX(0.0f);
      //advanced_panel.add(Box.createRigidArea(new Dimension(5,12)));
      advanced_panel.add(scaleCB_box);

      grouping_box.setAlignmentX(0.0f);
      advanced_panel.add(Box.createRigidArea(new Dimension(5,12)));
      advanced_panel.add(grouping_box);

      //char division_symbol = (char) 0x00f7;
      addB = new JButton("A + B");
      subB = new JButton("A - B");
      mulB = new JButton("A * B");
      divB = new JButton("A / B");
      addB.setMargin(new Insets(2,2,2,2));
      subB.setMargin(new Insets(2,2,2,2));
      mulB.setMargin(new Insets(2,2,2,2));
      divB.setMargin(new Insets(2,2,2,2));
      Box math_box = Box.createHorizontalBox();
      math_box.add(new JLabel("Combine:"));
      math_box.add(Box.createRigidArea(new Dimension(6,0)));
      math_box.add(addB);
      math_box.add(Box.createRigidArea(new Dimension(4,0)));
      math_box.add(subB);
      math_box.add(Box.createRigidArea(new Dimension(4,0)));
      math_box.add(mulB);
      math_box.add(Box.createRigidArea(new Dimension(4,0)));
      math_box.add(divB);
      math_box.setAlignmentX(0.0f);
      advanced_panel.add(Box.createRigidArea(new Dimension(5,12)));
      advanced_panel.add(math_box);

      cloneB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          scaleGraphs();
        }
      });

      floatCB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          floatGraphs(floatCB.isSelected());
        }
      });

      labelCB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          setShowLabels(labelCB.isSelected());
        }
      });

      yaxisCB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          setShowAxis(yaxisCB.isSelected());
        }
      });

      combineB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          combineGraphs();
        }
      });
      splitB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
	  splitGraphs();
        }
      });

      addB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          graphArithmetic(GraphGlyphUtils.MATH_SUM);
        }
      });
      subB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          graphArithmetic(GraphGlyphUtils.MATH_DIFFERENCE);
        }
      });
      mulB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          graphArithmetic(GraphGlyphUtils.MATH_PRODUCT);
        }
      });
      divB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          graphArithmetic(GraphGlyphUtils.MATH_RATIO);
        }
      });
    }

    /**
     *  Puts all selected graphs in the same tier.
     *  Current glyph factories do not support floating the combined graphs.
     */
    void combineGraphs() {
      int gcount = grafs.size();

      // Note that the combo_style does not implement IFloatableTierStyle
      // because the glyph factory doesn't support floating combo graphs anyway.
      IAnnotStyle combo_style = null;
      
      // If any of them already has a combo style, use that one
      for (int i=0; i<gcount && combo_style == null; i++) {
        GraphSym gsym = (GraphSym) grafs.get(i);
        combo_style = gsym.getGraphState().getComboStyle();
      }
      // otherwise, construct a new combo style
      if (combo_style == null) {
        combo_style = new DefaultIAnnotStyle("Joined Graphs", true);
        combo_style.setHumanName("Joined Graphs");
        combo_style.setExpandable(true);
        combo_style.setCollapsed(true);
      }
      //combo_style.setHeight(5); // just use the default height
      
      // Now apply that combo style to all the selected graphs
      for (int i=0; i<gcount; i++) {
        GraphSym gsym = (GraphSym) grafs.get(i);
        GraphStateI gstate = gsym.getGraphState();
        gstate.setComboStyle(combo_style);
        gstate.getTierStyle().setHeight(combo_style.getHeight());
        gstate.setFloatGraph(false); // ignored since combo_style is set
      }
      updateViewer();
    }

    /**
     *  Puts all selected graphs in separate tiers by setting the
     *  combo state of each graph's state to null.
     */
    void splitGraphs() {
      int gcount = grafs.size();
      
      if (gcount == 0) return;
      
      for (int i=0; i<gcount; i++) {
        GraphSym gsym = (GraphSym) grafs.get(i);
        GraphStateI gstate = gsym.getGraphState();

        gstate.setComboStyle(null);

        // For simplicity, set the floating state of all new tiers to false.
        // Otherwise, have to calculate valid, non-overlapping y-positions and heights.
        gstate.setFloatGraph(false); // for simplicity
      }
      updateViewer();
    }

    
    public void graphArithmetic(String operation) {
      if (glyphs.size() == 2) {
        GraphGlyph graphA = (GraphGlyph) glyphs.get(0);
        GraphGlyph graphB = (GraphGlyph) glyphs.get(1);
        GraphSym newsym = GraphGlyphUtils.graphArithmetic(graphA, graphB, operation);
        
        if (newsym != null) {
          MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq) newsym.getGraphSeq();
          aseq.addAnnotation(newsym);
          gviewer.setAnnotatedSeq(aseq, true, true);
          GlyphI newglyph = gviewer.getSeqMap().getItem(newsym);

          updateViewer();
        }
      } else {
        ErrorHandler.errorPanel("ERROR", "Must choose exactly 2 graphs", this);
      }
    }
    
    void setShowAxis(boolean b) {
      for (int i=0; i<glyphs.size(); i++) {
        GraphGlyph gl = (GraphGlyph) glyphs.get(i);
        gl.setShowAxis(b);
      }
      gviewer.getSeqMap().updateWidget();
    }

    void setShowLabels(boolean b) {
      for (int i=0; i<glyphs.size(); i++) {
        GraphGlyph gl = (GraphGlyph) glyphs.get(i);
        gl.setShowLabel(b);
      }
      gviewer.getSeqMap().updateWidget();
    }

    void scaleGraphs() {
      String selection = (String) scaleCB.getSelectedItem();
      System.out.println("selected scaling: " + selection);
      FloatTransformer trans = (FloatTransformer) name2transform.get(selection);
      Timer tim = new Timer();
      tim.start();
      List newgrafs = GraphAdjusterView.transformGraphs(grafs, selection, trans);
      System.out.println("time to transform graph: " + tim.read()/1000f);
      if (! newgrafs.isEmpty() )  {
        updateViewer();
      }
    }

    void floatGraphs(boolean do_float) {
      boolean something_changed = false;
      for (int i=0; i<glyphs.size(); i++) {
        GraphGlyph gl = (GraphGlyph) glyphs.get(i);
        GraphStateI gstate = gl.getGraphState();
        if (gstate.getComboStyle() != null) {
          gstate.setComboStyle(null);
          something_changed = true;
        }
        boolean is_floating = gstate.getFloatGraph();
        if (do_float && (! is_floating)) {
          //GraphGlyphUtils.floatGraph(gl, gviewer);
          
          // figure out correct height
          Rectangle2D coordbox = gl.getCoordBox();
          Rectangle pixbox = new Rectangle();
          gviewer.getSeqMap().getView().transformToPixels(coordbox, pixbox);
          gstate.getTierStyle().setY(pixbox.y);
          gstate.getTierStyle().setHeight(pixbox.height);

          gstate.setFloatGraph(true);
          something_changed = true;
        } else if ( (! do_float) && is_floating) {
          //GraphGlyphUtils.attachGraph(gl, gviewer);
          
          // figure out correct height
          Rectangle2D tempbox = gl.getCoordBox();  // pixels, since in PixelFloaterGlyph 1:1 mapping of pixel:coord
          Rectangle pixbox = new Rectangle((int)tempbox.x, (int)tempbox.y, (int)tempbox.width, (int)tempbox.height);
          Rectangle2D coordbox = new Rectangle2D(); 
          gviewer.getSeqMap().getView().transformToCoords(pixbox, coordbox);
          gstate.getTierStyle().setY(coordbox.y); // currently y has no effect on attached graphs, but will someday
          gstate.getTierStyle().setHeight(coordbox.height);
  
          gstate.setFloatGraph(false);
          something_changed = true;
        }
      }
      if (something_changed) {
        updateViewer();
      }
    }
  }
}
