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

package com.affymetrix.igb.view;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genometry.*;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.glyph.*;
import com.affymetrix.igb.event.*;
import com.affymetrix.igb.tiers.*;
import com.affymetrix.igb.util.*;

public class AltSpliceView extends JComponent
     implements ActionListener, ComponentListener, ItemListener,
		SymSelectionListener, SeqSelectionListener {

  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  boolean CONTROLS_ON_SIDE = false;
  SeqMapView original_view;
  //  AffyLabelledTierMap map;
  SeqMapView spliced_view;
  OrfAnalyzer2 orf_analyzer;
  JTextField buffer_sizeTF;
  JCheckBox slice_by_selectionCB;
  java.util.List last_selected_syms = new ArrayList();
  AnnotatedBioSeq last_seq_changed = null;
  boolean pending_sequence_change = false;
  boolean pending_selection_change = false;
  boolean slice_by_selection_on = true;

  public AltSpliceView() {
    original_view = IGB.getSingletonIGB().getMapView();
    Rectangle frmbounds = SwingUtilities.getWindowAncestor(original_view).getBounds();
    this.setLayout(new BorderLayout());
    spliced_view = new SeqMapView();
    orf_analyzer = new OrfAnalyzer2(spliced_view, CONTROLS_ON_SIDE);
    buffer_sizeTF = new JTextField(4);
    buffer_sizeTF.setText("" + getSliceBuffer());
    slice_by_selectionCB = new JCheckBox("Slice By Selection", true);

    JPanel buf_adjustP = new JPanel(new FlowLayout());
    buf_adjustP.add(new JLabel("Slice Buffer: "));
    buf_adjustP.add(buffer_sizeTF);

    JPanel pan1;
    if (CONTROLS_ON_SIDE) { pan1 = new JPanel(new GridLayout(2,1)); }
    else { pan1 = new JPanel(new GridLayout(1, 2)); }

    pan1.add(slice_by_selectionCB);
    pan1.add(buf_adjustP);
    JPanel options_panel = new JPanel(new BorderLayout());

    if (CONTROLS_ON_SIDE) {
      options_panel.add("North", pan1);
      options_panel.add("South", orf_analyzer);

      JScrollPane oscroller = new JScrollPane(options_panel);
      JSplitPane splitpane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
      splitpane.setOneTouchExpandable(true);
      splitpane.setDividerSize(8);
      int dloc = (int)frmbounds.getWidth() - (int)oscroller.getPreferredSize().getWidth() - 25;  // -20 for border etc.
      splitpane.setDividerLocation(dloc);
      splitpane.setLeftComponent(spliced_view);
      splitpane.setRightComponent(oscroller);
      this.add("Center", splitpane);
    }
    else {
      options_panel.add("West", pan1);
      options_panel.add("East", orf_analyzer);
      JSplitPane splitpane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
      splitpane.setResizeWeight(1);  // allocate as much space as possible to top panel
      splitpane.setOneTouchExpandable(true);
      splitpane.setDividerSize(8);
      splitpane.setTopComponent(spliced_view);
      splitpane.setBottomComponent(options_panel);
      this.add("Center", splitpane);
    }

    this.addComponentListener(this);
    spliced_view.setColorHash(original_view.getColorHash());
    spliced_view.setFrame(original_view.getFrame());
    buffer_sizeTF.addActionListener(this);
    slice_by_selectionCB.addItemListener(this);

    gmodel.addSeqSelectionListener(this);
    gmodel.addSymSelectionListener(this);
  }

  /**
   *  This method is notified when selected symmetries change.
   *  It usually triggers a re-computation of the sliced symmetries to draw.
   *  If no selected syms, then don't change.
   *  If all selected syms are graphs, don't change (because graphs currently span entire sequence).
   */
  public void symSelectionChanged(SymSelectionEvent evt) {
    if (IGB.DEBUG_EVENTS)  { System.out.println("AltSpliceView received selection changed event"); }
    Object src = evt.getSource();
    // ignore if symmetry selection originated from this AltSpliceView -- don't want to
    //   reslice based on internal selection!
    if ((src != this) && (src != spliced_view)) {
      // catching spliced_view as source of event because currently sym selection events actually originating
      //    from AltSpliceView have their source set to the AltSpliceView's internal SeqMapView...
      last_selected_syms = evt.getSelectedSyms();
      int symcount = last_selected_syms.size();
      if (symcount > 0) {
	boolean all_graphs = true;
	for (int i=0; i<symcount; i++) {
	  if (! (last_selected_syms.get(i) instanceof GraphSym)) {
	    all_graphs = false;
	    break;
	  }
	}
	if (! all_graphs) {
	  if (! this.isShowing()) {
	    pending_selection_change = true;
	  }
	  else if (slice_by_selection_on) {
	    this.sliceAndDice(last_selected_syms);
	    pending_selection_change = false;
	  }
	  else {
	    spliced_view.select(last_selected_syms);
	    pending_selection_change = false;
	  }
	}
      }
    }
  }

  public void seqSelectionChanged(SeqSelectionEvent evt)  {
//  public void sequenceChanged(AnnotatedBioSeq newseq) {
    if (IGB.DEBUG_EVENTS)  {
      System.out.println("AltSpliceView received SeqSelectionEvent, selected seq: " + evt.getSelectedSeq());
    }
    AnnotatedBioSeq newseq = gmodel.getSelectedSeq();
    if (last_seq_changed != newseq) {
      last_seq_changed = newseq;
      if (this.isShowing() && slice_by_selection_on) {
	spliced_view.setAnnotatedSeq(last_seq_changed);
	pending_sequence_change = false;
      }
      else {
	pending_sequence_change = true;
      }
    }
  }

  public void setSliceBySelection(boolean b) {
    slice_by_selection_on = b;
  }

  public boolean getSliceBySelection() { return slice_by_selection_on; }


  public int getSliceBuffer() { return spliced_view.getSliceBuffer(); }

  public void setSliceBuffer(int buf_size, boolean refresh) {
    spliced_view.setSliceBuffer(buf_size, refresh);
    orf_analyzer.redoOrfs();
  }

  public void sliceAndDice(java.util.List syms) {
    //    System.out.println("called AltSpliceView.sliceAndDice()");
    if (syms.size() > 0) {

      // recreating the spliced views graf2factory hash with each new sliceAndDice,
      //   to reflect any changes to original views's graph factories and their GraphStates.
      Map orig_graph_factories = original_view.getGraphFactoryHash();
      Map spliced_graph_factories = spliced_view.getGraphFactoryHash();
      spliced_graph_factories.clear();
      Iterator iter = orig_graph_factories.entrySet().iterator();
      while (iter.hasNext()) {
	Map.Entry keyval = (Map.Entry)iter.next();
	GraphSym gsym = (GraphSym)keyval.getKey();
	if (gsym.getGraphSeq() == original_view.getAnnotatedSeq()) {
	  GenericGraphGlyphFactory orig_factory = (GenericGraphGlyphFactory)keyval.getValue();
	  GraphState new_state = new GraphState(orig_factory.getGraphState());
	  GenericGraphGlyphFactory new_factory = new GenericGraphGlyphFactory(new_state);
	  /**
	   *  GAH 11-22-2003
	   *  There is a problem with showing SPAN_GRAPH style graphs in the spliced view,
	   *    because they assume that the even positions in the graph's xcoords array are
	   *    starts and the odd positions are stops of the spans.  But when slicing for
	   *    the AltSpliceView, even/odd of GraphSym's coords may not be preserved, so
	   *    can get rendering of spans that are the inverse of what it should be.
	   *  So for now, transfrag graphs and other graphs that use SPAN_GRAPH style
	   *    will be switched over to MINMAXAVG style for the sliced view, which ends up
	   *    showing a bar at the start and end of each span instead of filling across the span.
	   *    Hopefullly will fix soon with some modifications to GraphGlyph and GraphSym2Glyph.
	   */
	  if (new_state.getGraphStyle() == SmartGraphGlyph.SPAN_GRAPH) {
	    new_state.setGraphStyle(SmartGraphGlyph.MINMAXAVG);
	  }
	  spliced_graph_factories.put(gsym, new_factory);
	}
      }
      spliced_view.sliceAndDice(syms);
      orf_analyzer.redoOrfs();
    }
  }

  public static void main(String[] args) {
    // testing AltSpliceView as standalone
    JFrame frm = new JFrame("AltSpliceView test");
    AltSpliceView view = new AltSpliceView();
    Container cpane = frm.getContentPane();
    cpane.setLayout(new BorderLayout());
    cpane.add("Center", view);
    frm.setSize(600, 400);
    frm.show();
    frm.addWindowListener( new WindowAdapter() {
      public void windowClosing(WindowEvent evt) { System.exit(0); }
    });
  }

  // ComponentListener implementation
  public void componentResized(ComponentEvent e) { }
  public void componentMoved(ComponentEvent e) { }
  public void componentShown(ComponentEvent e) {
    //    System.out.println("AltSpliceView was just made visible");
    //    System.out.println("AltSpliceView.isShowing(): " + this.isShowing());
    if (pending_sequence_change && slice_by_selection_on) {
      spliced_view.setAnnotatedSeq(last_seq_changed);
      pending_sequence_change = false;
    }
    if (pending_selection_change)  {
      if (slice_by_selection_on) {
	this.sliceAndDice(last_selected_syms);
      }
      else {
	spliced_view.select(last_selected_syms);
      }
      pending_selection_change = false;
    }
  }
  public void componentHidden(ComponentEvent e) {
    //    System.out.println("AltSpliceView was just hidden");
    //    System.out.println("AltSpliceView.isShowing(): " + this.isShowing());
  }

  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    if (src == buffer_sizeTF) {
      String str = buffer_sizeTF.getText();
      if (str != null) {
	int new_buf_size = Integer.parseInt(str);
	this.setSliceBuffer(new_buf_size, true);
      }
    }
  }


  public void itemStateChanged(ItemEvent evt) {
    Object src = evt.getSource();
    if (src == slice_by_selectionCB) {
      if (evt.getStateChange() == ItemEvent.SELECTED) {
	setSliceBySelection(true);
      }
      else {
	setSliceBySelection(false);
      }
      //System.out.println("toggled selection tracking: " + getSliceBySelection());
    }
  }


}
