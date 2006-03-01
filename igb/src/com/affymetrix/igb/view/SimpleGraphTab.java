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

import com.affymetrix.genometry.AnnotatedBioSeq;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.event.SeqSelectionEvent;
import com.affymetrix.igb.event.SeqSelectionListener;
import com.affymetrix.igb.event.SymSelectionEvent;
import com.affymetrix.igb.event.SymSelectionListener;
import com.affymetrix.igb.genometry.GraphSym;
import com.affymetrix.igb.genometry.SingletonGenometryModel;
import com.affymetrix.igb.glyph.GraphGlyph;
import com.affymetrix.igb.glyph.GraphVisibleBoundsSetter;
import com.affymetrix.igb.glyph.SmartGraphGlyph;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;

public class SimpleGraphTab extends JPanel
implements SeqSelectionListener, SymSelectionListener {

  SeqMapView gviewer = null;
  AnnotatedBioSeq current_seq;
  SingletonGenometryModel gmodel;
  
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
  JRadioButton hidden_styleB = new JRadioButton("No Selectoin"); // this button will not be displayed
  ButtonGroup stylegroup = new ButtonGroup();
  
  JSlider height_slider = new JSlider(JSlider.VERTICAL);
  
  JButton selectAllB = new JButton("Select All Graphs");
  JButton resetB = new JButton("Reset Appearance");
  JButton advB = new JButton("Advanced...");
  
  public SimpleGraphTab() {
    this(IGB.getSingletonIGB());
  }
  
  public SimpleGraphTab(IGB igb) {
    if (igb == null) {
      this.gviewer = null;
    } else {
      this.gviewer = igb.getMapView();
    }
    
    this.setLayout(new BorderLayout());

    Box stylebox = Box.createVerticalBox();
    stylebox.add(barB);
    stylebox.add(dotB);
    stylebox.add(hmapB);
    stylebox.add(lineB);
    stylebox.add(mmavgB);
    stylebox.add(sstepB);

    barB.addActionListener(new GraphStyleSetter(SmartGraphGlyph.BAR_GRAPH));
    dotB.addActionListener(new GraphStyleSetter(SmartGraphGlyph.DOT_GRAPH));
    hmapB.addActionListener(new GraphStyleSetter(SmartGraphGlyph.HEAT_MAP));
    lineB.addActionListener(new GraphStyleSetter(SmartGraphGlyph.LINE_GRAPH));
    mmavgB.addActionListener(new GraphStyleSetter(SmartGraphGlyph.MINMAXAVG));
    sstepB.addActionListener(new GraphStyleSetter(SmartGraphGlyph.STAIRSTEP_GRAPH));
    
    stylegroup.add(barB);
    stylegroup.add(dotB);
    stylegroup.add(hmapB);
    stylegroup.add(lineB);
    stylegroup.add(mmavgB);
    stylegroup.add(sstepB);
    stylegroup.add(hidden_styleB); // invisible button
    stylebox.setBorder(new TitledBorder("Style"));
    
    hidden_styleB.setSelected(true); // deselect all visible radio buttons

    if (gviewer == null) {
      vis_bounds_setter = new GraphVisibleBoundsSetter(null);
    } else {
      vis_bounds_setter = new GraphVisibleBoundsSetter(gviewer.getSeqMap());
    }
    
    Box scalebox = Box.createVerticalBox();
    //    scalebox.setBorder(new TitledBorder("Graph Scaling"));
    scalebox.setBorder(new TitledBorder("Y-axis Scale"));
    scalebox.add(vis_bounds_setter);
    height_slider = new JSlider(JSlider.HORIZONTAL);
    height_slider.setBorder(new TitledBorder("Height"));
    scalebox.add(height_slider);

    Box butbox = Box.createHorizontalBox();
    butbox.add(Box.createHorizontalGlue());
    butbox.add(selectAllB);
    butbox.add(Box.createHorizontalStrut(5));
    butbox.add(resetB);
    butbox.add(Box.createHorizontalStrut(5));
    butbox.add(advB);
    butbox.add(Box.createHorizontalGlue());

    selectAllB.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (gviewer != null) { gviewer.selectAllGraphs(); }
      }
    });    
    
    this.add(selected_graphs_label, "North");
    this.add(stylebox, "West");
    this.add(scalebox, "Center");
    this.add(butbox, "South");    
    
    setSeqMapView(this.gviewer); // called for the side-effects

    gmodel = SingletonGenometryModel.getGenometryModel();
    gmodel.addSeqSelectionListener(this);
    gmodel.addSymSelectionListener(this);
  }
  
  void setSeqMapView(SeqMapView smv) {
    this.gviewer = smv;
    boolean enabled = (gviewer != null);
    enableButtons(stylegroup, enabled);
    selectAllB.setEnabled(enabled);
  }  
  
  public void setEnabled(boolean b) {
    super.setEnabled(b);
    resetB.setEnabled(b);
    height_slider.setEnabled(b);
    enableButtons(stylegroup, b);
  }
  
  void enableButtons(ButtonGroup g, boolean b) {
    Enumeration e = g.getElements();
    while (e.hasMoreElements()) {
      AbstractButton but = (AbstractButton) e.nextElement();
      but.setEnabled(b);
    }
  }
    
  java.util.List grafs = new ArrayList();
  java.util.List glyphs = new ArrayList();  
  int graph_style = -1;
  
  public void symSelectionChanged(SymSelectionEvent evt) {
    if (DEBUG_EVENTS) {
      System.out.println("SymSelectionEvent received by " + this.getClass().getName());
    }
    Object src = evt.getSource();
    // if selection event originally came from here, then ignore it...
    if (src == this) { return; }

    java.util.List selected_syms = evt.getSelectedSyms();
    int symcount = selected_syms.size();
    
    grafs.clear();
    glyphs.clear();
    
    graph_style = -1;
    
    for (int i=0; i<symcount; i++) {
      if (selected_syms.get(i) instanceof GraphSym) {
        GraphSym graf = (GraphSym) selected_syms.get(i);
        grafs.add(graf);
        GraphGlyph gl = (GraphGlyph) gviewer.getSeqMap().getItem(graf);
        if (gl != null) {
          glyphs.add(gl);
          if (gl instanceof SmartGraphGlyph) {
            SmartGraphGlyph sggl = (SmartGraphGlyph) gl;
            int this_graph_style = sggl.getGraphStyle();
            if (graph_style == -1) {
              graph_style = this_graph_style;
            } else if (this_graph_style != graph_style) {
              graph_style = -2; // indicates that multiple graphs are of different styles
            }
          }
        }
      }
    }
    
    int num_graphs = grafs.size();

    if (num_graphs == 0) {
      selected_graphs_label.setText("No graphs selected");
    } else if (num_graphs == 1) {
      GraphSym graf_0 =(GraphSym) grafs.get(0);
      selected_graphs_label.setText(graf_0.getGraphName());
    } else {
      selected_graphs_label.setText(num_graphs + " graphs selected");
    }
        
    switch(graph_style) {
      case SmartGraphGlyph.MINMAXAVG:
        mmavgB.setSelected(true);
        break;
      case GraphGlyph.LINE_GRAPH:
        lineB.setSelected(true);
        break;
      case GraphGlyph.BAR_GRAPH:
        barB.setSelected(true);
        break;
      case GraphGlyph.DOT_GRAPH:
        dotB.setSelected(true);
        break;
      case GraphGlyph.HEAT_MAP:
        hmapB.setSelected(true);
        break;
      case GraphGlyph.STAIRSTEP_GRAPH:
        sstepB.setSelected(true);
        break;
      default:
        hidden_styleB.setSelected(true);
        break;
    }
    
    setEnabled(! grafs.isEmpty());
    vis_bounds_setter.setGraphs(glyphs);    
  }
  
  public void seqSelectionChanged(SeqSelectionEvent evt)  {
    if (DEBUG_EVENTS)  {
      System.out.println("SeqSelectionEvent, selected seq: " + evt.getSelectedSeq() + " recieved by " + this.getClass().getName());
    }
    AnnotatedBioSeq newseq = evt.getSelectedSeq();
    if (newseq != current_seq) {
      current_seq = newseq;
      java.util.List selected_syms = gviewer.getSelectedSyms();
      SymSelectionEvent newevt = new SymSelectionEvent(gviewer, selected_syms);
      symSelectionChanged(newevt);
    }
  }
  
  public static void main(String[] args) {    
    SimpleGraphTab graph_tab = new SimpleGraphTab();
    JFrame fr = new JFrame();
    fr.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    Container cpan = fr.getContentPane();
    cpan.add(graph_tab);
    fr.pack();
    fr.show();
  }

  class GraphStyleSetter implements ActionListener {
    
    int style = 0;
    
    public GraphStyleSetter(int style) {
      this.style = style;
    }
    
    public void actionPerformed(ActionEvent event) {
      if (gviewer == null) {
        return;
      }
      
      Runnable r = new Runnable() {
        public void run() {
          for (int i=0; i<grafs.size(); i++) {
            GraphSym graf = (GraphSym) grafs.get(i);
            GraphGlyph gl = (GraphGlyph) gviewer.getSeqMap().getItem(graf);
            if (gl != null && gl instanceof SmartGraphGlyph) {
              gl.setShowGraph(true);
              ((SmartGraphGlyph) gl).setGraphStyle(style);
            }
          }
          gviewer.getSeqMap().updateWidget();
        }
      };
      
      SwingUtilities.invokeLater(r);
    }
  }
  
}
