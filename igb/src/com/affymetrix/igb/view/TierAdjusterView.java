/**
*   Copyright (c) 2001-2005 Affymetrix, Inc.
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
import javax.swing.event.*;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genometry.*;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.glyph.*;
import com.affymetrix.igb.tiers.*;
import com.affymetrix.igb.event.*;
import com.affymetrix.igb.IGB;

/**
 *  A view that allows adjusting the visibility of annotations in selected tiers 
 *    based on a number of different filters.
 */
public class TierAdjusterView extends JComponent implements ChangeListener {
  JTextField totalTF = new JTextField(6);
  JTextField passTF = new JTextField(6);
  JTextField missTF = new JTextField(6);
  TierLabelManager tier_manager;
  SeqMapView gviewer;
  AffyTieredMap map;
  java.util.List filters = new ArrayList();

  public TierAdjusterView() {

    filters.add(new LengthFilter());
    filters.add(new ChildCountFilter());
    filters.add(new ChildCoverageFilter());
    filters.add(new OtherCoverageFilter());

    javax.swing.Box holder = javax.swing.Box.createVerticalBox();
    this.setLayout(new FlowLayout());
    this.add(holder);

    for (int i=0; i<filters.size(); i++) {
      SymmetryFilter filter = (SymmetryFilter)filters.get(i);
      filter.addChangeListener(this);
      if (filter instanceof JComponent) {
	holder.add((JComponent)filter);
      }
    }

    javax.swing.Box result_comp = javax.swing.Box.createHorizontalBox();
    result_comp.add(new JLabel("Annot Count: "));
    result_comp.add(totalTF);
    result_comp.add(new JLabel("Passed Filters: "));
    result_comp.add(passTF);
    result_comp.add(new JLabel("Filtered Out: "));
    result_comp.add(missTF);
    holder.add(result_comp);

    gviewer = IGB.getSingletonIGB().getMapView();
    tier_manager = gviewer.getTierManager();
    map = gviewer.getSeqMap();

  }

  public void stateChanged(ChangeEvent evt) {
    runFilters();
  }

  public void runFilters() {
    BioSeq aseq = gviewer.getAnnotatedSeq();
    java.util.List tiers = tier_manager.getSelectedTiers();
    int tier_count = tiers.size();
    //    System.out.println("selected tier count: " + tier_count);
    int pass_count = 0;
    int miss_count = 0;
    int filter_count = filters.size();
    for (int i=0; i<tier_count; i++) {
      TierGlyph tier = (TierGlyph)tiers.get(i);
      java.util.List glyphs = tier.getChildren();
      int gcount = glyphs.size();
      for (int k=0; k<gcount; k++) {
	GlyphI gl = tier.getChild(k);
	if (gl.getInfo() instanceof SeqSymmetry) {
	  SeqSymmetry psym = (SeqSymmetry)gl.getInfo();
	  boolean passed_filters = true;
	  for (int m=0; m<filter_count; m++) {
	    SymmetryFilter filt = (SymmetryFilter)filters.get(m);
	    if (! filt.passesFilter(psym, aseq)) {
	      passed_filters = false;
	      break;
	    }
	  }
	  if (passed_filters) {
	    gl.setVisibility(true);
	    pass_count++;
	  }
	  else {
	    gl.setVisibility(false);
	    miss_count++;
	  }
	}
      }
    }
    int total_count = pass_count + miss_count;
    totalTF.setText(Integer.toString(total_count));
    passTF.setText(Integer.toString(pass_count));
    missTF.setText(Integer.toString(miss_count));
    map.updateWidget();
  }

}

interface SymmetryFilter {
  public boolean passesFilter(SeqSymmetry sym);
  public boolean passesFilter(SeqSymmetry sym, BioSeq seq);
  public void addChangeListener(ChangeListener listener);
  public void removeChangeListener(ChangeListener listener);
}

abstract class AbstractSymFilter extends JComponent implements SymmetryFilter  {
  java.util.List listeners = new ArrayList();
  public void addChangeListener(ChangeListener listener) {
    listeners.add(listener);
  }
  public void removeChangeListener(ChangeListener listener) {
    listeners.remove(listener);
  }
  public void notifyListeners() {
    ChangeEvent evt = new ChangeEvent(this);
    for (int i=0; i<listeners.size(); i++) {
      ChangeListener listener = (ChangeListener)listeners.get(i);
      listener.stateChanged(evt);
    }
  }
}


