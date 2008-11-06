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

package com.affymetrix.igb.tiers;

import java.util.*;
import java.util.List;
import java.awt.event.*;
import javax.swing.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.SeqSymSummarizer;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.util.ErrorHandler;
import com.affymetrix.igb.view.AnnotatedSeqViewer;


/**
 *  A PopupListener that adds the ability to create "union", "intersection", etc.,
 *  tiers based on selected annotation tiers.  Is not used on graph tiers.
 */
public class TierArithmetic implements TierLabelManager.PopupListener {

  static final boolean DEBUG = false;

  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

  AnnotatedSeqViewer gviewer;
  TierLabelManager handler;

  JMenu combineMenu = new JMenu("Combine Selected Tiers");
  JMenuItem intersectMI = new JMenuItem("Intersect");
  JMenuItem unionMI = new JMenuItem("Union");
  JMenuItem a_not_b_MI = new JMenuItem("A not B");
  JMenuItem b_not_a_MI = new JMenuItem("B not A");
  JMenuItem xorMI = new JMenuItem("Xor");
  JMenuItem notMI = new JMenuItem("Not");

  public TierArithmetic(TierLabelManager handler, AnnotatedSeqViewer gviewer) {
    this.handler = handler;
    this.gviewer = gviewer;


    intersectMI.addActionListener(this.action_listener);
    unionMI.addActionListener(this.action_listener);
    a_not_b_MI.addActionListener(this.action_listener);
    b_not_a_MI.addActionListener(this.action_listener);
    xorMI.addActionListener(this.action_listener);
    notMI.addActionListener(this.action_listener);

    combineMenu.add(intersectMI);
    combineMenu.add(unionMI);
    combineMenu.add(a_not_b_MI);
    combineMenu.add(b_not_a_MI);
    combineMenu.add(xorMI);
    combineMenu.add(notMI);
  }

  public void addUnionTier() {
    List selected = handler.getSelectedTiers();
    if (selected.size() >= 1) {
      addUnionTier(selected);
    }
    else {
      ErrorHandler.errorPanel("Must select one or more annotation tiers for union");
    }
  }

  public void addExclusiveTier(boolean exclusiveA) {
    List selected = handler.getSelectedTiers();
    if (selected.size() == 2) {
      TierGlyph tierA = (TierGlyph) selected.get(0);
      TierGlyph tierB = (TierGlyph) selected.get(1);
      addExclusiveTier(tierA, tierB, exclusiveA);
    }
    else {
      ErrorHandler.errorPanel("Must select two and only two tiers for union");
    }
  }


  public void addXorTier() {
    List selected = handler.getSelectedTiers();
    if (selected.size() == 2) {
      TierGlyph tierA = (TierGlyph) selected.get(0);
      TierGlyph tierB = (TierGlyph) selected.get(1);
      addXorTier(tierA, tierB);
    }
    else {
      ErrorHandler.errorPanel("Must select two and only two tiers for XOR(A,B)");
    }
  }

  public void addNotTier() {
    List selected = handler.getSelectedTiers();
    if (selected.size() == 1) {
      TierGlyph tierA = (TierGlyph) selected.get(0);
      addNotTier(tierA);
    }
    else {
      ErrorHandler.errorPanel("Must select one and only one tier for NOT(A)");
    }
  }


  public void addIntersectTier() {
    List selected = handler.getSelectedTiers();
    if (selected.size() == 2) {
      TierGlyph tierA = (TierGlyph) selected.get(0);
      TierGlyph tierB = (TierGlyph) selected.get(1);
      addIntersectTier(tierA, tierB);
    }
    else {
      ErrorHandler.errorPanel("Must select two and only two tiers for intersection");
    }
  }


