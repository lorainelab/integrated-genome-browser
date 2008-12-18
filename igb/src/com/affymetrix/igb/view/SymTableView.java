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

package com.affymetrix.igb.view;

import java.util.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.event.*;

public class SymTableView extends PropertySheet implements SymSelectionListener {
  static int testcount = 0;
  Vector<String> default_order;

  public SymTableView() {
    this(true, true);
  }

  public SymTableView(boolean useDefaultKeystrokes) {
    this(useDefaultKeystrokes, true);
  }

  public SymTableView(boolean useDefaultKeystrokes, boolean isSymSelectionListener) {
    super(useDefaultKeystrokes);
    setPreferredSize(new java.awt.Dimension(100, 250));
    setMinimumSize(new java.awt.Dimension(100, 250));
    if (isSymSelectionListener) {
      SingletonGenometryModel.getGenometryModel().addSymSelectionListener(this);
    }
    default_order = new Vector<String>(8);
    default_order.add("gene name");
    default_order.add("name");
    default_order.add("id");
    default_order.add("type");
    default_order.add("start");
    default_order.add("end");
    default_order.add("length");
  }
  
  public void setDefaultColumnOrder(List<String> columns) {
    default_order = new Vector<String>(columns);
  }

  public void symSelectionChanged(SymSelectionEvent evt) {
    Object src = evt.getSource();
    // if selection event originally came from here, then ignore it...
    if (src == this) { return; }
    List<SeqSymmetry> selected_syms = evt.getSelectedSyms();
    SeqMapView mapView = null;
    if (src instanceof SeqMapView) {
       mapView = (SeqMapView) src;
    }
    showSyms(selected_syms, mapView);
  }
  
  List<SeqSymmetry> currentSyms = Collections.<SeqSymmetry>emptyList();

  public void showSyms(List<SeqSymmetry> selected_syms, SeqMapView seqMap) {

    currentSyms = selected_syms;
    
    int symCount = selected_syms.size();
    Vector<Map<String,Object>> propvec = new Vector<Map<String,Object>>();
    for (int i=0; i<symCount; i++) {
      SeqSymmetry sym = selected_syms.get(i);
      Map<String,Object> props = null;
      if (sym instanceof SymWithProps) {
	// using Propertied.cloneProperties() here instead of Propertied.getProperties()
	//   because adding start, end, id, and length as additional key-val pairs to props Map
	//   and don't want these to bloat up sym's properties
        props = ((SymWithProps)sym).cloneProperties();
      }
      if (props == null && sym instanceof DerivedSeqSymmetry) {
        SeqSymmetry original_sym = ((DerivedSeqSymmetry) sym).getOriginalSymmetry();
        if (original_sym instanceof SymWithProps) {
          props = ((SymWithProps) original_sym).cloneProperties();
        }
      }
      if (props == null) {
	// make an empty hashtable if sym has no properties...
	props = new Hashtable<String,Object>();
      }
      String symid = sym.getID();
      if (symid != null)  {
        props.put("id", symid);
      }
      if (seqMap != null) {
        SeqSpan span = seqMap.getViewSeqSpan(sym);
	if (span != null) {
          String chromID = span.getBioSeq().getID();
          props.put("chr", chromID);
	  props.put("start", String.valueOf(span.getStart()));
	  props.put("end", String.valueOf(span.getEnd()));
	  props.put("length", String.valueOf(span.getLength()));
          props.remove("seq id"); // this is redundant if "chromosome" proprety is set
          if (props.containsKey("method") && ! props.containsKey("type")) {
            props.put("type", props.get("method"));
            props.remove("method");
          }
	}
      }
      testcount++;
      propvec.add(props);
    }
    Map[] prop_array = propvec.toArray(new Map[propvec.size()]);
    
    this.showProperties(prop_array, default_order, "");
  }

  public static void printMap(Map hash)  {
    Iterator iter = hash.entrySet().iterator();
    while (iter.hasNext())  {
      System.out.println(iter.next());
    }

  }
  
  public List<SeqSymmetry> getCurrentSyms() {
    return new ArrayList<SeqSymmetry>(currentSyms);
  }
  
  public SeqSymmetry getSymForRow(int i) {
    return currentSyms.get(getModelIndex(i));
  }
  
  public void destroy() {
    currentSyms = Collections.<SeqSymmetry>emptyList();
  }
}