class LengthFilter extends AbstractSymFilter implements ActionListener  {
  JTextField max_lengthTF = new JTextField(12);
  JTextField min_lengthTF = new JTextField(12);
  int min_length = 0;
  int max_length = Integer.MAX_VALUE;
  boolean filter_off = true;

  public LengthFilter() {
    this.setLayout(new FlowLayout());
    this.add(new JLabel("min length: "));
    this.add(min_lengthTF);
    this.add(new JLabel("max length: "));
    this.add(max_lengthTF);

    min_lengthTF.addActionListener(this);
    max_lengthTF.addActionListener(this);
  }

  public final boolean passesFilter(SeqSymmetry sym)  {
    return passesFilter(sym, sym.getSpanSeq(0));
  }
  public final boolean passesFilter(SeqSymmetry sym, BioSeq seq) {
    if (filter_off) { return true; }
    else  {
      SeqSpan span = sym.getSpan(seq);
      if (span == null) { return false; }
      int length = span.getLength();
      return ( (length >= min_length) && (length <= max_length) );
    }
  }

  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    if (src == min_lengthTF) {
      try {
	min_length = Integer.parseInt(min_lengthTF.getText());
      }
      catch (Exception ex) {
	min_length = 0;
	min_lengthTF.setText("");
      }
    }
    else if (src == max_lengthTF) {
      try {
	max_length = Integer.parseInt(max_lengthTF.getText());
      }
      catch (Exception ex) {
	max_length = Integer.MAX_VALUE;
	max_lengthTF.setText("");
      }
    }
    filter_off = ((min_length == 0) && (max_length == Integer.MAX_VALUE));
    notifyListeners();
  }
}

class ChildCountFilter extends AbstractSymFilter implements ActionListener {
  JTextField min_childrenTF = new JTextField(4);
  JTextField max_childrenTF = new JTextField(4);
  int min_children = 0;
  int max_children = Integer.MAX_VALUE;
  boolean filter_off = true;

  public ChildCountFilter() {
    this.setLayout(new FlowLayout());
    this.add(new JLabel("min chilren: "));
    this.add(min_childrenTF);
    this.add(new JLabel("max children: "));
    this.add(max_childrenTF);
    min_childrenTF.addActionListener(this);
    max_childrenTF.addActionListener(this);
  }

  public final boolean passesFilter(SeqSymmetry sym)  {
    return passesFilter(sym, sym.getSpanSeq(0));
  }
  public final boolean passesFilter(SeqSymmetry sym, BioSeq seq) {
    if (filter_off) { return true; }
    else  {
      int child_count = sym.getChildCount();
      // hmm, should no children be considered 1 child (itself)?  For now leaving as 0
      return ( (child_count >= min_children) && (child_count <= max_children) );
    }
  }

  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    if (src == min_childrenTF) {
      try {
	min_children = Integer.parseInt(min_childrenTF.getText());
      }
      catch (Exception ex) {
	min_children = 0;
	min_childrenTF.setText("");
      }
    }
    else if (src == max_childrenTF) {
      try {
	max_children = Integer.parseInt(max_childrenTF.getText());
      }
      catch (Exception ex) {
	max_children = Integer.MAX_VALUE;
	max_childrenTF.setText("");
      }
    }
    filter_off = ((min_children == 0) && (max_children == Integer.MAX_VALUE));
    notifyListeners();
  }
}


class ChildCoverageFilter extends AbstractSymFilter implements ActionListener {
  JTextField min_coverageTF = new JTextField(4);
  JTextField max_coverageTF = new JTextField(4);
  float min_coverage = 0;
  // max_coverage should really never be > 1.0, but since not merging children, if children overlap
  //    could actually get "coverage" > 1.0 -- for most annotations though, leaf spans will _not_ overlap
  float max_coverage = Float.POSITIVE_INFINITY;
  boolean filter_off = true;

