package com.affymetrix.igb.tiers;

import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.genometry.*;
import com.affymetrix.igb.genometry.SingletonGenometryModel;
import com.affymetrix.igb.glyph.*;
import com.affymetrix.igb.menuitem.FileTracker;
import com.affymetrix.igb.parsers.BedParser;
import com.affymetrix.igb.util.*;
import com.affymetrix.igb.view.*;
import java.awt.Color;

public class SeqMapViewPopup implements TierLabelManager.PopupListener {
  
  static final boolean DEBUG = false;

  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  AnnotatedSeqViewer gviewer;
  TierLabelManager handler;
  
  Action customize_action = new AbstractAction("Customize") {
    public void actionPerformed(ActionEvent e) {
      showCustomizer();
    }
  };
  
  Action expand_action = new AbstractAction("Expand") {
    public void actionPerformed(ActionEvent e) {
      setTiersCollapsed(handler.getSelectedTierLabels(), false);
    }
  };

  Action expand_all_action = new AbstractAction("Expand All") {
    public void actionPerformed(ActionEvent e) {
      setTiersCollapsed(handler.getAllTierLabels(), false);
    }
  };

  Action collapse_action = new AbstractAction("Collapse") {
    public void actionPerformed(ActionEvent e) {
      setTiersCollapsed(handler.getSelectedTierLabels(), true);
    }
  };

  Action collapse_all_action = new AbstractAction("Collapse All") {
    public void actionPerformed(ActionEvent e) {
      setTiersCollapsed(handler.getAllTierLabels(), true);
    }
  };
  
  Action hide_action = new AbstractAction("Hide") {
    public void actionPerformed(ActionEvent e) {
      hideTiers(handler.getSelectedTierLabels());
    }
  };
  
  Action show_all_action = new AbstractAction("Show All") {
    public void actionPerformed(ActionEvent e) {
      showAllTiers();
    }
  };
  
  Action change_color_action = new AbstractAction("Change Annotation Color") {
    public void actionPerformed(ActionEvent e) {
      changeColor(handler.getSelectedTierLabels());
    }
  };
  
  Action sym_summarize_action = new AbstractAction("Make Annotation Depth Track") {
    public void actionPerformed(ActionEvent e) {
      java.util.List current_tiers = handler.getSelectedTiers();
      if (current_tiers.size() > 1) {
        ErrorHandler.errorPanel("Must select only one tier");
      }
      TierGlyph current_tier = (TierGlyph) current_tiers.get(0);
      addSymSummaryTier(current_tier);
    }
  };
  Action coverage_action = new AbstractAction("Make Annotation Coverage Track") {
    public void actionPerformed(ActionEvent e) {
      java.util.List current_tiers = handler.getSelectedTiers();
      if (current_tiers.size() > 1) {
        ErrorHandler.errorPanel("Must select only one tier");
      }
      TierGlyph current_tier = (TierGlyph) current_tiers.get(0);
      addSymCoverageTier(current_tier);
    }
  };
  Action save_bed_action = new AbstractAction("Save tier as BED file") {
    public void actionPerformed(ActionEvent e) {
      java.util.List current_tiers = handler.getSelectedTiers();
      if (current_tiers.size() > 1) {
        ErrorHandler.errorPanel("Must select only one tier");
      }
      TierGlyph current_tier = (TierGlyph) current_tiers.get(0);
      saveAsBedFile(current_tier);
    }
  };

  Action change_expand_max_action = new AbstractAction("Adjust Max Expand") {
    public void actionPerformed(ActionEvent e) {
      changeExpandMax(handler.getSelectedTierLabels());
    }
  };

  Action change_expand_max_all_action = new AbstractAction("Adjust Max Expand All") {
    public void actionPerformed(ActionEvent e) {
      changeExpandMax(handler.getAllTierLabels());
    }
  };
  
  public SeqMapViewPopup(TierLabelManager handler, AnnotatedSeqViewer gviewer) {
    this.handler = handler;
    this.gviewer = gviewer;
  }


  void showCustomizer() {
    TierPrefsView.showFrame();
  }
  
  java.util.List getStyles(java.util.List tier_label_glyphs) {
    if (tier_label_glyphs.size() == 0) { return Collections.EMPTY_LIST; }
    
    // styles is a list of styles with no duplicates, so a Set rather than a List
    // might make sense.  But at the moment it seems faster to use a List
    java.util.List styles = new ArrayList(tier_label_glyphs.size());

    for (int i=0; i<tier_label_glyphs.size(); i++) {
      TierLabelGlyph tlg = (TierLabelGlyph) tier_label_glyphs.get(i);
      TierGlyph tier = tlg.getReferenceTier();
      AnnotStyle tps = tier.getAnnotStyle();
      if (tps != null && ! styles.contains(tps)) styles.add(tps);
    }
    return styles;
  }

