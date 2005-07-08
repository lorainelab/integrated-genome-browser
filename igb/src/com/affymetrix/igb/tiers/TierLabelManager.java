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

package com.affymetrix.igb.tiers;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.widget.*;
import com.affymetrix.genoviz.util.Timer;

// really need to remove genometry stuff from this...
import com.affymetrix.genometry.*;
import com.affymetrix.genometry.symmetry.*;
import com.affymetrix.genometry.util.SeqUtils;

import com.affymetrix.igb.IGB;
import com.affymetrix.igb.glyph.GraphGlyph;
import com.affymetrix.igb.glyph.GlyphSummarizer;
import com.affymetrix.igb.glyph.CoverageSummarizerGlyph;
import com.affymetrix.igb.menuitem.FileTracker;
import com.affymetrix.igb.genometry.GraphSym;
import com.affymetrix.igb.genometry.SeqSymSummarizer;
import com.affymetrix.igb.genometry.SymWithProps;
import com.affymetrix.igb.genometry.SimpleSymWithProps;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.AnnotatedSeqViewer;
import com.affymetrix.igb.util.UniFileChooser;
import com.affymetrix.igb.parsers.BedParser;
import com.affymetrix.igb.genometry.SingletonGenometryModel;

public class TierLabelManager
  //  implements MouseListener, MouseMotionListener, ActionListener  {
  implements MouseListener, ActionListener  {
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  final boolean DEBUG = false;
  boolean INCLUDE_COVERAGE_ITEM = true;

  AffyLabelledTierMap tiermap;
  NeoMap labelmap;
  JPopupMenu popup;
  JMenuItem select_allMI;
  JMenuItem expandMI;
  JMenuItem collapseMI;

  JMenuItem glyph_summarizeMI;
  JMenuItem sym_summarizeMI;
  JMenuItem sym_coverageMI;
  JMenuItem hideMI;
  JMenuItem collapseAllMI;
  JMenuItem expandAllMI;
  JMenuItem saveBedMI;
  JMenuItem intersectMI;
  JMenuItem unionMI;
  JMenuItem a_not_b_MI;
  JMenuItem b_not_a_MI;
  JMenuItem xorMI;
  JMenuItem notMI;
  JMenuItem changeExpandMaxMI;
  JMenuItem changeExpandMaxAllMI;
  JMenuItem color_childrenMI;
  JMenuItem showAllMI;

  JMenu showMenu;
  JMenu combineMenu;
  //  JMenuItem exportMI;

  TierGlyph current_tier;
  GlyphI current_label;
  boolean dragging_label = false;
  Hashtable tier2showitem =  new Hashtable();
  Hashtable menuitem_hash = new Hashtable();
  int xoffset_pop = 10;
  int yoffset_pop = 0;
  java.util.List default_menu_list = new ArrayList();

  AnnotatedSeqViewer gviewer;
  /**
   *  BLECH -- should move this out and generalize so other classes can add popup menu choices...
   */
  public void setViewer(AnnotatedSeqViewer view) {
    //  public void setViewer(SeqMapView view) {
    gviewer = view;
  }

  public TierLabelManager(AffyLabelledTierMap map) {
    tiermap = map;
    popup = new JPopupMenu();
    select_allMI = new JMenuItem("Select All Tiers");
    color_childrenMI = new JMenuItem("Change Annotation Color");
    expandMI = new JMenuItem("Expand");
    changeExpandMaxMI = new JMenuItem("Adjust Max Expand");
    changeExpandMaxAllMI = new JMenuItem("Adjust Max Expand All");
    collapseMI = new JMenuItem("Collapse");
    hideMI = new JMenuItem("Hide");
    sym_summarizeMI = new JMenuItem("Make Annotation Depth Track");
    sym_coverageMI = new JMenuItem("Make Annotation Coverage Track");
    glyph_summarizeMI = new JMenuItem("Glyph Summarize");
    saveBedMI = new JMenuItem("Save tier as BED file");
    intersectMI = new JMenuItem("Intersect Selected");
    unionMI = new JMenuItem("Union Selected");
    a_not_b_MI = new JMenuItem("A not B Selected");
    b_not_a_MI = new JMenuItem("B not A Selected");
    xorMI = new JMenuItem("Xor Selected");
    notMI = new JMenuItem("Not Selected");

    collapseAllMI = new JMenuItem("Collapse All");
    expandAllMI = new JMenuItem("Expand All");
    showAllMI = new JMenuItem("Show All");
    //    exportMI = new JMenuItem("Export...");

    select_allMI.addActionListener(this);
    color_childrenMI.addActionListener(this);
    expandMI.addActionListener(this);
    collapseMI.addActionListener(this);
    hideMI.addActionListener(this);
    expandAllMI.addActionListener(this);
    changeExpandMaxMI.addActionListener(this);
    changeExpandMaxAllMI.addActionListener(this);
    collapseAllMI.addActionListener(this);
    glyph_summarizeMI.addActionListener(this);
    sym_summarizeMI.addActionListener(this);
    sym_coverageMI.addActionListener(this);
    saveBedMI.addActionListener(this);
    intersectMI.addActionListener(this);
    unionMI.addActionListener(this);
    a_not_b_MI.addActionListener(this);
    b_not_a_MI.addActionListener(this);
    xorMI.addActionListener(this);
    notMI.addActionListener(this);
    //    exportMI.addActionListener(this);
    showAllMI.addActionListener(this);

    showMenu = new JMenu("Show");
    combineMenu = new JMenu("Combine Tiers");

    labelmap = tiermap.getLabelMap();
    labelmap.addMouseListener(this);

    default_menu_list.add(hideMI);
    default_menu_list.add(showMenu);
    default_menu_list.add(color_childrenMI);
    default_menu_list.add(new JSeparator());
    default_menu_list.add(collapseMI);
    default_menu_list.add(expandMI);
    default_menu_list.add(changeExpandMaxMI);
    default_menu_list.add(collapseAllMI);
    default_menu_list.add(expandAllMI);
    default_menu_list.add(changeExpandMaxAllMI);
    default_menu_list.add(new JSeparator());
    default_menu_list.add(saveBedMI);
    default_menu_list.add(new JSeparator());
    // default_menu_list.add(glyph_summarizeMI); }  // deprecated
    default_menu_list.add(sym_summarizeMI);
    if (INCLUDE_COVERAGE_ITEM)  { default_menu_list.add(sym_coverageMI); }


    default_menu_list.add(combineMenu);
    combineMenu.add(unionMI);
    combineMenu.add(intersectMI);
    combineMenu.add(a_not_b_MI);
    combineMenu.add(b_not_a_MI);
    combineMenu.add(xorMI);
    combineMenu.add(notMI);

    showMenu.add(showAllMI);
    showMenu.add(new JSeparator());

    default_menu_list = Collections.unmodifiableList(default_menu_list);

    tiermap.getLabelMap().getScene().setSelectionAppearance(SceneI.SELECT_OUTLINE);
  }

  public void setCurrentTier(TierGlyph tglyph) {
    current_tier = tglyph;
  }

  public TierGlyph getCurrentTier() {
    return current_tier;
  }

  /**
   *  Returns the List of menu items that populate the popup menu when
   *  the tier is right-clicked.  Internally, TierLabelManager uses this
   *  List to build the popup on-the-fly based on the tier that is clicked
   *
   *  This can be used by other code to modify a specific tier's popup
   *    (for example change the "axis coordinate" tier's popup by removing
   *    the  collapse and expand menu items)
   *
   *  In general, the list returned will be modifiable.  But, the one returned
   *  by getMenuList(null) will be a default list which is not modifiable.
   */
  public java.util.List getMenuList(TierGlyph tgl) {
    java.util.List mlist = default_menu_list;
    if (tgl != null) {
      mlist = (java.util.List) menuitem_hash.get(tgl);
      if (mlist == null) {
        mlist = new ArrayList(default_menu_list);
        menuitem_hash.put(tgl, mlist);
      }
    }
    return mlist;
  }

  private static final String SELECT_SINGLE_TIER = "Must select a single tier";

  public java.util.List getSelectedTiers() {
    java.util.List selected_labels = labelmap.getSelected();
    int sel_count = selected_labels.size();
    java.util.List selected_tiers = new ArrayList();

    for (int i=0; i<sel_count; i++) {
      // TierGlyph should be data model for tier label, access via lable.getInfo()
      //      if (selected.get(i) instanceof TierLabelGlyph) {
      // currently assuming all selected glyphs in label map are TierLabelGlyphs
      TierLabelGlyph tlg = (TierLabelGlyph)selected_labels.get(i);
      TierGlyph tier = (TierGlyph)tlg.getInfo();
      selected_tiers.add(tier);
      //      }
    }
    return selected_tiers;
  }

  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();

    Vector selected = labelmap.getSelected();
    int num_selected = selected.size();

    if (src == expandMI) {
      expandTiers(selected);
    }
    else if (src == changeExpandMaxMI) {
      if (num_selected != 1) {IGB.errorPanel(SELECT_SINGLE_TIER);}
      else {changeExpandMax(current_tier);}
    }
    else if (src == changeExpandMaxAllMI) {
      changeExpandMaxAll();
    }
    else if (src == collapseMI) {
      collapseTiers(selected);
    }
    else if (src == hideMI) {
      hideTiers(selected);
    }
    else if (src == showAllMI) {
      showAllTiers();
    }
    else if (src == sym_summarizeMI) {
      if (num_selected != 1) {IGB.errorPanel(SELECT_SINGLE_TIER);}
      else { addSymSummaryTier(current_tier); }
    }
    else if (src == sym_coverageMI) {
      if (num_selected != 1) {IGB.errorPanel(SELECT_SINGLE_TIER);}
      else { addSymCoverageTier(current_tier); }
    }
    else if (src == glyph_summarizeMI) {
      if (num_selected != 1) {IGB.errorPanel(SELECT_SINGLE_TIER);}
      else { addGlyphSummaryTier(current_tier); }
    }
    else if (src == saveBedMI) {
      if (num_selected != 1) {IGB.errorPanel(SELECT_SINGLE_TIER);}
      else {saveAsBedFile(current_tier);}
    }
    else if (src == color_childrenMI) {
      colorTierChildren();
    }
    else if (src == select_allMI) {
      selectAllTiers();
    }
    else if (src == intersectMI) {
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
    else if (src == collapseAllMI) {
      collapseAllTiers();
    }
    else if (src == expandAllMI) {
      expandAllTiers();
    }
    //    else if (src == exportMI) {
    //      exportTier(current_tier);
    //    }
    //else {
    //  System.out.println("action Performed: " + evt);
    //}
  }

  public void colorTierChildren() {
    java.util.List selected = labelmap.getSelected();
    int selcount = selected.size();
    if (selcount > 0) {
      Color col = JColorChooser.showDialog(((SeqMapView)gviewer).getFrame(),
					   "Grid Color Chooser", null);
      if (col != null) {
	for (int i=0; i<selcount; i++) {
	  TierLabelGlyph tlg = (TierLabelGlyph)selected.get(i);
	  TierGlyph tier = (TierGlyph)tlg.getInfo();
	  colorChildren(tier, col);
	}
	tiermap.updateWidget();
      }
    }
  }

  // recursively set color of all children of a parent glyph
  public void colorChildren(GlyphI parent, Color col) {
    int child_count = parent.getChildCount();
    for (int i=0; i<child_count; i++) {
      GlyphI child = parent.getChild(i);
      child.setColor(col);
      if (child.getChildCount() > 0) {
	colorChildren(child, col);
      }
    }
  }


  public java.util.List getAllTierLabels() {
    java.util.List labels = new ArrayList();
    java.util.List tiers = tiermap.getAllTiers();
    int tiercount = tiers.size();
    for (int i=0; i<tiercount; i++) {
      TierGlyph tg = (TierGlyph)tiers.get(i);
      GlyphI tierlabel = labelmap.getItem(tg);
      labels.add(tierlabel);
    }
    return labels;
  }

  public void selectAllAnnotationTiers() {

  }

  public void selectAllGraphTiers() {

  }

  public void selectAllTiers()  {
    java.util.List labels = getAllTierLabels();
    int tiercount = labels.size();
    for (int i=0; i<tiercount; i++) {
      GlyphI tierlabel = (GlyphI)labels.get(i);
      labelmap.select(tierlabel);
    }
    labelmap.updateWidget();
  }

  public void expandAllTiers() {
    java.util.List tiervec = tiermap.getTierLabels();
    expandTiers(tiervec);
  }

  /**
   *  Expands all the tiers corresponding to the given list of label tiers.
   *  @param tier_labels  a List of GlyphI objects for each of which getInfo()
   *                      returns a TierGlyph.  This list can be empty, but not null.
   */
  void expandTiers(java.util.List tier_labels) {
    Timer tim = new Timer();
    tim.start();
    int tiercount = tier_labels.size();
    for (int i=0; i<tiercount; i++) {
      GlyphI label = (GlyphI) tier_labels.get(i);
      TierGlyph tier = (TierGlyph) label.getInfo();
      if (tier.getState() != TierGlyph.HIDDEN) {
        // Don't try to expand a hidden tier, because that would make it visible
        // but tier2showitem wouldn't know about the change.
        tier.setState(TierGlyph.EXPANDED);
        tier.pack(tiermap.getView());
      }
    }
    tiermap.packTiers(false, true, false);
    // forcing stretchToFit vertically (looks better than the alternative...
    tiermap.stretchToFit(false, true);
    long packtime = tim.read();
    if (DEBUG) System.out.println("time to pack tiers: " + (packtime/1000f));
    tiermap.updateWidget();
  }

  public void collapseAllTiers() {
    java.util.List tiervec = tiermap.getTierLabels();
    collapseTiers(tiervec);
  }

  /**
   *  Collapses all the tiers corresponding to the given list of label tiers.
   *  @param tier_labels  a List of GlyphI objects for each of which getInfo()
   *                      returns a TierGlyph.  This list can be empty, but not null.
   */
  void collapseTiers(java.util.List tier_labels) {
    Timer tim = new Timer();
    tim.start();
    int tiercount = tier_labels.size();
    for (int i=0; i<tiercount; i++) {
      GlyphI label = (GlyphI) tier_labels.get(i);
      TierGlyph tier = (TierGlyph) label.getInfo();
      if (tier.getState() != TierGlyph.HIDDEN) {
        // Don't try to collapse a hidden tier, because that would make it visible
        // but tier2showitem wouldn't know about the change.
        tier.setState(TierGlyph.COLLAPSED);
        tier.pack(tiermap.getView());
      }
    }
    tiermap.packTiers(false, true, false);
    // although tiermap.packTiers() calls stretchToFit(), STILL need to call it
    //   here (it needs to be called once BEFORE stretchToFit is called -- there's
    //   got to be a cleaner way to deal with this...
    tiermap.stretchToFit(false, false);
    long packtime = tim.read();
    if (DEBUG) System.out.println("time to pack tiers: " + (packtime/1000f));
    verifyZoom();
    tiermap.updateWidget();
  }

  public void changeExpandMaxAll() {
    String input =
      (String)JOptionPane.showInputDialog(null,
					  "Enter new maximum tier height for all tiers",
					  "Change All Tiers Max Height", JOptionPane.PLAIN_MESSAGE,
					  null, null, "");

    if (input == JOptionPane.UNINITIALIZED_VALUE || !(input instanceof String)) {
      return;
    }

    int newmax;
    try {
      newmax = Integer.parseInt(input);
    }
    catch (NumberFormatException ex) {
      IGB.errorPanel("Couldn't parse new tier max '"+input+"'");
      return;
    }
    java.util.List tiervec = tiermap.getAllTiers();
    int tiercount = tiervec.size();
    for (int i=0; i<tiercount; i++) {
      TierGlyph tier = (TierGlyph)tiervec.get(i);
      PackerI packer = tier.getExpandedPacker();
      if (packer instanceof FasterExpandPacker) {
	FasterExpandPacker fpacker = (FasterExpandPacker)packer;
	fpacker.setMaxSlots(newmax);
	tier.pack(tiermap.getView());
      }
    }

    tiermap.packTiers(false, true, false);
    // forcing stretchToFit vertically (looks better than the alternative...
    tiermap.stretchToFit(false, true);
    tiermap.updateWidget();
  }

  public void changeExpandMax(TierGlyph atier) {
    if (DEBUG) {System.out.println("called TierLabelManager.changeExpandMax()");}
    PackerI packer = atier.getExpandedPacker();
    if (packer instanceof FasterExpandPacker) {
      FasterExpandPacker fpacker = (FasterExpandPacker)packer;
      int curmax = fpacker.getMaxSlots();
      if (DEBUG) System.out.println("current max slots: " + curmax);
      int newmax;
      Object input = JOptionPane.showInputDialog(null,
					    "Enter new maximum tier height",
					    "Change Tier Max Height", JOptionPane.PLAIN_MESSAGE,
					    null, null, Integer.toString(curmax));
      if (input == JOptionPane.UNINITIALIZED_VALUE || !(input instanceof String)) {
        return;
      }
      try {
	newmax = Integer.parseInt((String) input);
      }
      catch (NumberFormatException ex) {
	IGB.errorPanel("Problem parsing new tier max '"+input+"'");
	return;
      }
      fpacker.setMaxSlots(newmax);
      atier.pack(tiermap.getView());
      tiermap.packTiers(false, true, false);
      tiermap.stretchToFit(false, true);  // tiermap.packTiers() calls stretchToFit
      tiermap.updateWidget();
    }
  }


  public void addUnionTier() {
    Vector selected = labelmap.getSelected();
    if (selected.size() == 2) {
      // only consider first two selected...
      TierLabelGlyph labelA = (TierLabelGlyph)selected.elementAt(0);
      TierLabelGlyph labelB = (TierLabelGlyph)selected.elementAt(1);
      TierGlyph tierA = (TierGlyph)labelA.getInfo();
      TierGlyph tierB = (TierGlyph)labelB.getInfo();
      if (DEBUG) {
      System.out.println("labels: " + labelA.getString() + ",  " + labelB.getString());
      System.out.println("tiers: " + tierA + ",  " + tierB);
      }
      addUnionTier(tierA, tierB);
    }
    else {
      IGB.errorPanel("Must select two and only two tiers for union");
    }
  }

  public void addExclusiveTier(boolean exclusiveA) {
    Vector selected = labelmap.getSelected();
    if (selected.size() == 2) {
      // only consider first two selected...
      TierLabelGlyph labelA = (TierLabelGlyph)selected.elementAt(0);
      TierLabelGlyph labelB = (TierLabelGlyph)selected.elementAt(1);
      TierGlyph tierA = (TierGlyph)labelA.getInfo();
      TierGlyph tierB = (TierGlyph)labelB.getInfo();
      if (DEBUG) {
      System.out.println("labels: " + labelA.getString() + ",  " + labelB.getString());
      System.out.println("tiers: " + tierA + ",  " + tierB);
      }
      addExclusiveTier(tierA, tierB, exclusiveA);
    }
    else {
      IGB.errorPanel("Must select two and only two tiers for union");
    }
  }


  public void addXorTier() {
    Vector selected = labelmap.getSelected();
    if (selected.size() == 2) {
      // only consider first two selected...
      TierLabelGlyph labelA = (TierLabelGlyph)selected.elementAt(0);
      TierLabelGlyph labelB = (TierLabelGlyph)selected.elementAt(1);
      TierGlyph tierA = (TierGlyph)labelA.getInfo();
      TierGlyph tierB = (TierGlyph)labelB.getInfo();
      if (DEBUG) {System.out.println("tiers: " + tierA + ",  " + tierB);}
      addXorTier(tierA, tierB);
    }
    else {
      IGB.errorPanel("Must select two and only two tiers for XOR(A,B)");
    }
  }

  public void addNotTier() {
    Vector selected = labelmap.getSelected();
    if (selected.size() == 1) {
      // only consider first two selected...
      TierLabelGlyph labelA = (TierLabelGlyph)selected.elementAt(0);
      TierGlyph tierA = (TierGlyph)labelA.getInfo();
      if (DEBUG) {System.out.println("tiers: " + tierA);}
      addNotTier(tierA);
    }
    else {
      IGB.errorPanel("Must select one and only one tier for NOT(A)");
    }
  }


  public void addIntersectTier() {
    Vector selected = labelmap.getSelected();
    if (selected.size() == 2) {
      // only consider first two selected...
      TierLabelGlyph labelA = (TierLabelGlyph)selected.elementAt(0);
      TierLabelGlyph labelB = (TierLabelGlyph)selected.elementAt(1);
      TierGlyph tierA = (TierGlyph)labelA.getInfo();
      TierGlyph tierB = (TierGlyph)labelB.getInfo();
      if (DEBUG)  {
        System.out.println("labels: " + labelA.getString() + ",  " + labelB.getString());
        System.out.println("tiers: " + tierA + ",  " + tierB);
      }
      addIntersectTier(tierA, tierB);
    }
    else {
      //      JOptionPane.showMessageDialog(gviewer.getFrame(),
      IGB.errorPanel("Must select two and only two tiers for intersection");
    }
  }



  public void addNotTier(TierGlyph tierA) {
    int index = getTierIndex(tierA);
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
    wrapperSym.addChild(inverse_sym);
    aseq.addAnnotation(wrapperSym);
    gviewer.setAnnotatedSeq(aseq, true, true);
  }



  public void addExclusiveTier(TierGlyph tierA, TierGlyph tierB, boolean exclusiveA) {
    int index = getTierIndex(tierB);
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

    wrapperSym.addChild(exclusive_sym);
    aseq.addAnnotation(wrapperSym);
    gviewer.setAnnotatedSeq(aseq, true, true);
  }





  public void addXorTier(TierGlyph tierA, TierGlyph tierB) {
    int index = getTierIndex(tierB);
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
    wrapperSym.addChild(xor_sym);
    aseq.addAnnotation(wrapperSym);
    gviewer.setAnnotatedSeq(aseq, true, true);
  }


  public void addUnionTier(TierGlyph tierA, TierGlyph tierB) {
    if (DEBUG) {System.out.println("making new union tier");}
    int index = getTierIndex(tierB);
    SimpleSymWithProps wrapperSym = new SimpleSymWithProps();
    wrapperSym.setProperty("method", ("union: " + tierA.getLabel() + ", " + tierB.getLabel()) );
    //    MutableSeqSymmetry symA = new SimpleMutableSeqSymmetry();
    //    MutableSeqSymmetry symB = new SimpleMutableSeqSymmetry();
    //    SeqSymmetry tempsym = (SeqSymmetry)tierA.getChild(0).getInfo();
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
    wrapperSym.addChild(union_sym);
    aseq.addAnnotation(wrapperSym);
    gviewer.setAnnotatedSeq(aseq, true, true);
  }



  /**
   *  Warning: Currently very dangerous.
   *  may muck with parent child relationships in SeqSymmetries, and not restore them!!
   */
  public void addIntersectTier(TierGlyph tierA, TierGlyph tierB) {
    int index = getTierIndex(tierB);
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

    wrapperSym.addChild(intersect_sym);
    aseq.addAnnotation(wrapperSym);
    gviewer.setAnnotatedSeq(aseq, true, true);

    // place right below tierB

  }



  public int getTierIndex(TierGlyph atier) {
    Vector tiers = tiermap.getAllTiers();
    int index = -1;
    for (int i=0; i<tiers.size(); i++) {
      if (atier == tiers.elementAt(i)) {
	index = i+1;
	break;
      }
    }
    return index;
  }

  public void saveAsBedFile(TierGlyph atier) {
    if (DEBUG) {
    System.out.println("trying to save tier as bed file, tier = " + atier.getLabel());
    }
    int childcount= atier.getChildCount();
    java.util.List syms = new ArrayList(childcount);
    for (int i=0; i<childcount; i++) {
      GlyphI child = atier.getChild(i);
      if (child.getInfo() instanceof SeqSymmetry) {
	syms.add(child.getInfo());
      }
    }
    JFileChooser chooser = UniFileChooser.getFileChooser("Bed file (*.bed)", "bed");
    chooser.setCurrentDirectory(FileTracker.DATA_DIR_TRACKER.getFile());
    int option = chooser.showSaveDialog(tiermap);
    if (option == JFileChooser.APPROVE_OPTION) {
      FileTracker.DATA_DIR_TRACKER.setFile(chooser.getCurrentDirectory());
      MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)gmodel.getSelectedSeq();
      BufferedWriter bw = null;
      try {
	File fil = chooser.getSelectedFile();
	FileWriter fw = new FileWriter(fil);
	bw = new BufferedWriter(fw);
	BedParser.writeBedFormat(bw, syms, aseq);
	bw.close();
      }
      catch (Exception ex) {
	IGB.errorPanel("Problem saving file", ex);
      } finally {
        if (bw != null) try {bw.close();} catch (IOException ioe) {}
      }
    }
  }

  static void collectSyms(GlyphI gl, java.util.List syms) {
    Object info = gl.getInfo();
    if ((info != null)  && (info instanceof SeqSymmetry)) {
      syms.add((SeqSymmetry)info);
    }
    else if (gl.getChildCount() > 0) {
      // if no SeqSymmetry associated with glyph, descend and try children
      int child_count = gl.getChildCount();
      for (int i=0; i<child_count; i++) {
	collectSyms(gl.getChild(i), syms);
      }
    }
  }

  public void addSymCoverageTier(TierGlyph atier) {
    MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)gmodel.getSelectedSeq();
    int child_count = atier.getChildCount();
    java.util.List syms = new ArrayList(child_count);
    collectSyms(atier, syms);

    SeqSymmetry union_sym = SeqSymSummarizer.getUnion(syms, aseq);
    SimpleSymWithProps wrapperSym = new SimpleSymWithProps();
    wrapperSym.setProperty("method", ("coverage: " + atier.getLabel()));
    wrapperSym.addChild(union_sym);
    aseq.addAnnotation(wrapperSym);
    gviewer.setAnnotatedSeq(aseq, true, true);
  }


  public void addSymSummaryTier(TierGlyph atier) {
    if (DEBUG) {
    System.out.println("trying to summarize syms in tier");
    }
    // not sure best way to collect syms from tier, but for now,
    //   just recursively descend through child glyphs of the tier, and if
    //   childA.getInfo() is a SeqSymmetry, add to symmetry list and prune recursion
    //   (don't descend into childA's children)
    java.util.List syms = new ArrayList();
    collectSyms(atier, syms);
    MutableAnnotatedBioSeq aseq = (MutableAnnotatedBioSeq)gmodel.getSelectedSeq();
    GraphSym gsym = SeqSymSummarizer.getSymmetrySummary(syms, aseq);
    gsym.setGraphName("depth: " + atier.getLabel());
    aseq.addAnnotation(gsym);
    gviewer.setAnnotatedSeq(aseq, true, true);
    GraphGlyph gl = (GraphGlyph)((SeqMapView)gviewer).getSeqMap().getItem(gsym);
    //    gl.setState(GraphGlyph.STAIRSTEP);
    gl.setGraphStyle(GraphGlyph.STAIRSTEP_GRAPH);
    gl.setColor(atier.getForegroundColor());
    // System.out.println("glyph: " + gl);
    // System.out.println("datamodel: " + gsym);
  }

  /** Deprecated */
  public void addGlyphSummaryTier(TierGlyph atier) {
    // find index of atier, to put summary tier right below atier in tier ordering

    int index = getTierIndex(atier);
    if (index == -1) { return; } // atier not found in map, so don't bother continuing

    GlyphSummarizer summarizer = new GlyphSummarizer();
    summarizer.setScaleFactor(2.0f);
    //    summarizer.setFilterZeros(true);
    summarizer.setFilterZeros(false);
    summarizer.setColor(Color.yellow);

    // getSummaryGlyph returns a SmartGraphGlyph
    GlyphI sumglyph2 = summarizer.getSummaryGlyph(atier, tiermap);
    TierGlyph sumtier2 = new TierGlyph();
    sumglyph2.setColor(atier.getForegroundColor());
    sumtier2.setLabel(atier.getLabel() + " sum");
    sumtier2.setFillColor(Color.darkGray);
    sumtier2.setForegroundColor(atier.getForegroundColor());
    sumtier2.addChild(sumglyph2);
    //    sumtier2.setState(sumtier.COLLAPSED);
    tiermap.addTier(sumtier2, index);
    sumtier2.pack(tiermap.getView());

    tiermap.packTiers(false, true, false);
    tiermap.stretchToFit(false, false);
    tiermap.updateWidget();
  }


  public void showTier(TierGlyph tier) {
    //System.out.println("trying to show tier");
    tier.restoreState();
    JMenuItem item = (JMenuItem)tier2showitem.get(tier);
    if (item != null) {
      showMenu.remove(item);
    }
    tier2showitem.remove(tier);
    tiermap.packTiers(false, true, false);
    tiermap.stretchToFit(false, false);
    verifyZoom();
    tiermap.updateWidget();
  }

  /** Shows all tiers that are keys in the tier2showitem and then clears tier2showitem.
   */
  protected void showAllTiers() {
    Enumeration e = tier2showitem.keys();
    while (e.hasMoreElements()) {
      TierGlyph tier = (TierGlyph) e.nextElement();
      tier.restoreState();
      JMenuItem item = (JMenuItem)tier2showitem.get(tier);
      if (item != null) {
        showMenu.remove(item);
      }
    }
    tier2showitem.clear();
    tiermap.packTiers(false, true, false);
    tiermap.stretchToFit(false, false);
    verifyZoom();
    tiermap.updateWidget();
  }

  /** Hides one tier and creates a JMenuItem that can be used to show it again.
   *  Does not re-pack the given tier, or any other tiers.
   */
  protected void hideOneTier(final TierGlyph tier) {
    tier.setState(current_tier.HIDDEN);
    String label = tier.getLabel();
    if (label == null) label = "unlabeled tier";
    AbstractAction show_action = new AbstractAction(label) {
      public void actionPerformed(ActionEvent ae) {
	showTier(tier);
      }
    };
    JMenuItem show_tier_item = showMenu.add(show_action);
    tier2showitem.put(tier, show_tier_item);
  }

  /** Hides multiple tiers and then repacks.
   *  @param tiers  a List of GlyphI objects for each of which getInfo() returns a TierGlyph.
   */
  public void hideTiers(java.util.List tiers) {
    Iterator iter = tiers.iterator();
    while (iter.hasNext()) {
      GlyphI g = (GlyphI) iter.next();
      if (g.getInfo() instanceof TierGlyph) {
        TierGlyph tier = (TierGlyph) g.getInfo();
        hideOneTier(tier);
       // tier.pack(tiermap.getView());
      }
    }

    tiermap.packTiers(false, true, false);
    tiermap.stretchToFit(false, false);
    verifyZoom();
    tiermap.updateWidget();
  }

  public void mouseEntered(MouseEvent evt) { }
  public void mouseExited(MouseEvent evt) { }

  /** Tests whether the mouse event is due to the 3rd button.
   *  (For the sake of Macintosh, considers Meta key and Control key as
   *  simulation of 3rd button.)
   */
  boolean isOurPopupTrigger(MouseEvent evt) {
    int mods = evt.getModifiers();
    return ( evt.isMetaDown() || evt.isControlDown() ||
	 ((mods & InputEvent.BUTTON3_MASK) != 0)  );
  }

  public void mouseClicked(MouseEvent evt) {}

  public void mousePressed(MouseEvent evt) {
    if (evt instanceof NeoMouseEvent) {
      if (evt.getSource() != labelmap) { return; }
      NeoMouseEvent nevt = (NeoMouseEvent)evt;
      Vector selected_glyphs = nevt.getItems();
      GlyphI topgl = null;
      if (!selected_glyphs.isEmpty()) {
        topgl = (GlyphI) selected_glyphs.lastElement();
      }
      // Normally, clicking will clear previons selections before selecting new things.
      // but we preserve the current selections if:
      //  1. shift or alt key is pressed, or
      //  2. the pop-up key is being pressed
      //     2a. on top of nothing
      //     2b. on top of something previously selected
      boolean preserve_selections = false;
      if (nevt.isAltDown() || nevt.isShiftDown()) {
        preserve_selections = true;
      }
      else if (topgl != null && isOurPopupTrigger(nevt)) {
        if (labelmap.getSelected().contains(topgl)) {
          preserve_selections = true;
        }
      }
      if (! preserve_selections ) {
	labelmap.clearSelected();
      }
      Vector selected = nevt.getItems();
      labelmap.select(selected);
      if (selected.size() > 0) {
	// take glyph at end of selected, just in case there is more
	//    than one -- the last one should be on top...
	GlyphI gl = (GlyphI)selected.elementAt(selected.size()-1);
	labelmap.toFront(gl);
	dragLabel(gl, nevt);
      }
      if ( isOurPopupTrigger(evt)  ) {
        // Note: Checking for popup trigger inside mouseClicked() does not
        // work on the Macintosh.  Checking it here in mousePressed() seems to
        // work on all of Mac/Windows/Linux.
        if (! selected.isEmpty()) {
          current_label = (GlyphI) selected.lastElement();
          setCurrentTier((TierGlyph)current_label.getInfo());
        } else {
          current_label = null;
          setCurrentTier(null);
        }
        rebuildPopup(current_tier);
        popup.show(labelmap, nevt.getX()+xoffset_pop, nevt.getY()+yoffset_pop);
      }
    }
  }

  /** comparator class needed to sort tiers based on label placement */
  public class MinYSorter implements Comparator {
    public int compare(Object obj1, Object obj2) {
      Rectangle2D box1 = ((GlyphI)obj1).getCoordBox();
      Rectangle2D box2 = ((GlyphI)obj2).getCoordBox();
      if (box1.y < box2.y) { return -1; }
      else if (box1.y > box2.y) { return 1; }
      else { return 0; }
    }
  }

  Comparator ysorter = new MinYSorter();

  // if a tier has been dragged, then try to sort out rearrangement of tiers
  //    in tiermap based on new positions of labels in labelmap
  public void mouseReleased(MouseEvent evt) {
    if (evt.getSource() == labelmap &&
	evt instanceof NeoMouseEvent &&
	dragging_label) {
      orderTiersByLabel();
    }
  }

  // sort new arrangement of tiers for tiermap based
  //    on vertical ordering of current label positions in labelmap
  public void orderTiersByLabel() {
      java.util.List label_glyphs = tiermap.getTierLabels();
      int tierCount = label_glyphs.size();
      // get an ordering of tiers based on label positions
      //    assuming tiers are always packed down in tiermap
      //    (which is currently true)
      // therefore order the label glyphs based on y position
      Collections.sort(label_glyphs, ysorter);

      // then reorder tiermap glyphs based on sorted label glyphs

      // mucking directly with tiermap's tier Vector, which is not
      //     the cleanest way to do this, but is efficient...
      Vector tiervec = tiermap.getAllTiers();

      //      Vector neworder = new Vector(tiervec.size());
      tiervec.removeAllElements();
      for (int i=0; i<tierCount; i++) {
	GlyphI label = (GlyphI)label_glyphs.get(i);
	TierGlyph tier = (TierGlyph)label.getInfo();
	//	tiermap.moveTier(tier, i);
	//	neworder.add(tier);
	tiervec.add(tier);
      }
      dragging_label = false;
      current_label = null;

      // then repack of course (tiermap repack also redoes labelmap glyph coords...)
      tiermap.packTiers(false, true, false);
      tiermap.updateWidget();
  }

  public void dragLabel(GlyphI gl, NeoMouseEvent nevt) {
    dragging_label = true;
    current_label = gl;
    GlyphDragger dragger = new GlyphDragger((NeoWidgetI)nevt.getSource());
    dragger.setUseCopy(false);
    dragger.startDrag(gl, nevt);
    dragger.setConstraint(NeoWidgetI.HORIZONTAL, true);
  }


  void rebuildPopup(TierGlyph tgl) {
    popup.removeAll();
    java.util.List items = getMenuList(tgl);
    for (int i=0; i<items.size(); i++) {
      Object item = items.get(i);
      if (item instanceof JMenuItem) { popup.add((JMenuItem)item); }
      else if (item instanceof Action) { popup.add((Action)item); }
      else if (item instanceof String) { popup.add((String)item); }
      else if (item instanceof JSeparator) { popup.addSeparator(); }
    }
    Vector selected = labelmap.getSelected();
    int num_selected = selected.size();
    a_not_b_MI.setEnabled(num_selected==2);
    b_not_a_MI.setEnabled(num_selected==2);
    changeExpandMaxMI.setEnabled(num_selected==1);
    changeExpandMaxAllMI.setEnabled(true);
    // If num_selected > 1, always enable the Collapse item,
    // but if exactly one, disable Collapse if the selection is already collapsed
    collapseMI.setEnabled(num_selected != 0);
    if (num_selected == 1 && (tgl == null || tgl.getState()==TierGlyph.COLLAPSED)) {
      collapseMI.setEnabled(false);
    }
    collapseAllMI.setEnabled(true);
    expandMI.setEnabled(num_selected != 0);
    if (num_selected == 1 && (tgl == null || tgl.getState()==TierGlyph.EXPANDED)) {
      expandMI.setEnabled(false);
    }
    expandAllMI.setEnabled(true);
    hideMI.setEnabled(num_selected != 0);
    intersectMI.setEnabled(num_selected==2);
    notMI.setEnabled(num_selected==1);
    saveBedMI.setEnabled(num_selected==1);
    sym_summarizeMI.setEnabled(num_selected==1);
    glyph_summarizeMI.setEnabled(num_selected==1);
    unionMI.setEnabled(num_selected==2);
    xorMI.setEnabled(num_selected==2);
    showMenu.setEnabled(! tier2showitem.isEmpty());
    showAllMI.setEnabled(! tier2showitem.isEmpty());
    combineMenu.setEnabled(num_selected==1 || num_selected==2);
    color_childrenMI.setEnabled(num_selected != 0);
  }

  // A hack
  void verifyZoom() {
    if ((tiermap.getZoom(tiermap.Y) < tiermap.getMinZoom(tiermap.Y))
      || (tiermap.getZoom(tiermap.Y) > tiermap.getMaxZoom(tiermap.Y))) {
      if (DEBUG) {
        System.out.println("fixing problem with scene vs view coords after tier collapse");
        System.out.println("current zoom: " + tiermap.getZoom(tiermap.Y));
        System.out.println("min zoom: " + tiermap.getMinZoom(tiermap.Y));
      }
      tiermap.stretchToFit(false, true);
    }
  }


  public void adjustMap() {
    tiermap.repack();
    tiermap.stretchToFit(false, true);
    tiermap.updateWidget();
  }

}