  public ChildCoverageFilter() {
    this.setLayout(new FlowLayout());
    this.add(new JLabel("min % coverage: "));
    this.add(min_coverageTF);
    this.add(new JLabel("max % coverage: "));
    this.add(max_coverageTF);
    min_coverageTF.addActionListener(this);
    max_coverageTF.addActionListener(this);
  }

  public final boolean passesFilter(SeqSymmetry sym)  {
    return passesFilter(sym, sym.getSpanSeq(0));
  }
  public final boolean passesFilter(SeqSymmetry sym, BioSeq seq) {
    if (filter_off) { return true; }
    else  {
      int child_count = sym.getChildCount();
      SeqSpan pspan = sym.getSpan(seq);
      float covered_coords = 0;
      float total_coords = pspan.getLength();
      for (int m=0; m<child_count; m++) {
	SeqSymmetry child = sym.getChild(m);
	SeqSpan cspan = child.getSpan(seq);
	covered_coords += cspan.getLength();
      }
      float coverage = (covered_coords / total_coords) * 100;
      return ( (coverage >= min_coverage) && (coverage <= max_coverage) );
    }
  }

  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    if (src == min_coverageTF) {
      try {
	min_coverage = Float.parseFloat(min_coverageTF.getText());
      }
      catch (Exception ex) {
	min_coverage = 0;
	min_coverageTF.setText("");
      }
    }
    else if (src == max_coverageTF) {
      try {
	max_coverage = Float.parseFloat(max_coverageTF.getText());
      }
      catch (Exception ex) {
	max_coverage = Float.POSITIVE_INFINITY;
	max_coverageTF.setText("");
      }
    }
    filter_off = ( (min_coverage == 0) && (max_coverage == Float.POSITIVE_INFINITY) );
    notifyListeners();
  }
}

/**
 *  passes symA through filter only if % leaf coverage of (another_ set of syms (symlistB) over the
 *   length of symA is between min_other_coverage and max_other_coverage
 *
 *  NOT YET IMPLEMENTED
 */
class OtherCoverageFilter extends AbstractSymFilter implements ActionListener {
  JTextField min_other_coverageTF = new JTextField(4);
  JTextField max_other_coverageTF = new JTextField(4);
  JTextField expandTF = new JTextField(6);
  JComboBox trackCB = new JComboBox();
  JButton refreshB = new JButton("refresh");
  SeqMapView gviewer = IGB.getSingletonIGB().getMapView();
  AffyTieredMap map = gviewer.getSeqMap();
  SeqSpanComparator span_comp = new SeqSpanComparator();

  float min_other_coverage = 0;
  float max_other_coverage = Float.POSITIVE_INFINITY;
  int expand_length = 0;
  boolean filter_off = true;
  Map item2tier = new HashMap();
  //  java.util.List other_span_list = new ArrayList();
  int[] other_mins = new int[0];
  int[] other_maxs = new int[0];

  public OtherCoverageFilter() {
    this.setLayout(new FlowLayout());
    this.add(new JLabel("other track: "));
    this.add(trackCB);
    this.add(refreshB);

    this.add(new JLabel("min other track % coverage: "));
    this.add(min_other_coverageTF);
    this.add(new JLabel("max other track % coverage: "));
    this.add(max_other_coverageTF);
    this.add(new JLabel("  expand length to: "));
    this.add(expandTF);

    trackCB.addItem("This");
    trackCB.addItem("is a");
    trackCB.addItem("test");
    trackCB.addActionListener(this);
    refreshB.addActionListener(this);
    min_other_coverageTF.addActionListener(this);
    max_other_coverageTF.addActionListener(this);
    expandTF.addActionListener(this);
  }

