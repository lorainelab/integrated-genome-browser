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
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.tiers.*;
import com.affymetrix.igb.menuitem.MenuUtil;
import com.affymetrix.igb.util.ErrorHandler;


/**
 *  Curation Control is temporarily disabled and temporarily deprecated.
 *
 *  This needs to be re-written to work with the TierLabelManager.PopupListener,
 *  and with the AnnotStyle system.  Among other things, it needs to get the
 *  tiers from the SeqMapView.getTiers() method rather than the AffyTieredMap.getTier()
 *  method, and there have to be AnnotStyle objects for each curation type.
 *  (Remember that users can create new annotation types.)
 *  There are also bugs that pre-date the AnnotStyle changes.
 *
 *  @deprecated
 */
public class CurationControl implements ActionListener, ContextualPopupListener  {
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

  static boolean KEEP_PREVIOUS_CURATION = false;

  SeqMapView gviewer;
  CurationSym prev_curation;
  String current_type = "Curation";

  // A fake menu item, prevents null pointer exceptions in actionPerformed()
  // for menu items whose real definitions are commented-out in the code
  private static final JMenuItem empty_menu_item = new JMenuItem("");
  JMenuItem copyAsCurationMI = empty_menu_item;
  JMenuItem addToCurationMI = empty_menu_item;
  JMenuItem deleteFromCurationMI = empty_menu_item;
  JMenuItem flipCurationMI = empty_menu_item;
  JMenuItem undoCurationMI = empty_menu_item;
  JMenuItem redoCurationMI = empty_menu_item;
  JMenuItem newCurTypeMI = empty_menu_item;
  JMenuItem selectCurationMI = empty_menu_item;
  //  JMenuItem splitCurationMI = empty_menu_item;

  TierLabelManager tier_manager;
  JPopupMenu annot_popup;
  JMenu curationM;

  public CurationControl(SeqMapView smv) {
    gviewer = smv;
    tier_manager = gviewer.getTierManager();
    annot_popup = gviewer.getSelectionPopup();

    curationM = new JMenu("Curation");
    copyAsCurationMI = setUpMenuItem(curationM, "Make new curation");
    selectCurationMI = setUpMenuItem(curationM, "Set as current curation");
    newCurTypeMI = setUpMenuItem(curationM, "Make new curation type");
    addToCurationMI = setUpMenuItem(curationM, "Add to current curation");
    deleteFromCurationMI = setUpMenuItem(curationM, "Delete from current curation");
    flipCurationMI = setUpMenuItem(curationM, "Flip current curation");
    undoCurationMI = setUpMenuItem(curationM, "Undo previous edit");
    redoCurationMI = setUpMenuItem(curationM, "Redo previous edit");
    //splitCurationMI = setUpMenuItem(curationM, "Split Curation");

    annot_popup.add(curationM);
    gviewer.addPopupListener(this);
  }

  public void actionPerformed(ActionEvent evt) {
    //Object src = evt.getSource();
    String com = evt.getActionCommand();
    //    System.out.println("Event: " + evt);
    if (copyAsCurationMI.getText().equals(com)) { makeNewCuration(); }
    else if (addToCurationMI.getText().equals(com)) { addToCuration(); }
    else if (deleteFromCurationMI.getText().equals(com)) { deleteFromCuration(); }
    else if (flipCurationMI.getText().equals(com)) { flipCuration(); }
    else if (undoCurationMI.getText().equals(com)) { undoCuration(); }
    else if (redoCurationMI.getText().equals(com)) { redoCuration(); }
    else if (newCurTypeMI.getText().equals(com)) { makeNewCurationType(); }
    else if (selectCurationMI.getText().equals(com)) { setCurrentCuration(); }
    //    else if (splitCurationMI.getText().equals(com)) { splitCuration(); }
  }

  int new_type_count = 0;

  /**
   *  Both make a new curation and create a new annotation tier to support
   *  new type of curation.
   */
  public void makeNewCurationType() {
    String new_type = JOptionPane.showInputDialog("Please enter new type");
    if (new_type != null) {
      current_type = new_type;
      new_type_count++;
      makeNewCuration();
    }
  }

  public void setCurrentCuration() {
    SeqSymmetry selected_sym = gviewer.getSelectedSymmetry();
    if (selected_sym instanceof CurationSym) {
      prev_curation = (CurationSym)selected_sym;
      current_type = (String)prev_curation.getProperty("method");
    }
    else {
      ErrorHandler.errorPanel("Must select curation first");
      return;
    }
  }

  public void flipCuration() {
    SeqSymmetry selected_sym = gviewer.getSelectedSymmetry();
    if (selected_sym instanceof CurationSym) {
      System.out.println("trying to flip curation");
      prev_curation = (CurationSym)selected_sym;
      current_type = (String)prev_curation.getProperty("method");
    }
    else {
      ErrorHandler.errorPanel("Must select curation first");
      return;
    }
  }

