/**
*   Copyright (c) 2005-2007 Affymetrix, Inc.
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

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.MutableAnnotatedBioSeq;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.util.UniFileChooser;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.SeqSymSummarizer;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.style.IAnnotStyle;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.glyph.*;
import com.affymetrix.igb.menuitem.FileTracker;
import com.affymetrix.genometryImpl.parsers.BedParser;
import com.affymetrix.genometryImpl.style.IAnnotStyleExtended;
import com.affymetrix.genometryImpl.style.GraphStateI;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.tiers.AffyTieredMap.ActionToggler;
import com.affymetrix.igb.view.*;
import com.affymetrix.igb.view.AnnotatedSeqViewer;

public final class SeqMapViewPopup implements TierLabelManager.PopupListener {

  private static final boolean DEBUG = false;
  
  private static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  private AnnotatedSeqViewer gviewer;
  private TierLabelManager handler;

  private final JMenu showMenu = new JMenu("Show...");
  private final JMenu changeMenu = new JMenu("Change...");
  private final JMenu strandsMenu = new JMenu("Strands...");
  private final JMenuItem deleteMI = new JMenuItem("Delete");

  private ActionToggler at1;
  private ActionToggler at2;
  private ActionToggler at3;

  private Action select_all_tiers_action = new AbstractAction("Select All Tiers") {
    public void actionPerformed(ActionEvent e) {
      handler.selectAllTiers();
    }
  };

  private Action rename_action = new AbstractAction("Change Display Name") {
    public void actionPerformed(ActionEvent e) {
      List current_tiers = handler.getSelectedTiers();
      if (current_tiers.size() != 1) {
        ErrorHandler.errorPanel("Must select only one tier");
      }
      TierGlyph current_tier = (TierGlyph) current_tiers.get(0);
      renameTier(current_tier);
    }
  };

  private Action customize_action = new AbstractAction("Customize") {
    public void actionPerformed(ActionEvent e) {
      showCustomizer();
    }
  };

  private Action expand_action = new AbstractAction("Expand") {
    public void actionPerformed(ActionEvent e) {
      setTiersCollapsed(handler.getSelectedTierLabels(), false);
    }
  };

  private Action expand_all_action = new AbstractAction("Expand All") {
    public void actionPerformed(ActionEvent e) {
      setTiersCollapsed(handler.getAllTierLabels(), false);
    }
  };

  private Action collapse_action = new AbstractAction("Collapse") {
    public void actionPerformed(ActionEvent e) {
      setTiersCollapsed(handler.getSelectedTierLabels(), true);
    }
  };

  private Action collapse_all_action = new AbstractAction("Collapse All") {
    public void actionPerformed(ActionEvent e) {
      setTiersCollapsed(handler.getAllTierLabels(), true);
    }
  };

  private Action hide_action = new AbstractAction("Hide") {
    public void actionPerformed(ActionEvent e) {
      hideTiers(handler.getSelectedTierLabels());
    }
  };

  private Action show_all_action = new AbstractAction("Show All Types") {
    public void actionPerformed(ActionEvent e) {
      showAllTiers();
    }
  };

  private Action change_color_action = new AbstractAction("Change FG Color") {
    public void actionPerformed(ActionEvent e) {
      changeColor(handler.getSelectedTierLabels(), true);
    }
  };

  private Action change_bg_color_action = new AbstractAction("Change BG Color") {
    public void actionPerformed(ActionEvent e) {
      changeColor(handler.getSelectedTierLabels(), false);
    }
  };

  private Action color_by_score_on_action = new AbstractAction("Color By Score ON") {
    public void actionPerformed(ActionEvent e) {
      setColorByScore(handler.getSelectedTierLabels(), true);
    }
  };

  private Action color_by_score_off_action = new AbstractAction("Color By Score OFF") {
    public void actionPerformed(ActionEvent e) {
      setColorByScore(handler.getSelectedTierLabels(), false);
    }
  };

  private Action show_two_tiers = new AbstractAction("Show 2 tiers (+) and (-)") {
    public void actionPerformed(ActionEvent e) {
      setTwoTiers(handler.getSelectedTierLabels(), true);
    }
  };

  private Action show_single_tier = new AbstractAction("Show 1 tier (+/-)") {
    public void actionPerformed(ActionEvent e) {
      setTwoTiers(handler.getSelectedTierLabels(), false);
    }
  };
  
  private Action sym_summarize_action = new AbstractAction("Make Annotation Depth Graph") {
    public void actionPerformed(ActionEvent e) {
      List current_tiers = handler.getSelectedTiers();
      if (current_tiers.size() > 1) {
        ErrorHandler.errorPanel("Must select only one tier");
      }
      TierGlyph current_tier = (TierGlyph) current_tiers.get(0);
      addSymSummaryTier(current_tier);
    }
  };
  private Action coverage_action = new AbstractAction("Make Annotation Coverage Track") {
    public void actionPerformed(ActionEvent e) {
      List current_tiers = handler.getSelectedTiers();
      if (current_tiers.size() > 1) {
        ErrorHandler.errorPanel("Must select only one tier");
      }
      TierGlyph current_tier = (TierGlyph) current_tiers.get(0);
      addSymCoverageTier(current_tier);
    }
  };
  private Action save_bed_action = new AbstractAction("Save tier as BED file") {
    public void actionPerformed(ActionEvent e) {
      List<TierGlyph> current_tiers = handler.getSelectedTiers();
      if (current_tiers.size() > 1) {
        ErrorHandler.errorPanel("Must select only one tier");
      }
      TierGlyph current_tier = current_tiers.get(0);
      saveAsBedFile(current_tier);
    }
  };

  private Action change_expand_max_action = new AbstractAction("Adjust Max Expand") {
    public void actionPerformed(ActionEvent e) {
      changeExpandMax(handler.getSelectedTierLabels());
    }
  };

  private Action change_expand_max_all_action = new AbstractAction("Adjust Max Expand All") {
    public void actionPerformed(ActionEvent e) {
      changeExpandMax(handler.getAllTierLabels());
    }
  };

  
  private Action delete_action = new AbstractAction("Delete selected tiers (NOT IMPLEMENTED") {
    public void actionPerformed(ActionEvent e) {
      List current_tiers = handler.getSelectedTierLabels();
      if (Application.confirmPanel("Really remove selected tiers?\n"+
          "Data will be removed from all chromosomes on this genome.")) {
        removeTiers(current_tiers);
      }
    }
  };


  public SeqMapViewPopup(TierLabelManager handler, AnnotatedSeqViewer gviewer) {
    this.handler = handler;
    this.gviewer = gviewer;
    if (gviewer instanceof SeqMapView) {
      SeqMapView smv = (SeqMapView) gviewer;
      at1 = new ActionToggler(smv.getSeqMap().show_plus_action);
      at2 = new ActionToggler(smv.getSeqMap().show_minus_action);
      at3 = new ActionToggler(smv.getSeqMap().show_mixed_action);
    }
  }

  private void showCustomizer() {
    PreferencesPanel pv = PreferencesPanel.getSingleton();
    pv.setTab(PreferencesPanel.TAB_NUM_TIERS);
    JFrame f = pv.getFrame();
    f.setVisible(true);
  }

  List<IAnnotStyle> getStyles(List<TierLabelGlyph> tier_label_glyphs) {
    if (tier_label_glyphs.size() == 0) { return Collections.<IAnnotStyle>emptyList(); }

    // styles is a list of styles with no duplicates, so a Set rather than a List
    // might make sense.  But at the moment it seems faster to use a List
    List<IAnnotStyle> styles = new ArrayList<IAnnotStyle>(tier_label_glyphs.size());

		for (TierLabelGlyph tlg : tier_label_glyphs) {
      TierGlyph tier = tlg.getReferenceTier();
      IAnnotStyle tps = tier.getAnnotStyle();
      if (tps != null && ! styles.contains(tps)) styles.add(tps);
    }
    return styles;
  }

  private void setTiersCollapsed(List<TierLabelGlyph> tier_labels, boolean collapsed) {
    handler.setTiersCollapsed(tier_labels, collapsed, true, true);
    refreshMap(true);
  }

  public void changeExpandMax(List<TierLabelGlyph> tier_labels) {
    if (tier_labels == null || tier_labels.size() == 0) {
      ErrorHandler.errorPanel("changeExpandMaxAll called with an empty list");
      return;
    }

    String initial_value = "0";
    if (tier_labels.size() == 1) {
      TierLabelGlyph tlg = tier_labels.get(0);
      TierGlyph tg = (TierGlyph) tlg.getInfo();
      IAnnotStyle style = tg.getAnnotStyle();
      if (style != null) { initial_value = "" + style.getMaxDepth(); }
    }

    String input =
      (String)JOptionPane.showInputDialog(null,
					  "Enter new maximum tier height, 0 for unlimited",
					  "Change Selected Tiers Max Height", JOptionPane.PLAIN_MESSAGE,
					  null, null, initial_value);

    if ( input == null || input.equals(JOptionPane.UNINITIALIZED_VALUE)) {
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

	private void changeExpandMax(List<TierLabelGlyph> tier_label_glyphs, int max) {
		for (TierLabelGlyph tlg : tier_label_glyphs) {
			TierGlyph tier = (TierGlyph) tlg.getInfo();
			IAnnotStyle style = tier.getAnnotStyle();
			style.setMaxDepth(max);
			tier.setMaxExpandDepth(max);
		}
		refreshMap(false);
	}

  private void setTwoTiers(List<TierLabelGlyph> tier_label_glyphs, boolean b) {
    for (TierLabelGlyph tlg : tier_label_glyphs) {
      TierGlyph tier = (TierGlyph) tlg.getInfo();
      IAnnotStyle style = tier.getAnnotStyle();
      if (style instanceof IAnnotStyleExtended) {
        ((IAnnotStyleExtended) style).setSeparate(b);
      }
    }
    refreshMap(false);
    handler.sortTiers();
  }

  private void showAllTiers() {
	  List<TierLabelGlyph> tiervec = handler.getAllTierLabels();

	  for (TierLabelGlyph label : tiervec) {
		  TierGlyph tier = (TierGlyph) label.getInfo();
		  IAnnotStyle style = tier.getAnnotStyle();
		  if (style != null) {
			  style.setShow(true);
			  tier.restoreState();
		  }
	  }
	  showMenu.removeAll();
	  refreshMap(true); // when re-showing all tiers, do strech_to_fit in the y-direction
	  handler.sortTiers();
	}

  /** Hides one tier and creates a JMenuItem that can be used to show it again.
   *  Does not re-pack the given tier, or any other tiers.
   */
  private void hideOneTier(final TierGlyph tier) {
    final IAnnotStyle style = tier.getAnnotStyle();
    // if style.getShow() is already false, there is likely a bug somewhere!
		if (style == null) {
			return;
		}
    if (style.getShow()) {
      style.setShow(false);
      final JMenuItem show_tier = new JMenuItem() {
        // override getText() because the HumanName of the style might change
				@Override
        public String getText() {
          String name = style.getHumanName();
          if (name == null) { name = "<unnamed>"; }
          if (name.length() > 30) {
            name = name.substring(0,30) + "...";
          }
          return name;
        }
      };
      show_tier.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          style.setShow(true);
          showMenu.remove(show_tier);
          refreshMap(false);
          handler.sortTiers();
        }
      });
      showMenu.add(show_tier);
    }
    if (! style.getShow()) {
      tier.setState(TierGlyph.HIDDEN);
    }
  }

  /** Hides multiple tiers and then repacks.
   *  @param tiers  a List of GlyphI objects for each of which getInfo() returns a TierGlyph.
   */
  private void hideTiers(List<GlyphI> tiers) {
    for (GlyphI g : tiers) {
      if (g.getInfo() instanceof TierGlyph) {
        TierGlyph tier = (TierGlyph) g.getInfo();
        hideOneTier(tier);
      }
    }

    refreshMap(false);
  }

  private void changeColor(final List<TierLabelGlyph> tier_label_glyphs, final boolean fg) {
    if (tier_label_glyphs.isEmpty()) {
      return;
    }

    final JColorChooser chooser = new JColorChooser();

    TierLabelGlyph tlg_0 = tier_label_glyphs.get(0);
    TierGlyph tier_0 = (TierGlyph) tlg_0.getInfo();
    IAnnotStyle style_0 = tier_0.getAnnotStyle();
    if (style_0 != null) {
      if (fg) {
        chooser.setColor(style_0.getColor());
      } else {
        chooser.setColor(style_0.getBackground());
      }
    }

    ActionListener al = new ActionListener() {

		  public void actionPerformed(ActionEvent e) {
			  for (TierLabelGlyph tlg : tier_label_glyphs) {
				  TierGlyph tier = (TierGlyph) tlg.getInfo();
				  IAnnotStyle style = tier.getAnnotStyle();

				  if (style != null) {
					  if (fg) {
						  style.setColor(chooser.getColor());
					  } else {
						  style.setBackground(chooser.getColor());
					  }
				  }
				  for (GraphGlyph gg : TierLabelManager.getContainedGraphs(tier_label_glyphs)) {
					  if (fg) {
						  gg.setColor(chooser.getColor());
						  gg.getGraphState().getTierStyle().setColor(chooser.getColor());
					  } else {
						  gg.getGraphState().getTierStyle().setBackground(chooser.getColor());
					  }
				  }
			  }
		  }
    };

    JDialog dialog = JColorChooser.createDialog((java.awt.Component) null, // parent
                                        "Pick a Color",
                                        true,  //modal
                                        chooser,
                                        al,  //OK button handler
                                        null); //no CANCEL button handler
    dialog.setVisible(true);

    refreshMap(false);
  }

  public void renameTier(final TierGlyph tier) {
    if (tier == null) {
      return;
    }
    IAnnotStyle style = tier.getAnnotStyle();

    String new_label = JOptionPane.showInputDialog("Label: ", style.getHumanName());
    if (new_label != null && new_label.length() > 0) {
      style.setHumanName(new_label);
    }
    refreshMap(false);
  }

  private void setColorByScore(List<TierLabelGlyph> tier_labels, boolean b) {
    for (TierLabelGlyph tlg : tier_labels) {
      IAnnotStyle style = tlg.getReferenceTier().getAnnotStyle();
      if (style instanceof IAnnotStyleExtended) {
        IAnnotStyleExtended astyle = (IAnnotStyleExtended) style;
        astyle.setColorByScore(b);
      }
    }

    refreshMap(false);
  }



  private static void saveAsBedFile(TierGlyph atier) {
		int childcount = atier.getChildCount();
		List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>(childcount);
		for (int i = 0; i < childcount; i++) {
			GlyphI child = atier.getChild(i);
			if (child.getInfo() instanceof SeqSymmetry) {
				syms.add((SeqSymmetry)child.getInfo());
			}
		}
		System.out.println("Saving symmetries as BED file: " + syms.size());

		JFileChooser chooser = UniFileChooser.getFileChooser("Bed file", "bed");
		chooser.setCurrentDirectory(FileTracker.DATA_DIR_TRACKER.getFile());

		int option = chooser.showSaveDialog(null);
		if (option == JFileChooser.APPROVE_OPTION) {
			FileTracker.DATA_DIR_TRACKER.setFile(chooser.getCurrentDirectory());
			MutableAnnotatedBioSeq aseq = gmodel.getSelectedSeq();
			DataOutputStream dos = null;
			try {
				File fil = chooser.getSelectedFile();
				dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fil)));
				BedParser.writeBedFormat(dos, syms, aseq);
			} catch (Exception ex) {
				ErrorHandler.errorPanel("Problem saving file", ex);
			} finally {
				GeneralUtils.safeClose(dos);
			}
		}
	}

  private static void collectSyms(GlyphI gl, List<SeqSymmetry> syms) {
		Object info = gl.getInfo();
		if ((info != null) && (info instanceof SeqSymmetry)) {
			syms.add((SeqSymmetry) info);
		} else if (gl.getChildCount() > 0) {
			// if no SeqSymmetry associated with glyph, descend and try children
			int child_count = gl.getChildCount();
			for (int i = 0; i < child_count; i++) {
				collectSyms(gl.getChild(i), syms);
			}
		}
	}

  private void addSymCoverageTier(TierGlyph atier) {
    MutableAnnotatedBioSeq aseq = gmodel.getSelectedSeq();
    int child_count = atier.getChildCount();

    List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>(child_count);
    collectSyms(atier, syms);
    if (child_count == 0 || syms.size() == 0) {
      ErrorHandler.errorPanel("Empty Tier",
        "The selected tier is empty.  Can not make a coverage tier for an empty tier.");
      return;
    }

    SeqSymmetry union_sym = SeqSymSummarizer.getUnion(syms, aseq);
    SymWithProps wrapperSym;
    if (union_sym instanceof SymWithProps) {
      wrapperSym = (SymWithProps) union_sym;
    } else {
      wrapperSym = new SimpleSymWithProps();
      ((SimpleSymWithProps) wrapperSym).addChild(union_sym);
      for (int i=0; i<union_sym.getSpanCount(); i++) {
        ((SimpleSymWithProps) wrapperSym).addSpan(union_sym.getSpan(i));
      }
    }

    String human_name = "coverage: " + atier.getLabel();

    // Generate a non-persistent style.
    // Factory will be CoverageSummarizerFactory because name starts with "coverage:"

    String unique_name = AnnotStyle.getUniqueName(human_name);
    wrapperSym.setProperty("method", unique_name);
    AnnotStyle style = AnnotStyle.getInstance(unique_name, false);
    style.setHumanName(human_name);
    style.setGlyphDepth(1);
    style.setSeparate(false); // there are not separate (+) and (-) strands
    style.setExpandable(false); // cannot expand and collapse
    style.setCustomizable(false); // the user can change the color, but not much else is meaningful

    aseq.addAnnotation(wrapperSym);
    gviewer.setAnnotatedSeq(aseq, true, true);
  }


  private void addSymSummaryTier(TierGlyph atier) {
    // not sure best way to collect syms from tier, but for now,
    //   just recursively descend through child glyphs of the tier, and if
    //   childA.getInfo() is a SeqSymmetry, add to symmetry list and prune recursion
    //   (don't descend into childA's children)
    List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>();
    collectSyms(atier, syms);
    if (syms.size() == 0) {
      ErrorHandler.errorPanel("Nothing to Summarize",
        "The selected tier is empty. It contains nothing to summarize");
      return;
    }

    MutableAnnotatedBioSeq aseq = gmodel.getSelectedSeq();
    String graphid = "summary: " + atier.getLabel();
    GraphSym gsym = SeqSymSummarizer.getSymmetrySummary(syms, aseq, false, graphid);
    gsym.setGraphName("depth: " + atier.getLabel());
    aseq.addAnnotation(gsym);
    gviewer.setAnnotatedSeq(aseq, true, true);
    GraphGlyph gl = (GraphGlyph)((SeqMapView)gviewer).getSeqMap().getItem(gsym);
    gl.setGraphStyle(GraphStateI.STAIRSTEP_GRAPH);
    gl.setColor(atier.getForegroundColor());
  }

  private void refreshMap(boolean stretch_vertically) {
    if (gviewer != null) {
      // if an AnnotatedSeqViewer is being used, ask it to update itself.
      // later this can be made more specific to just update the tiers that changed
      boolean preserve_view = ! stretch_vertically;
      gviewer.setAnnotatedSeq(gviewer.getAnnotatedSeq(), true, preserve_view);
    } else {
      // if no AnnotatedSeqViewer (as in simple test programs), update the tiermap itself.
      handler.repackTheTiers(false, stretch_vertically);
    }
  }

  public void popupNotify(javax.swing.JPopupMenu popup, TierLabelManager handler) {
    List<TierLabelGlyph> labels = handler.getSelectedTierLabels();
    int num_selections = labels.size();
    boolean not_empty = ! handler.getAllTierLabels().isEmpty();

    boolean any_are_collapsed = false;
    boolean any_are_expanded = false;
    boolean any_are_color_on = false; // whether any allow setColorByScore()
    boolean any_are_color_off = false; // whether any allow setColorByScore()
    boolean any_are_separate_tiers = false;
    boolean any_are_single_tier = false;

	for (TierLabelGlyph label : labels) {
      TierGlyph glyph = label.getReferenceTier();
      IAnnotStyle style = glyph.getAnnotStyle();
      if (style instanceof IAnnotStyleExtended) {
        IAnnotStyleExtended astyle = (IAnnotStyleExtended) style;
        any_are_color_on = any_are_color_on || astyle.getColorByScore();
        any_are_color_off = any_are_color_off || (! astyle.getColorByScore());
        any_are_separate_tiers = any_are_separate_tiers || astyle.getSeparate();
        any_are_single_tier = any_are_single_tier || (! astyle.getSeparate());
      }
      if (style.getExpandable()) {
        any_are_collapsed = any_are_collapsed || style.getCollapsed();
        any_are_expanded = any_are_expanded || ! style.getCollapsed();
      }
    }

    select_all_tiers_action.setEnabled(true);
    customize_action.setEnabled(true);

    hide_action.setEnabled(num_selections > 0);
    show_all_action.setEnabled(not_empty);

    change_color_action.setEnabled(num_selections > 0);
    change_bg_color_action.setEnabled(num_selections > 0);
    rename_action.setEnabled(num_selections == 1);

    color_by_score_on_action.setEnabled(any_are_color_off);
    color_by_score_off_action.setEnabled(any_are_color_on);

    collapse_action.setEnabled(any_are_expanded);
    expand_action.setEnabled(any_are_collapsed);
    change_expand_max_action.setEnabled(any_are_expanded);
    show_single_tier.setEnabled(any_are_separate_tiers);
    show_two_tiers.setEnabled(any_are_single_tier);
    collapse_all_action.setEnabled(not_empty);
    expand_all_action.setEnabled(not_empty);
    change_expand_max_all_action.setEnabled(not_empty);
    showMenu.setEnabled(showMenu.getMenuComponentCount() > 0);

    save_bed_action.setEnabled(num_selections == 1);
    JMenu save_menu = new JMenu("Save Annotations");

    if (num_selections == 1) {
      // Check whether this selection is a graph or an annotation
      TierLabelGlyph label = labels.get(0);
      TierGlyph glyph = (TierGlyph) label.getInfo();
      IAnnotStyle style = glyph.getAnnotStyle();
      boolean is_annotation_type = ! style.isGraphTier();
      sym_summarize_action.setEnabled(is_annotation_type);
      coverage_action.setEnabled(is_annotation_type);
      save_menu.setEnabled(is_annotation_type);
      save_bed_action.setEnabled(is_annotation_type);
    } else {
      sym_summarize_action.setEnabled(false);
      coverage_action.setEnabled(false);
      save_menu.setEnabled(false);
      save_bed_action.setEnabled(false);
    }

    changeMenu.removeAll();
    changeMenu.add(change_color_action);
    changeMenu.add(change_bg_color_action);
    changeMenu.add(rename_action);
    changeMenu.add(change_expand_max_action);
    changeMenu.add(new JSeparator());
    changeMenu.add(show_two_tiers);
    changeMenu.add(show_single_tier);
    changeMenu.add(new JSeparator());
    changeMenu.add(color_by_score_on_action);
    changeMenu.add(color_by_score_off_action);

    popup.add(customize_action);
    popup.add(new JSeparator());
    popup.add(hide_action);
    popup.add(showMenu);
    popup.add(show_all_action);

    if (gviewer instanceof SeqMapView) {
      strandsMenu.removeAll();
      strandsMenu.add(at1);
      strandsMenu.add(at2);
      strandsMenu.add(at3);
      popup.add(strandsMenu);
    }
    popup.add(new JSeparator());
    popup.add(select_all_tiers_action);
    popup.add(changeMenu);
    popup.add(new JSeparator());
    popup.add(collapse_action);
    popup.add(expand_action);
    popup.add(change_expand_max_action);
    popup.add(new JSeparator());

    popup.add(save_menu);
    save_menu.add(save_bed_action);

    popup.add(delete_action);

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

  private void removeTiers(List<TierLabelGlyph> tiers) {
	  for (TierLabelGlyph tlg: tiers) {
		  System.out.println("DEBUG: eventually would delete this tier: " + tlg);
	  }
  }
  // purely for debugging
	private void doDebugAction() {
		List<TierGlyph> current_tiers = handler.getSelectedTiers();
		for (TierGlyph tg : current_tiers) {
			IAnnotStyle style = tg.getAnnotStyle();
			System.out.println("Tier: " + tg);
			System.out.println("Style: " + style);
		}
	}
}