  public final boolean passesFilter(SeqSymmetry sym)  {
    return passesFilter(sym, sym.getSpanSeq(0));
  }
  public final boolean passesFilter(SeqSymmetry sym, BioSeq seq) {
    if (filter_off || other_mins.length == 0) { return true; }
    else  {
      SeqSpan pspan = sym.getSpan(seq);
      int pmin = pspan.getMin();
      int pmax = pspan.getMax();
      int total_coords = pspan.getLength();
      if (total_coords < expand_length) {
	BioSeq aseq = gviewer.getAnnotatedSeq();
	int center = pmin + (total_coords/2);
	pmin = Math.max( 0, (center - (expand_length/2)) );
	pmax = Math.min( aseq.getLength(), (center + (expand_length/2)) );
	total_coords = pmax - pmin;
      }
      int nearest_index = Arrays.binarySearch(other_mins, pmin);
      if (nearest_index < 0) {
	nearest_index = -nearest_index -1;
	if (nearest_index < 0) { nearest_index = 0; }
      }
      int curindex = nearest_index;
      int curmin;
      int curmax;
      float covered_coords = 0;
      while ( (curindex < other_mins.length-1) &&
	      ((curmin = other_mins[curindex]) < pmax) ) {
	curmax = other_maxs[curindex];
	if (curmax > pmax) { curmax = pmax; }
	covered_coords += (curmax - curmin);
	curindex++;
      }
      float coverage = (covered_coords / total_coords) * 100;
      return ( (coverage >= min_other_coverage) && (coverage <= max_other_coverage) );
    }
  }

  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    if (src == min_other_coverageTF) {
      try {
	min_other_coverage = Float.parseFloat(min_other_coverageTF.getText());
      }
      catch (Exception ex) {
	min_other_coverage = 0;
	min_other_coverageTF.setText("");
      }
    }
    else if (src == max_other_coverageTF) {
      try {
	max_other_coverage = Float.parseFloat(max_other_coverageTF.getText());
      }
      catch (Exception ex)  {
	max_other_coverage = Float.POSITIVE_INFINITY;
	max_other_coverageTF.setText("");
      }
    }
    else if (src == expandTF) {
      try {
	expand_length = Integer.parseInt(expandTF.getText());
      }
      catch (Exception ex) {
	expand_length = 0;
	expandTF.setText("");
      }
    }
    else if (src == trackCB) {
      System.out.println("action detected on track choice box");
      redoTrack();
    }
    else if (src == refreshB) {
      System.out.println("refresh button pushed");
      trackCB.removeAllItems();
      item2tier.clear();
      java.util.List tiers = map.getAllTiers();
      for (int i=0; i<tiers.size(); i++) {
	TierGlyph tgl = (TierGlyph)tiers.get(i);
	String label = tgl.getLabel();
	if (label != null) {
	  trackCB.addItem(label);
	  item2tier.put(label, tgl);
	}
      }
    }
    filter_off = ( (min_other_coverage == 0) && (max_other_coverage == Float.POSITIVE_INFINITY) );
    notifyListeners();
  }

  protected void redoTrack() {
    BioSeq aseq = gviewer.getAnnotatedSeq();
    String label = (String)trackCB.getSelectedItem();
    TierGlyph tgl = (TierGlyph)item2tier.get(label);
    if (tgl == null)  { System.out.println("no track found for label: " + label); return; }
    System.out.println("other track selected: " + label + ", " + tgl);
    // this assumes "other" tier's glyphs are sorted...
    // get all child glyphs of other tier,
    //   get SeqSymmetries from tier's child glyphs
    //   collect leaf spans from SeqSymmetries
    //   covert to min and max arrays??
    // Then in passesFilter the leaf spans (or min/max arrays) can be binary-searched
    //   to quickly find spans that overlap with the sym in question
    //   to calculate coverage
    int child_count = tgl.getChildCount();
    java.util.List other_span_list = new ArrayList(child_count);
    for (int i=0; i<child_count; i++) {
      GlyphI gl = tgl.getChild(i);
      Object obj = gl.getInfo();
      if (obj instanceof SeqSymmetry) {
	SeqSymmetry sym = (SeqSymmetry)obj;
	SeqUtils.collectLeafSpans(sym, aseq, other_span_list);
      }
    }
    // sorting just to make sure...
    Collections.sort(other_span_list, new SeqSpanComparator());
    int span_count = other_span_list.size();
    other_mins = new int[span_count];
    other_maxs = new int[span_count];
    for (int i=0; i<span_count; i++) {
      SeqSpan span = (SeqSpan)other_span_list.get(i);
      other_mins[i] = span.getMin();
      other_maxs[i] = span.getMax();
    }
    System.out.println("span count: " + span_count);
  }

}
