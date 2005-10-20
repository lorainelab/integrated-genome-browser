package com.affymetrix.igb.tiers;

import java.util.*;
import java.awt.event.*;
import javax.swing.*;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genometry.*;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.util.ErrorHandler;
import com.affymetrix.igb.view.AnnotatedSeqViewer;


/**
 *  A PopupListener that adds the ability to create "union", "intersection", etc.,
 *  tiers based on selected tiers.
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
    java.util.List selected = handler.getSelectedTiers();
    if (selected.size() == 2) {
      TierGlyph tierA = (TierGlyph) selected.get(0);
      TierGlyph tierB = (TierGlyph) selected.get(1);
      addUnionTier(tierA, tierB);
    }
    else {
      ErrorHandler.errorPanel("Must select two and only two tiers for union");
    }
  }

  public void addExclusiveTier(boolean exclusiveA) {
    java.util.List selected = handler.getSelectedTiers();
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
    java.util.List selected = handler.getSelectedTiers();
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
    java.util.List selected = handler.getSelectedTiers();
    if (selected.size() == 1) {
      TierGlyph tierA = (TierGlyph) selected.get(0);
      addNotTier(tierA);
    }
    else {
      ErrorHandler.errorPanel("Must select one and only one tier for NOT(A)");
    }
  }


  public void addIntersectTier() {
    java.util.List selected = handler.getSelectedTiers();
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
    int index = handler.getTierIndex(tierA);
    SimpleSymWithProps wrapperSym = new SimpleSymWithProps();
    wrapperSym.setProperty("method", ("not: " + tierA.getLabel()));
    //    MutableSeqSymmetry symA = new SimpleMutableSeqSymmetry();
    SeqSymmetry tempsym = (SeqSymmetry)tierA.getChild(0).getInfo();
    MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)gmodel.getSelectedSeq();
    java.util.List listA = new ArrayList();
    for (int i=0; i<tierA.getChildCount(); i++) {
      GlyphI child = tierA.getChild(i);
      SeqSymmetry csym = (SeqSymmetry)child.getInfo();
      if (csym != null) {
	//	symA.addChild(csym);
	listA.add(csym);
      }
    }
    if (DEBUG) {
    System.out.println("listA children: " + listA.size());
    }
    //    listA.add(symA);
    //    SeqSymmetry inverse_sym = SeqUtils.inverse(symA, aseq);
    SeqSymmetry inverse_sym = SeqSymSummarizer.getNot(listA, aseq);
    if (DEBUG) {System.out.println("inverseSym children: " + inverse_sym.getChildCount());}

    makeNonPersistentStyle(wrapperSym);
    wrapperSym.addChild(inverse_sym);
    aseq.addAnnotation(wrapperSym);
    gviewer.setAnnotatedSeq(aseq, true, true);
  }


  public void addExclusiveTier(TierGlyph tierA, TierGlyph tierB, boolean exclusiveA) {
    int index = handler.getTierIndex(tierB);
    SimpleSymWithProps wrapperSym = new SimpleSymWithProps();
    if (exclusiveA) {
      wrapperSym.setProperty("method", ("A not B:" + tierA.getLabel() + ", " + tierB.getLabel()) );
    }
    else {
      wrapperSym.setProperty("method", ("B not A:" + tierB.getLabel() + ", " + tierA.getLabel()) );
    }

    //    MutableSeqSymmetry symA = new SimpleMutableSeqSymmetry();
    //    MutableSeqSymmetry symB = new SimpleMutableSeqSymmetry();
    SeqSymmetry tempsym = (SeqSymmetry)tierA.getChild(0).getInfo();
    MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)gmodel.getSelectedSeq();
    java.util.List listA = new ArrayList();
    java.util.List listB = new ArrayList();

    //    BioSeq iseq = tempsym.getSpan(0).getBioSeq();
    for (int i=0; i<tierA.getChildCount(); i++) {
      GlyphI child = tierA.getChild(i);
      SeqSymmetry csym = (SeqSymmetry)child.getInfo();
      if (csym != null) {
	listA.add(csym);
	//	symA.addChild(csym);
	//	SeqUtils.printSymmetry(csym);
      }
    }
    for (int i=0; i<tierB.getChildCount(); i++) {
      GlyphI child = tierB.getChild(i);
      SeqSymmetry csym = (SeqSymmetry)child.getInfo();
      if (csym != null) {
	listB.add(csym);
	//	symB.addChild(csym);
	//	SeqUtils.printSymmetry(csym);
      }
    }
    if (DEBUG) {
    System.out.println("listA children: " + listA.size());
    System.out.println("listB children: " + listB.size());
    }
    SeqSymmetry exclusive_sym;
    //    listA.add(symA);
    //    listB.add(symB);
    if (exclusiveA) {
      //      exclusive_sym = SeqUtils.exclusive(symA, symB, aseq);
      exclusive_sym = SeqSymSummarizer.getExclusive(listA, listB, aseq);
    }
    else {
      //      exclusive_sym = SeqUtils.exclusive(symB, symA, aseq);
      exclusive_sym = SeqSymSummarizer.getExclusive(listB, listA, aseq);
    }
    if (DEBUG) {
    System.out.println("exclusive_sym children: " + exclusive_sym.getChildCount());
    }

    makeNonPersistentStyle(wrapperSym);
    wrapperSym.addChild(exclusive_sym);
    aseq.addAnnotation(wrapperSym);
    gviewer.setAnnotatedSeq(aseq, true, true);
  }





  public void addXorTier(TierGlyph tierA, TierGlyph tierB) {
    int index = handler.getTierIndex(tierB);
    SimpleSymWithProps wrapperSym = new SimpleSymWithProps();
    wrapperSym.setProperty("method", ("xor: " + tierA.getLabel() + ", " + tierB.getLabel()) );
    //    MutableSeqSymmetry symA = new SimpleMutableSeqSymmetry();
    //    MutableSeqSymmetry symB = new SimpleMutableSeqSymmetry();
    SeqSymmetry tempsym = (SeqSymmetry)tierA.getChild(0).getInfo();
    MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)gmodel.getSelectedSeq();
    java.util.List listA = new ArrayList();
    java.util.List listB = new ArrayList();

    //    BioSeq iseq = tempsym.getSpan(0).getBioSeq();
    for (int i=0; i<tierA.getChildCount(); i++) {
      GlyphI child = tierA.getChild(i);
      SeqSymmetry csym = (SeqSymmetry)child.getInfo();
      if (csym != null) {
	listA.add(csym);
	//	symA.addChild(csym);
	//	SeqUtils.printSymmetry(csym);
      }
    }
    for (int i=0; i<tierB.getChildCount(); i++) {
      GlyphI child = tierB.getChild(i);
      SeqSymmetry csym = (SeqSymmetry)child.getInfo();
      if (csym != null) {
	listB.add(csym);
	//	symB.addChild(csym);
	//	SeqUtils.printSymmetry(csym);
      }
    }
    if (DEBUG) {
    System.out.println("listA children: " + listA.size());
    System.out.println("listB children: " + listB.size());
    }
    //    listA.add(symA);
    //    listB.add(symB);
    SeqSymmetry xor_sym = SeqSymSummarizer.getXor(listA, listB, aseq);
    //    SeqSymmetry xor_sym = SeqUtils.xor(symA, symB, aseq);
    if (DEBUG) {
    System.out.println("xorSym children: " + xor_sym.getChildCount());
    }
    
    makeNonPersistentStyle(wrapperSym);
    wrapperSym.addChild(xor_sym);
    aseq.addAnnotation(wrapperSym);
    gviewer.setAnnotatedSeq(aseq, true, true);
  }


  public void addUnionTier(TierGlyph tierA, TierGlyph tierB) {
    if (DEBUG) {System.out.println("making new union tier");}
    int index = handler.getTierIndex(tierB);
    SimpleSymWithProps wrapperSym = new SimpleSymWithProps();
    wrapperSym.setProperty("method", ("union: " + tierA.getLabel() + ", " + tierB.getLabel()) );

    MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)gmodel.getSelectedSeq();
    java.util.List syms = new ArrayList();
    for (int i=0; i<tierA.getChildCount(); i++) {
      GlyphI child = tierA.getChild(i);
      SeqSymmetry csym = (SeqSymmetry)child.getInfo();
      if (csym != null) { syms.add(csym); }
    }
    for (int i=0; i<tierB.getChildCount(); i++) {
      GlyphI child = tierB.getChild(i);
      SeqSymmetry csym = (SeqSymmetry)child.getInfo();
      if (csym != null) { syms.add(csym); }
    }
    //    SeqSymmetry union_sym = SeqUtils.union(symA, symB, aseq);
    SeqSymmetry union_sym = SeqSymSummarizer.getUnion(syms, aseq);

    makeNonPersistentStyle(wrapperSym);
    wrapperSym.addChild(union_sym);
    aseq.addAnnotation(wrapperSym);
    gviewer.setAnnotatedSeq(aseq, true, true);
  }



  /**
   *  Warning: Currently very dangerous.
   *  may muck with parent child relationships in SeqSymmetries, and not restore them!!
   */
  public void addIntersectTier(TierGlyph tierA, TierGlyph tierB) {
    int index = handler.getTierIndex(tierB);
    SimpleSymWithProps wrapperSym = new SimpleSymWithProps();
    wrapperSym.setProperty("method", ("intersect: " + tierA.getLabel() + ", " + tierB.getLabel()) );

    // this is how it should work, but right now tier info isn't set correctly
    //    SeqSymmetry symA = (SeqSymmetry)tierA.getInfo();
    //    SeqSymmetry symB = (SeqSymmetry)tierB.getInfo();

    //    MutableSeqSymmetry symA = new SimpleMutableSeqSymmetry();
    //    MutableSeqSymmetry symB = new SimpleMutableSeqSymmetry();
    SeqSymmetry tempsym = (SeqSymmetry)tierA.getChild(0).getInfo();
    MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)gmodel.getSelectedSeq();
    //    BioSeq iseq = tempsym.getSpan(0).getBioSeq();
    java.util.List listA = new ArrayList();
    java.util.List listB = new ArrayList();
    for (int i=0; i<tierA.getChildCount(); i++) {
      GlyphI child = tierA.getChild(i);
      SeqSymmetry csym = (SeqSymmetry)child.getInfo();
      if (csym != null) {
	listA.add(csym);
	//	symA.addChild(csym);
	//	SeqUtils.printSymmetry(csym);
      }
    }
    for (int i=0; i<tierB.getChildCount(); i++) {
      GlyphI child = tierB.getChild(i);
      SeqSymmetry csym = (SeqSymmetry)child.getInfo();
      if (csym != null) {
	listB.add(csym);
	//	symB.addChild(csym);
	//	SeqUtils.printSymmetry(csym);
      }
    }
    if (DEBUG) {
    System.out.println("listA children: " + listA.size());
    System.out.println("listB children: " + listB.size());
    }
    //    SeqSymmetry intersect_sym = SeqUtils.intersection(symA, symB, aseq);
    //    SeqUtils.printSymmetry(intersect_sym);
    SeqSymmetry intersect_sym = SeqSymSummarizer.getIntersection(listA, listB, aseq);

    makeNonPersistentStyle(wrapperSym);
    wrapperSym.addChild(intersect_sym);
    aseq.addAnnotation(wrapperSym);
    gviewer.setAnnotatedSeq(aseq, true, true);

    // place right below tierB

  }

  public void popupNotify(javax.swing.JPopupMenu popup, TierLabelManager handler) {
    if (handler != this.handler) {
      throw new RuntimeException("");
    }
    int num_selected = handler.getSelectedTierLabels().size();
    
    intersectMI.setEnabled(num_selected==2);
    unionMI.setEnabled(num_selected==2);
    a_not_b_MI.setEnabled(num_selected==2);
    b_not_a_MI.setEnabled(num_selected==2);
    notMI.setEnabled(num_selected==1);
    xorMI.setEnabled(num_selected==2);
    combineMenu.setEnabled(num_selected==1 || num_selected==2);
    
    popup.add(combineMenu);
  }  
  
  void makeNonPersistentStyle(SymWithProps sym) {
    String method = (String) com.affymetrix.igb.view.SeqMapView.determineMethod(sym);    
    AnnotStyle style = AnnotStyle.getInstance(method, false);
    style.setGlyphDepth(1);
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
