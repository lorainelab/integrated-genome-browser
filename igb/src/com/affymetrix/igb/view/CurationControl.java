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

import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.SimpleMutableSeqSpan;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.CurationSym;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.igb.parsers.Das2FeatureSaxParser;
import com.affymetrix.igb.tiers.*;
import com.affymetrix.igb.util.ErrorHandler;
import org.xml.sax.InputSource;


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
 */
public class CurationControl implements ActionListener, ContextualPopupListener  {
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();

  static boolean KEEP_PREVIOUS_CURATION = false;

  /**
  *   Used to assign curation syms an id unique to this session
  *   (If curation is persisted, may need to change this id later based
  *     on id assigment from persistent store)
  */
  static int curation_id_count = 0;

  /**
   *  The prefix to use for all curation ids,
   *   (If curation is persisted, may need to change this id later based
   *     on id assigment from persistent store)
   */
  static String curation_id_root = "das-private:";

  public static final String PREF_ENABLE_CURATIONS = "Enable Curations";
  public static final boolean default_enable_curations = false;

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
      CurationSym curation_sym = copyToCuration(annot_sym);
      curation_sym.setProperty("method", current_type);
      aseq.addAnnotation(curation_sym);

      // place annotation tiers next to axis...
      gviewer.addAnnotationGlyphs(curation_sym);
      AffyTieredMap tmap = gviewer.getSeqMap();
      TierGlyph[] tiers = gviewer.getTiers(current_type, true, DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(current_type));
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
      //      CurationSym curation_sym = new CurationSym();
      SeqSymmetry union_sym = SeqUtils.union(prev_curation, annot_sym, aseq);
      CurationSym curation_sym = copyToCuration(union_sym);
      curation_sym.setPredecessor(prev_curation);
      prev_curation.setSuccessor(curation_sym);
      curation_sym.setProperty("method", current_type);
      //      SeqUtils.union(prev_curation, annot_sym, curation_sym, aseq);
      aseq.addAnnotation(curation_sym);
      gviewer.addAnnotationGlyphs(curation_sym);

      AffyTieredMap tmap = gviewer.getSeqMap();
      TierGlyph[] tiers = gviewer.getTiers(current_type, true, DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(current_type));
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
      //      CurationSym curation_sym = new CurationSym();
      SeqSymmetry inverted_annot = SeqUtils.inverse(annot_sym, aseq, true);
      MutableSeqSymmetry intersect_sym = SeqUtils.intersection(prev_curation, inverted_annot, aseq);
      symCleanupHack(intersect_sym, aseq);
      CurationSym curation_sym = copyToCuration(intersect_sym);
      curation_sym.setPredecessor(prev_curation);
      prev_curation.setSuccessor(curation_sym);
      curation_sym.setProperty("method", current_type);

      TierGlyph[] tiers = gviewer.getTiers(current_type, true, DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(current_type));
      TierGlyph tgl = (prev_curation.getSpan(aseq).isForward() ? tiers[0] : tiers[1]);

      //      SeqUtils.intersection(prev_curation, inverted_annot, curation_sym, aseq);
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

  protected CurationSym copyToCuration(SeqSymmetry sym) {
    CurationSym cur = new CurationSym();
    String curation_id = (curation_id_root + current_type + "." + curation_id_count);
    curation_id_count++;
    boolean success = copyToCuration(sym, cur, curation_id);
    if (success) { return cur; }
    else { return null; }
  }

  protected boolean copyToCuration(SeqSymmetry sym, CurationSym cur, String curation_id) {
    cur.clear();
    cur.setID(curation_id);
    int spanCount = sym.getSpanCount();
    for (int i=0; i<spanCount; i++) {
      SeqSpan span = sym.getSpan(i);
      SeqSpan newspan = new SimpleMutableSeqSpan(span);
      cur.addSpan(newspan);
    }
    int childCount = sym.getChildCount();
    for (int i=0; i<childCount; i++) {
      SeqSymmetry child = sym.getChild(i);
      CurationSym newchild = new CurationSym();
      String child_id = curation_id + "." + i;
      copyToCuration(child, newchild, child_id);
      cur.addChild(newchild);
    }
    return true;
  }