  void setTiersCollapsed(java.util.List tier_labels, boolean collapsed) {
    java.util.List styles = getStyles(tier_labels);
    for (int i=0; i<styles.size(); i++) {
      AnnotStyle style = (AnnotStyle) styles.get(i);
      style.setCollapsed(collapsed);
    }
    refreshMap(false);
  }
  
  public void changeExpandMax(java.util.List tier_labels) {
    if (tier_labels == null || tier_labels.size() == 0) {
      ErrorHandler.errorPanel("changeExpandMaxAll called with an empty list");
      return;
    }
    
    String initial_value = "0";
    if (tier_labels.size() == 1) {
      TierLabelGlyph tlg = (TierLabelGlyph) tier_labels.get(0);
      TierGlyph tg = (TierGlyph) tlg.getInfo();
      AnnotStyle style = tg.getAnnotStyle();
      if (style != null) { initial_value = "" + style.getMaxDepth(); }
    }
    
    String input =
      (String)JOptionPane.showInputDialog(null,
					  "Enter new maximum tier height, 0 for unlimited",
					  "Change Selected Tiers Max Height", JOptionPane.PLAIN_MESSAGE,
					  null, null, initial_value);

    if (input == JOptionPane.UNINITIALIZED_VALUE || !(input instanceof String)) {
      return;
    }

    int newmax;
    try {
      newmax = Integer.parseInt(input);
    }
    catch (NumberFormatException ex) {
      ErrorHandler.errorPanel("Couldn't parse new tier max '"+input+"'");
      return;
    }
    
    changeExpandMax(tier_labels, newmax);
  }
  
  void changeExpandMax(java.util.List tier_label_glyphs, int max) {
    for (int i=0; i<tier_label_glyphs.size(); i++) {
      TierLabelGlyph tlg = (TierLabelGlyph) tier_label_glyphs.get(i);
      TierGlyph tier = (TierGlyph) tlg.getInfo();
      AnnotStyle style = tier.getAnnotStyle();

      if (style != null) {
        style.setMaxDepth(max); 
      }
      tier.setMaxExpandDepth(max);
    }
    refreshMap(false);
  }

  public void showAllTiers() {
    java.util.List tiervec = handler.getAllTierLabels();

    for (int i=0; i<tiervec.size(); i++) {
      TierLabelGlyph label = (TierLabelGlyph) tiervec.get(i);
      TierGlyph tier = (TierGlyph) label.getInfo();
      tier.restoreState();
      AnnotStyle style = tier.getAnnotStyle();
      if (style != null) {
        style.setShow(true);
      }
    }
    refreshMap(true);
  }