  // make two new curations based on splitting the current curation at the selected region...
  /** NOT YET IMPLEMENTED */
  //  public void splitCuration() {
  //  }

  public void makeNewCuration() {
    System.out.println("making new curation, type = " + current_type);
    MutableAnnotatedBioSeq aseq =
      (MutableAnnotatedBioSeq) gmodel.getSelectedSeq();
    if (aseq == null) {
      ErrorHandler.errorPanel("Nothing selected");
      return;
    }
    SeqSymmetry annot_sym = gviewer.getSelectedSymmetry();
    SeqSymmetry region_sym = gviewer.getSelectedRegion();
    if (annot_sym == null && region_sym == null) {
      ErrorHandler.errorPanel("No selected symmetry in map view, so nothing to add to curation");
      return;
    }
    else {
      if (annot_sym == null) { annot_sym = region_sym; }
      CurationSym curation_sym = new CurationSym();
      curation_sym.setProperty("method", current_type);
      SeqUtils.copyToMutable(annot_sym, curation_sym);
      aseq.addAnnotation(curation_sym);

      // place annotation tiers next to axis...
      gviewer.addAnnotationGlyphs(curation_sym);
      AffyTieredMap tmap = gviewer.getSeqMap();
      TierGlyph[] tiers = gviewer.getTiers(current_type, true, AnnotStyle.getInstance(current_type));
      TierGlyph tgl = (curation_sym.getSpan(aseq).isForward() ? tiers[0] : tiers[1]);
      tgl.pack(tmap.getView());
      tmap.stretchToFit(false, false);
      //      tmap.packTiers(false, true, false);  // already called in tmap.stretchToFit()
      tmap.updateWidget();
      prev_curation = curation_sym;
    }
  }