  public void addNotTier(TierGlyph tierA) {
    //    int index = handler.getTierIndex(tierA);
    String method = "not: " + tierA.getLabel();
    SeqSymmetry tempsym = (SeqSymmetry)tierA.getChild(0).getInfo();
    MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)gmodel.getSelectedSeq();
    List listA = new ArrayList();
    for (int i=0; i<tierA.getChildCount(); i++) {
      GlyphI child = tierA.getChild(i);
      SeqSymmetry csym = (SeqSymmetry)child.getInfo();
      if (csym != null) {
	listA.add(csym);
      }
    }
    if (listA.isEmpty()) {
      ErrorHandler.errorPanel("Illegal Operation",
          "Cannot perform this operation on this tier.");
      return;
    }
    SeqSymmetry inverse_sym = SeqSymSummarizer.getNot(listA, aseq);
    if (inverse_sym != null) {
      System.out.println("not result count: " + inverse_sym.getChildCount());
      makeNonPersistentStyle((SymWithProps)inverse_sym, method);
      aseq.addAnnotation(inverse_sym);
      gviewer.setAnnotatedSeq(aseq, true, true);
    }
  }


  public void addExclusiveTier(TierGlyph tierA, TierGlyph tierB, boolean exclusiveA) {
    //    int index = handler.getTierIndex(tierB);
    String method;
    if (exclusiveA) {
      method = "A not B:" + tierA.getLabel() + ", " + tierB.getLabel();
    }
    else {
      method = "B not A:" + tierB.getLabel() + ", " + tierA.getLabel();
    }
    SeqSymmetry tempsym = (SeqSymmetry)tierA.getChild(0).getInfo();
    MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)gmodel.getSelectedSeq();
    List listA = new ArrayList();
    List listB = new ArrayList();

    for (int i=0; i<tierA.getChildCount(); i++) {
      GlyphI child = tierA.getChild(i);
      SeqSymmetry csym = (SeqSymmetry)child.getInfo();
      if (csym != null) {
	listA.add(csym);
      }
    }
    for (int i=0; i<tierB.getChildCount(); i++) {
      GlyphI child = tierB.getChild(i);
      SeqSymmetry csym = (SeqSymmetry)child.getInfo();
      if (csym != null) {
	listB.add(csym);
      }
    }
    SeqSymmetry exclusive_sym;
    if (exclusiveA) {
      exclusive_sym = SeqSymSummarizer.getExclusive(listA, listB, aseq);
    }
    else {
      exclusive_sym = SeqSymSummarizer.getExclusive(listB, listA, aseq);
    }
    if (exclusive_sym != null) {
      System.out.println("exclusive result children: " + exclusive_sym.getChildCount());
      makeNonPersistentStyle((SymWithProps)exclusive_sym, method);
      aseq.addAnnotation(exclusive_sym);
      gviewer.setAnnotatedSeq(aseq, true, true);
    }
  }


  public void addXorTier(TierGlyph tierA, TierGlyph tierB) {
    //    int index = handler.getTierIndex(tierB);
    String method = "xor: " + tierA.getLabel() + ", " + tierB.getLabel();
    SeqSymmetry tempsym = (SeqSymmetry)tierA.getChild(0).getInfo();
    MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)gmodel.getSelectedSeq();
    List listA = new ArrayList();
    List listB = new ArrayList();

    for (int i=0; i<tierA.getChildCount(); i++) {
      GlyphI child = tierA.getChild(i);
      SeqSymmetry csym = (SeqSymmetry)child.getInfo();
      if (csym != null) {
	listA.add(csym);
      }
    }
    for (int i=0; i<tierB.getChildCount(); i++) {
      GlyphI child = tierB.getChild(i);
      SeqSymmetry csym = (SeqSymmetry)child.getInfo();
      if (csym != null) {
	listB.add(csym);
      }
    }
    SeqSymmetry xor_sym = SeqSymSummarizer.getXor(listA, listB, aseq);
    if (xor_sym != null) {
      System.out.println("xor result count: " + xor_sym.getChildCount()); 
      makeNonPersistentStyle((SymWithProps)xor_sym, method);
      aseq.addAnnotation(xor_sym);
      gviewer.setAnnotatedSeq(aseq, true, true);
    }
  }


  public void addUnionTier(List tiers) {
    StringBuffer meth = new StringBuffer();
    meth.append("union: ");
    MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)gmodel.getSelectedSeq();
    List syms = new ArrayList();
    for (int t=0; t<tiers.size(); t++) {
      TierGlyph tier = (TierGlyph) tiers.get(t);
      meth.append(tier.getLabel() + ", ");
      for (int i=0; i<tier.getChildCount(); i++) {
        GlyphI child = tier.getChild(i);
        SeqSymmetry csym = (SeqSymmetry)child.getInfo();
        if (csym != null) { syms.add(csym); }
      }
    }
    SeqSymmetry union_sym = SeqSymSummarizer.getUnion(syms, aseq);
    if (union_sym != null) {
      System.out.println("union result count: " + union_sym.getChildCount());
      makeNonPersistentStyle((SymWithProps)union_sym, meth.toString());
      aseq.addAnnotation(union_sym);
      gviewer.setAnnotatedSeq(aseq, true, true);
    }
  }


  public void addIntersectTier(TierGlyph tierA, TierGlyph tierB) {
    //    int index = handler.getTierIndex(tierB);
    String method = "intersect: " + tierA.getLabel() + ", " + tierB.getLabel();

    MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)gmodel.getSelectedSeq();
    List listA = new ArrayList();
    List listB = new ArrayList();
    for (int i=0; i<tierA.getChildCount(); i++) {
      GlyphI child = tierA.getChild(i);
      SeqSymmetry csym = (SeqSymmetry)child.getInfo();
      if (csym != null) {
	listA.add(csym);
      }
    }
    for (int i=0; i<tierB.getChildCount(); i++) {
      GlyphI child = tierB.getChild(i);
      SeqSymmetry csym = (SeqSymmetry)child.getInfo();
      if (csym != null) {
	listB.add(csym);
      }
    }
    SeqSymmetry intersect_sym = SeqSymSummarizer.getIntersection(listA, listB, aseq);
    if (intersect_sym != null) {
      System.out.println("intersect result count: " + intersect_sym.getChildCount());
      makeNonPersistentStyle((SymWithProps)intersect_sym, method);
      aseq.addAnnotation(intersect_sym);
      gviewer.setAnnotatedSeq(aseq, true, true);
    }

  }

  public void popupNotify(javax.swing.JPopupMenu popup, TierLabelManager handler) {
    if (handler != this.handler) {
      throw new RuntimeException("");
    }
    List labels = handler.getSelectedTierLabels();
    int num_selected = labels.size();

    boolean all_are_annotations = true;
    for (int i=0; i<num_selected; i++) {
      TierLabelGlyph tlg = (TierLabelGlyph) labels.get(i);
      if (tlg.getReferenceTier().getAnnotStyle().isGraphTier()) {
        all_are_annotations = false;
        break;
      }
    }

    intersectMI.setEnabled(all_are_annotations && num_selected==2);
    unionMI.setEnabled(all_are_annotations && num_selected > 0);
    a_not_b_MI.setEnabled(all_are_annotations && num_selected==2);
    b_not_a_MI.setEnabled(all_are_annotations && num_selected==2);
    notMI.setEnabled(all_are_annotations && num_selected==1);
    xorMI.setEnabled(all_are_annotations && num_selected==2);
    combineMenu.setEnabled(all_are_annotations && num_selected > 0);

    popup.add(combineMenu);
  }

  public static AnnotStyle makeNonPersistentStyle(SymWithProps sym, String human_name) {
    // Needs a unique name so that if any later tier is produced with the same
    // human name, it will not automatically get the same color, etc.
    String unique_name = AnnotStyle.getUniqueName(human_name);
    sym.setProperty("method", unique_name);
    AnnotStyle style = AnnotStyle.getInstance(unique_name, false);
    style.setHumanName(human_name);
    style.setGlyphDepth(1);
    style.setSeparate(false); // there are not separate (+) and (-) strands
    style.setCustomizable(false); // the user can change the color, but not much else is meaningful
    System.out.println("making non-persistent style, id = " + unique_name + ", name = " + human_name);
    return style;
  }

  ActionListener action_listener = new ActionListener() {
    public void actionPerformed(ActionEvent e) {
      Object src = e.getSource();

      if (src == intersectMI) {
        addIntersectTier();
      }
      else if (src == unionMI) {
        addUnionTier();
      }
      else if (src == a_not_b_MI) {
        addExclusiveTier(true);
      }
      else if (src == b_not_a_MI) {
        addExclusiveTier(false);
      }
      else if (src == xorMI) {
        addXorTier();
      }
      else if (src == notMI) {
        addNotTier();
      }
    }
  };

}
