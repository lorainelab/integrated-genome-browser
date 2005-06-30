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

package com.affymetrix.igb.genometry;

import java.util.*;

import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.igb.event.GroupSelectionListener;
import com.affymetrix.igb.event.SeqSelectionListener;
import com.affymetrix.igb.event.SymSelectionListener;
import com.affymetrix.igb.event.GroupSelectionEvent;
import com.affymetrix.igb.event.SeqSelectionEvent;
import com.affymetrix.igb.event.SymSelectionEvent;

public class SingletonGenometryModel {
  static public boolean DEBUG = false;
  static SingletonGenometryModel smodel = new SingletonGenometryModel();

  Map seq_groups = new LinkedHashMap();
  // LinkedHashMap preserves the order things were added in, which is nice for QuickLoad

  List seq_selection_listeners = new ArrayList();
  List group_selection_listeners = new ArrayList();
  List sym_selection_listeners = new ArrayList();

  AnnotatedSeqGroup selected_group = null;
  MutableAnnotatedBioSeq selected_seq = null;
  List selected_syms = null;

  public static SingletonGenometryModel getGenometryModel() {
    return smodel;
  }

  public Map getSeqGroups() {
    return seq_groups;
  }

  public AnnotatedSeqGroup getSeqGroup(String group_syn) {
    AnnotatedSeqGroup group = (AnnotatedSeqGroup)seq_groups.get(group_syn);
    if (group == null) {
      // try and find a synonym
      Iterator iter = seq_groups.values().iterator();
      while (iter.hasNext()) {
	AnnotatedSeqGroup curgroup = (AnnotatedSeqGroup)iter.next();
	if (curgroup.isSynonymous(group_syn)) {
	  group = curgroup;
	  break;
	}
      }
    }
    return group;
  }

  /**
   *  Returns the seq group with the given id, creating a new one if there
   *  isn't an existing one.
   *  @return a non-null AnnotatedSeqGroup
   */
  public AnnotatedSeqGroup addSeqGroup(String group_id) {
    //    System.out.println("SingletonGenometryModel.addSeqGroup() called, id = " + group_id);
    // if AnnotatedSeqGroup with same or synonymous id already exists, then return it
    AnnotatedSeqGroup group = getSeqGroup(group_id);
    // otherwise create a new AnnotatedSeqGroup
    if (group == null) {
      //      System.out.println("  adding new seq group: " + group_id);
      group = new AnnotatedSeqGroup(group_id);
      seq_groups.put(group.getID(), group);
    }
    else {
      //      System.out.println("  already have seq group: " + group_id + ", actual id = " + group.getID());
    }
    return group;
  }

  public void addSeqGroup(AnnotatedSeqGroup group) {
    seq_groups.put(group.getID(), group);
  }

  public void removeSeqGroup(AnnotatedSeqGroup group) {
    seq_groups.remove(group.getID());
  }

  public AnnotatedSeqGroup getSelectedSeqGroup() {
    return selected_group;
  }

  public void setSelectedSeqGroup(AnnotatedSeqGroup group) {
    if (DEBUG)  {
      System.out.println("SingletonGenometryModel.setSelectedSeqGroup() called, ");
      System.out.println("    group = " + (group == null ? null : group.getID()));
    }

    selected_group = group;
    ArrayList glist = new ArrayList();
    glist.add(selected_group);
    GroupSelectionEvent evt = new GroupSelectionEvent(this, glist);
    Iterator iter = group_selection_listeners.iterator();
    while (iter.hasNext()) {
      GroupSelectionListener listener = (GroupSelectionListener)iter.next();
      listener.groupSelectionChanged(evt);
    }
  }

  public void addGroupSelectionListener(SeqSelectionListener listener) {
    group_selection_listeners.add(listener);
  }

  public void removeGroupSelectionListener(SeqSelectionListener listener) {
    group_selection_listeners.remove(listener);
  }

  public List getGroupSelectionListeners() {
    return group_selection_listeners;
  }


  public MutableAnnotatedBioSeq getSelectedSeq() {
    return selected_seq;
  }

  public void setSelectedSeq(MutableAnnotatedBioSeq seq) {
    setSelectedSeq(seq, this);
  }

  public void setSelectedSeq(MutableAnnotatedBioSeq seq, Object src) {
    if (DEBUG)  {
      System.out.println("SingletonGenometryModel.setSelectedSeq() called, ");
      System.out.println("    seq = " + (seq == null ? null : seq.getID()));
    }

    selected_seq = seq;
    ArrayList slist = new ArrayList();
    slist.add(selected_seq);
    SeqSelectionEvent evt = new SeqSelectionEvent(src, slist);
    Iterator iter = seq_selection_listeners.iterator();
    while (iter.hasNext()) {
      SeqSelectionListener listener = (SeqSelectionListener)iter.next();
      listener.seqSelectionChanged(evt);
    }
  }


  public void addSeqSelectionListener(SeqSelectionListener listener) {
    seq_selection_listeners.add(listener);
  }

  public void removeSeqSelectionListener(SeqSelectionListener listener) {
    seq_selection_listeners.remove(listener);
  }

  public List getSeqSelectionListeners() {
    return seq_selection_listeners;
  }

  public void addSymSelectionListener(SymSelectionListener listener) {
    sym_selection_listeners.add(listener);
  }

  public void removeSymSelectionListener(SymSelectionListener listener) {
    sym_selection_listeners.remove(listener);
  }

  public List getSymSelectionListeners() {
    return sym_selection_listeners;
  }

  public void setSelectedSymmetries(List syms)  {
    setSelectedSymmetries(syms, this);
  }

  public void setSelectedSymmetries(List syms, Object src) {
    if (DEBUG)  {
      System.out.println("SingletonGenometryModel.setSelectedSymmetries() called, ");
      System.out.println("    syms = " + syms);
    }
    selected_syms = syms;
    SymSelectionEvent sevt = new SymSelectionEvent(src, selected_syms);
    for (int i=0; i<sym_selection_listeners.size(); i++) {
      SymSelectionListener listener = (SymSelectionListener)sym_selection_listeners.get(i);
      listener.symSelectionChanged(sevt);
    }
  }

  public List getSelectedSymmetries() {
    return selected_syms;
  }
}