  public void addToCuration() {
    MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)gmodel.getSelectedSeq();
    SeqSymmetry annot_sym = gviewer.getSelectedSymmetry();
    SeqSymmetry region_sym = gviewer.getSelectedRegion();
    if (annot_sym == null && region_sym == null) {
      ErrorHandler.errorPanel("No selected symmetry in map view, so nothing to add to curation");
      return;
    }
    else if (prev_curation == null) {
      ErrorHandler.errorPanel("No previous curation chosen, so can't add to it");
      return;
    }
    else {
      System.out.println("adding to curation");
      if (annot_sym == null) { annot_sym = region_sym; }
      CurationSym curation_sym = new CurationSym();
      curation_sym.setPredecessor(prev_curation);
      prev_curation.setSuccessor(curation_sym);
      curation_sym.setProperty("method", current_type);
      SeqUtils.union(prev_curation, annot_sym, curation_sym, aseq);
      aseq.addAnnotation(curation_sym);
      gviewer.addAnnotationGlyphs(curation_sym);

      AffyTieredMap tmap = gviewer.getSeqMap();
      TierGlyph[] tiers = gviewer.getTiers(current_type, true, AnnotStyle.getInstance(current_type));
      TierGlyph tgl = (curation_sym.getSpan(aseq).isForward() ? tiers[0] : tiers[1]);

      if (! KEEP_PREVIOUS_CURATION) {
	aseq.removeAnnotation(prev_curation);
	GlyphI prevgl = tmap.getItem(prev_curation);
	if (prevgl != null) { tmap.removeItem(prevgl); }
      }
      tgl.pack(tmap.getView());
      tmap.stretchToFit(false, false);
      //      tmap.packTiers(false, true, false);  // already called in tmap.stretchToFit()
      tmap.updateWidget();
      prev_curation = curation_sym;
    }
  }

  public void deleteFromCuration() {
    MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)gmodel.getSelectedSeq();
    SeqSymmetry annot_sym = gviewer.getSelectedSymmetry();
    SeqSymmetry region_sym = gviewer.getSelectedRegion();
    if (annot_sym == null && region_sym == null) {
      ErrorHandler.errorPanel("No selected symmetry in map view, so nothing to delete from curation");
      return;
    }
    else if (prev_curation == null) {
      ErrorHandler.errorPanel("No previous curation chosen, so can't delete from it");
      return;
    }
    else {
      System.out.println("deleting from curation");
      if (annot_sym == null) { annot_sym = region_sym; }
      CurationSym curation_sym = new CurationSym();
      TierGlyph[] tiers = gviewer.getTiers(current_type, true, AnnotStyle.getInstance(current_type));
      TierGlyph tgl = (prev_curation.getSpan(aseq).isForward() ? tiers[0] : tiers[1]);

      curation_sym.setPredecessor(prev_curation);
      prev_curation.setSuccessor(curation_sym);
      curation_sym.setProperty("method", current_type);
      SeqSymmetry inverted_annot = SeqUtils.inverse(annot_sym, aseq, true);
      SeqUtils.intersection(prev_curation, inverted_annot, curation_sym, aseq);
      symCleanupHack(curation_sym, aseq);
      //      SeqUtils.printSymmetry(curation_sym);
      // if entire prev_curation is deleted, then don't want to add new curation
      //     if prev_curation is deleted, curation_sym will be empty, so check based on presence of aseq span in curation_sym
      if (curation_sym.getSpan(aseq) != null)  {
        aseq.addAnnotation(curation_sym);
        gviewer.addAnnotationGlyphs(curation_sym);
      }

      AffyTieredMap tmap = gviewer.getSeqMap();

      if (! KEEP_PREVIOUS_CURATION) {
	aseq.removeAnnotation(prev_curation);
	GlyphI prevgl = tmap.getItem(prev_curation);
	if (prevgl != null) { tmap.removeItem(prevgl); }
      }
      tgl.pack(tmap.getView());
      tmap.stretchToFit(false, false);
      //      tmap.packTiers(false, true, false);  // already called in tmap.stretchToFit()
      tmap.updateWidget();
      if (curation_sym.getSpan(aseq) == null)  {  // no curation sym added, so null out prev_curation
        prev_curation = null;
      }
      else  { prev_curation = curation_sym; }
    }
  }

  protected void symCleanupHack(CurationSym curation_sym, AnnotatedBioSeq aseq) {
      // little hack to clean up, because intersection() ends up creating 0-length
      //   symmetries if two compared spans abut --
      //   (which is why genometry implementation needs to be fixed so that
      //    abutment isn't considered intersection)
      ArrayList clist = new ArrayList();
      for (int i=0; i<curation_sym.getChildCount(); i++) {
	clist.add(curation_sym.getChild(i));
      }
      boolean fix_parent = false;
      for (int i=0; i<clist.size(); i++) {
	SeqSymmetry csym = (SeqSymmetry)clist.get(i);
	SeqSpan cspan = csym.getSpan(aseq);
	if (cspan.getLength() == 0) {
	  curation_sym.removeChild(csym);
	  fix_parent = true;
	}
      }
      if (fix_parent) {
	curation_sym.removeSpan(curation_sym.getSpan(aseq));
	SeqSpan new_pspan = SeqUtils.getChildBounds(curation_sym, aseq);
	curation_sym.addSpan(new_pspan);
      }
      // end zero-length workaround hack
  }

  public void undoCuration() {
    if (prev_curation == null || (prev_curation.getPredecessor() == null)) {
      System.out.println("No predecessor curation");
      return;
    }
    else {
      CurationSym current_curation = prev_curation;
      current_type = (String)current_curation.getProperty("method");
      prev_curation = current_curation.getPredecessor();
      MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)gmodel.getSelectedSeq();
      aseq.removeAnnotation(current_curation);
      aseq.addAnnotation(prev_curation);
      AffyTieredMap tmap = gviewer.getSeqMap();
      TierGlyph[] tiers = gviewer.getTiers(current_type, true, AnnotStyle.getInstance(current_type));
      TierGlyph tgl = (current_curation.getSpan(aseq).isForward() ? tiers[0] : tiers[1]);
      GlyphI curgl = tmap.getItem(current_curation);
      if (curgl != null) { tmap.removeItem(curgl); }
      gviewer.addAnnotationGlyphs(prev_curation);

      tgl.pack(tmap.getView());
      tmap.stretchToFit(false, false);
      //      tmap.packTiers(false, true, false);  // already called in tmap.stretchToFit()
      tmap.updateWidget();
    }
  }

  public void redoCuration() {
    if (prev_curation == null || (prev_curation.getSuccessor() == null)) {
      System.out.println("No successor curation");
      return;
    }
    else {
      CurationSym current_curation = prev_curation;
      current_type = (String)current_curation.getProperty("method");
      CurationSym next_curation = current_curation.getSuccessor();
      MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)gmodel.getSelectedSeq();
      aseq.removeAnnotation(current_curation);
      aseq.addAnnotation(next_curation);
      AffyTieredMap tmap = gviewer.getSeqMap();
      TierGlyph[] tiers = gviewer.getTiers(current_type, true, AnnotStyle.getInstance(current_type));
      TierGlyph tgl = (next_curation.getSpan(aseq).isForward() ? tiers[0] : tiers[1]);
      GlyphI curgl = tmap.getItem(current_curation);
      if (curgl != null) { tmap.removeItem(curgl); }
      gviewer.addAnnotationGlyphs(next_curation);
      prev_curation = next_curation;
      tgl.pack(tmap.getView());
      tmap.stretchToFit(false, false);
      //      tmap.packTiers(false, true, false);  // already called in tmap.stretchToFit()
      tmap.updateWidget();
    }
  }


  private final JMenuItem setUpMenuItem(JMenu menu, String action_command) {
    return gviewer.setUpMenuItem((Container) menu,
      action_command, (ActionListener) this);
  }

  /**
   *  Implementing ContextualPopupListener to dynamicly modify
   *  right-click popup on SeqMapView to add a curation menu.
   */
  public void popupNotify(JPopupMenu popup, java.util.List selected_items, SymWithProps primary_sym) {
    popup.add(curationM);
  }

}