  //  protected void symCleanupHack(CurationSym curation_sym, AnnotatedBioSeq aseq) {
  protected void symCleanupHack(MutableSeqSymmetry psym, AnnotatedBioSeq aseq) {
      // little hack to clean up, because intersection() ends up creating 0-length
      //   symmetries if two compared spans abut --
      //   (which is why genometry implementation needs to be fixed so that
      //    abutment isn't considered intersection)
      ArrayList clist = new ArrayList();
      for (int i=0; i<psym.getChildCount(); i++) {
	clist.add(psym.getChild(i));
      }
      boolean fix_parent = false;
      for (int i=0; i<clist.size(); i++) {
	SeqSymmetry csym = (SeqSymmetry)clist.get(i);
	SeqSpan cspan = csym.getSpan(aseq);
	if (cspan.getLength() == 0) {
	  psym.removeChild(csym);
	  fix_parent = true;
	}
      }
      if (fix_parent) {
	psym.removeSpan(psym.getSpan(aseq));
	SeqSpan new_pspan = SeqUtils.getChildBounds(psym, aseq);
	psym.addSpan(new_pspan);
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
      TierGlyph[] tiers = gviewer.getTiers(current_type, true, DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(current_type));
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
      TierGlyph[] tiers = gviewer.getTiers(current_type, true, DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(current_type));
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


  long prev_writeback_time = System.currentTimeMillis();
  /**
   *  commitCurations() posts curation changes to DAS/2 writeback server
   *  only sends server data for curations that have changed since last commit
   *  For now assuming all commits are on same genome (but can be on different chromosomes)
   *  For now, assuming all curation commits go to UCLA DAS/2 writeback server
   */
  public boolean commitCurations(TierGlyph atier) {
    boolean success = false;
    // probably want to save all changes across all chromosomes and all curation types????
    MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)gmodel.getSelectedSeq();
    AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
    String annot_type = atier.getLabel();
    // should probably do this via aseq.getAnnotation(annot_type);
    int childcount= atier.getChildCount();
    java.util.List syms = new ArrayList(childcount);
    for (int i=0; i<childcount; i++) {
      GlyphI child = atier.getChild(i);
      if (child.getInfo() instanceof SeqSymmetry) {
	//      if (child.getInfo() instanceof CurationSym) {
	syms.add(child.getInfo());
      }
    }
    Das2FeatureSaxParser das_parser = new Das2FeatureSaxParser();
    System.out.println("writeback doc:");
    das_parser.writeBackAnnotations(syms, aseq, "type/SO:region", System.out); // diagnostic
    prev_writeback_time = System.currentTimeMillis();
    try {
      System.out.println("Testing DAS/2 writeback: "+ syms.size());
      String writeback_loc = "http://genomics.ctrl.ucla.edu/~allenday/cgi-bin/das2xml-parser/stable4.pl";
      URL writeback_url = new URL(writeback_loc);
      URLConnection con = writeback_url.openConnection();
      con.setDoInput(true);
      con.setDoOutput(true);

      OutputStream conos = con.getOutputStream();
      BufferedOutputStream bos = new BufferedOutputStream(conos);
      //      das_parser.writeBackAnnotations(syms, aseq, annot_type, bos);
      das_parser.writeBackAnnotations(syms, aseq, "type/SO:region", bos);
      bos.flush();
      bos.close();

      InputStream istr = con.getInputStream();
      //  for now just need to change ids to match ids from writeback server
      //  eventually want to completely replace syms with given ids with those from server (if they differ...)
      StringBuffer sbuf = new StringBuffer();
      BufferedReader reader = new BufferedReader(new InputStreamReader(istr));
      System.out.println("****** Response from writeback server: ");
      String line;
      while ((line = reader.readLine()) != null) {
	System.out.println(line);
	sbuf.append(line);
	sbuf.append("\n");
      }
      istr.close();
      String return_contents = sbuf.toString();
      InputSource isrc = new InputSource(new StringReader(return_contents));
      Das2FeatureSaxParser return_parser = new Das2FeatureSaxParser();
      // read return document as new annotations for now...
      java.util.List results = return_parser.parse(isrc, writeback_loc, group, true);
      System.out.println("results returned: " + results.size());
      success = true;
    }
    catch (Exception ex) {
      ex.printStackTrace();
      success = false;
    }
    gviewer.setAnnotatedSeq(aseq, true, true);
    System.out.println("finished test writeback");
    return success;
  }


  private final JMenuItem setUpMenuItem(JMenu menu, String action_command) {
    return gviewer.setUpMenuItem((Container) menu,
      action_command, (ActionListener) this);
  }

  /**
   *  Implementing ContextualPopupListener to dynamicly modify
   *  right-click popup on SeqMapView to add a curation menu.
   */
  public void popupNotify(JPopupMenu popup, java.util.List selected_items, SeqSymmetry primary_sym) {
    popup.add(curationM);
  }

}