  /** Hides one tier and creates a JMenuItem that can be used to show it again.
   *  Does not re-pack the given tier, or any other tiers.
   */
  protected void hideOneTier(final TierGlyph tier) {
    tier.setState(TierGlyph.HIDDEN);
    AnnotStyle style = tier.getAnnotStyle();
    if (style != null) {
      style.setShow(false);
    }
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
        //tier.pack(tiermap.getView());
      }
    }

    refreshMap(false);
  }
  
  // although this will work on a list of selected tier labels, it is
  // probably more intuitive if its use is restricted to a single tier.
  public void changeColor(final java.util.List tier_label_glyphs) {
    if (tier_label_glyphs.isEmpty()) {
      return;
    }
    
    final JColorChooser chooser = new JColorChooser();
    
    TierLabelGlyph tlg_0 = (TierLabelGlyph) tier_label_glyphs.get(0);
    TierGlyph tier_0 = (TierGlyph) tlg_0.getInfo();
    AnnotStyle style_0 = tier_0.getAnnotStyle();
    if (style_0 != null) {
      chooser.setColor(style_0.getColor());
    }
    
    ActionListener al = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        for (int i=0; i<tier_label_glyphs.size(); i++) {
          TierLabelGlyph tlg = (TierLabelGlyph) tier_label_glyphs.get(i);
          TierGlyph tier = (TierGlyph) tlg.getInfo();
          AnnotStyle style = tier.getAnnotStyle();

          if (style != null) { style.setColor(chooser.getColor()); }
        }
      }
    };
    
    JDialog dialog = JColorChooser.createDialog((java.awt.Component) null, // parent
                                        "Pick a Color",
                                        true,  //modal
                                        chooser,
                                        al,  //OK button handler
                                        null); //no CANCEL button handler
    dialog.show();

    refreshMap(false);
  }
  
  public void saveAsBedFile(TierGlyph atier) {
    int childcount= atier.getChildCount();
    java.util.List syms = new ArrayList(childcount);
    for (int i=0; i<childcount; i++) {
      GlyphI child = atier.getChild(i);
      if (child.getInfo() instanceof SeqSymmetry) {
	syms.add(child.getInfo());
      }
    }
    System.out.println("Saving symmetries: "+ syms.size());
//    com.affymetrix.genometry.util.SeqUtils.printSymmetry((SeqSymmetry) syms.get(0));
    
    JFileChooser chooser = UniFileChooser.getFileChooser("Bed file (*.bed)", "bed");
    chooser.setCurrentDirectory(FileTracker.DATA_DIR_TRACKER.getFile());
        
    int option = chooser.showSaveDialog(null);
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
	ErrorHandler.errorPanel("Problem saving file", ex);
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
    if (child_count == 0 || syms.size() == 0) {
      ErrorHandler.errorPanel("Empty Tier",
        "The selected tier is empty.  Can not make a coverage tier for an empty tier.");
      return;
    }

    SeqSymmetry union_sym = SeqSymSummarizer.getUnion(syms, aseq);
    SymWithProps wrapperSym;;
    if (union_sym instanceof SymWithProps) {
      wrapperSym = (SymWithProps) union_sym;
    } else {
      wrapperSym = new SimpleSymWithProps();
      ((SimpleSymWithProps) wrapperSym).addChild(union_sym);
      for (int i=0; i<union_sym.getSpanCount(); i++) {
        ((SimpleSymWithProps) wrapperSym).addSpan(union_sym.getSpan(i));
      }
    }
       
    String method = "coverage: " + atier.getLabel();
    wrapperSym.setProperty("method", method);

    // Generate a non-persistent style.  Factory should be CoverageSummarizerFactory
    AnnotStyle style = AnnotStyle.getInstance(method, false);
    
    aseq.addAnnotation(wrapperSym);
    gviewer.setAnnotatedSeq(aseq, true, true);
  }


  public void addSymSummaryTier(TierGlyph atier) {
    // not sure best way to collect syms from tier, but for now,
    //   just recursively descend through child glyphs of the tier, and if
    //   childA.getInfo() is a SeqSymmetry, add to symmetry list and prune recursion
    //   (don't descend into childA's children)
    java.util.List syms = new ArrayList();
    collectSyms(atier, syms);
    if (syms.size() == 0) {
      ErrorHandler.errorPanel("Nothing to Summarize",
        "The selected tier is empty. It contains nothing to summarize");
      return;
    }
    
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

  void refreshMap(boolean stretch_vertically) {
    if (gviewer != null) {
      // if an AnnotatedSeqViewer is being used, ask it to update itself.
      // later this can be made more specific to just update the tiers that changed
      gviewer.setAnnotatedSeq(gviewer.getAnnotatedSeq(), true, true);
    } else {
      // if no AnnotatedSeqViewer (as in simple test programs), update the tiermap itself.
      handler.repackTheTiers(false, stretch_vertically);
    }
  }
  
  public void popupNotify(javax.swing.JPopupMenu popup, TierLabelManager handler) {
    int num_selections = handler.getSelectedTierLabels().size();
    boolean not_empty = ! handler.getAllTierLabels().isEmpty();
    
    customize_action.setEnabled(true);

    hide_action.setEnabled(num_selections > 0);
    show_all_action.setEnabled(not_empty);
    
    change_color_action.setEnabled(num_selections > 0);

    collapse_action.setEnabled(num_selections > 0);
    expand_action.setEnabled(num_selections > 0);
    change_expand_max_action.setEnabled(num_selections > 0);
    collapse_all_action.setEnabled(not_empty);
    expand_all_action.setEnabled(not_empty);
    change_expand_max_all_action.setEnabled(not_empty);

    save_bed_action.setEnabled(num_selections == 1);

    sym_summarize_action.setEnabled(num_selections == 1);
    coverage_action.setEnabled(num_selections == 1);
    
    popup.add(customize_action);
    popup.add(new JSeparator());
    popup.add(hide_action);
    popup.add(show_all_action);
    popup.add(new JSeparator());
    popup.add(change_color_action);
    popup.add(new JSeparator());
    popup.add(collapse_action);
    popup.add(expand_action);
    popup.add(change_expand_max_action);
    popup.add(collapse_all_action);
    popup.add(expand_all_action);
    popup.add(change_expand_max_all_action);
    popup.add(new JSeparator());
    popup.add(save_bed_action);
    popup.add(new JSeparator());
    popup.add(sym_summarize_action);
    popup.add(coverage_action);
    
    if (DEBUG) {
      popup.add(new AbstractAction("DEBUG") {
        public void actionPerformed(ActionEvent e) {
          doDebugAction();
        }
      });
    }
  }
  
  // purely for debugging
  void doDebugAction() {
    if (DEBUG) {
      java.util.List current_tiers = handler.getSelectedTiers();
      Iterator iter = current_tiers.iterator();
      while (iter.hasNext()) {
        TierGlyph tg = (TierGlyph) iter.next();
        AnnotStyle style = (AnnotStyle) tg.getAnnotStyle();
        System.out.println("Tier: " + tg);
        System.out.println("Style: " + style);
      }
    }
  }
}
