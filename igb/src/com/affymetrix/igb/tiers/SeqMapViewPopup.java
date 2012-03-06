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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

import com.affymetrix.common.ExtensionPointHandler;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.BedParser;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symloader.Wiggle;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.UniFileChooser;

import com.affymetrix.genoviz.bioviews.GlyphI;

import com.affymetrix.igb.IGB;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.action.*;
import com.affymetrix.igb.glyph.MismatchPileupGlyphProcessor;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.shared.FileTracker;
import com.affymetrix.igb.shared.GraphGlyph;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.TrackstylePropertyMonitor;
import com.affymetrix.igb.tiers.AffyTieredMap.ActionToggler;
import com.affymetrix.igb.util.TrackUtils;
import com.affymetrix.igb.view.DependentData;
import com.affymetrix.igb.view.DependentData.DependentType;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.TrackView;
import com.affymetrix.igb.view.load.GeneralLoadView;
import com.affymetrix.igb.viewmode.MapViewModeHolder;
import com.affymetrix.igb.viewmode.TransformHolder;


public final class SeqMapViewPopup implements TierLabelManager.PopupListener {

	private static final boolean DEBUG = false;
	private ResourceBundle BUNDLE = IGBConstants.BUNDLE;
	private static final GenometryModel gmodel = GenometryModel.getGenometryModel();
	private final SeqMapView gviewer;
	private final TierLabelManager handler;
	private final JMenu showMenu = new JMenu(BUNDLE.getString("showMenu"));
	private final JMenu changeMenu = new JMenu(BUNDLE.getString("changeMenu"));
	private final JMenu strandsMenu = new JMenu(BUNDLE.getString("strandsMenu"));
	private final JMenu viewModeMenu = new JMenu(BUNDLE.getString("viewModeMenu"));
	private final JMenu transformMenu = new JMenu(BUNDLE.getString("transformMenu"));
	private final JMenu summaryMenu = new JMenu(BUNDLE.getString("summaryMenu"));
	private final ActionToggler at1;
	private final ActionToggler at2;
//  private final ActionToggler at3;

	private final Action save_track_action = ExportFileAction.getAction();
	private final Action rename_action = new GenericAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			List<TierGlyph> current_tiers = handler.getSelectedTiers();
			if (current_tiers.size() != 1) {
				ErrorHandler.errorPanel(BUNDLE.getString("multTrackError"));
			}
			TierGlyph current_tier = current_tiers.get(0);
			renameTier(current_tier);
			TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
		}

		@Override
		public String getText() {
			return BUNDLE.getString("renameAction");
		}
	};
	private final Action customize_action = new GenericAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			showCustomizer();
		}

		@Override
		public String getText() {
			return BUNDLE.getString("customizeAction");
		}

		@Override
		public boolean isPopup() {
			return true;
		}
	};
	private final Action expand_action = new GenericAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			setTiersCollapsed(handler.getSelectedTierLabels(), false);
			TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
		}

		@Override
		public String getText() {
			return BUNDLE.getString("expandAction");
		}
	};
	private final Action expand_all_action = new GenericAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			setTiersCollapsed(handler.getAllTierLabels(), false);
			TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
		}

		@Override
		public String getText() {
			return BUNDLE.getString("expandAllAction");
		}
	};
	private final Action collapse_action = new GenericAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			setTiersCollapsed(handler.getSelectedTierLabels(), true);
			TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
		}

		@Override
		public String getText() {
			return BUNDLE.getString("collapseAction");
		}
	};
	private final Action collapse_all_action = new GenericAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			setTiersCollapsed(handler.getAllTierLabels(), true);
			TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
		}

		@Override
		public String getText() {
			return BUNDLE.getString("collapseAllAction");
		}
	};
	private final Action hide_action = new GenericAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			hideTiers(handler.getSelectedTierLabels());
			TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
		}

		@Override
		public String getText() {
			return BUNDLE.getString("hideAction");
		}
	};
	private final Action show_all_action = new GenericAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			showAllTiers();
		}

		@Override
		public String getText() {
			return BUNDLE.getString("showAllAction");
		}
	};
	private final Action change_color_action = new GenericAction() {

		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			changeColor(handler.getSelectedTierLabels(), true);
			TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
		}

		@Override
		public String getText() {
			return BUNDLE.getString("changeColorAction");
		}
	};
	private final Action change_bg_color_action = new GenericAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			changeColor(handler.getSelectedTierLabels(), false);
			TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
		}

		@Override
		public String getText() {
			return BUNDLE.getString("changeBGColorAction");
		}
	};
	private final Action color_by_score_on_action = new GenericAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			setColorByScore(handler.getSelectedTierLabels(), true);
			TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
		}

		@Override
		public String getText() {
			return BUNDLE.getString("colorByScoreONAction");
		}
	};
	private final Action color_by_score_off_action = new GenericAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			setColorByScore(handler.getSelectedTierLabels(), false);
			TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
		}

		@Override
		public String getText() {
			return BUNDLE.getString("colorByScoreOFFAction");
		}
	};
	private final Action show_two_tiers = new GenericAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			setTwoTiers(handler.getSelectedTierLabels(), true);
			TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
		}

		@Override
		public String getText() {
			return BUNDLE.getString("showTwoTiersAction");
		}
	};
	private final Action show_single_tier = new GenericAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			setTwoTiers(handler.getSelectedTierLabels(), false);
			TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
		}

		@Override
		public String getText() {
			return BUNDLE.getString("showSingleTierAction");
		}
	};
	private final Action sym_summarize_single_action = new GenericAction() {
		private static final long serialVersionUID = 1L;
	
		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			List<TierGlyph> current_tiers = handler.getSelectedTiers();
			if (current_tiers.size() > 1) {
				ErrorHandler.errorPanel(BUNDLE.getString("multTrackError"));
			}
			TierGlyph current_tier = current_tiers.get(0);
			addSymSummaryTier(current_tier, false);
		}

		@Override
		public String getText() {
			return "";
		}
	};
	private final Action sym_summarize_both_action = new GenericAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			List<TierGlyph> current_tiers = handler.getSelectedTiers();
			if (current_tiers.size() > 1) {
				ErrorHandler.errorPanel(BUNDLE.getString("multTrackError"));
			}
			TierGlyph current_tier = current_tiers.get(0);
			addSymSummaryTier(current_tier, true);
		}

		@Override
		public String getText() {
			return "";
		}
	};
	private final Action mismatch_action = new GenericAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			List<TierGlyph> current_tiers = handler.getSelectedTiers();
			if (current_tiers.size() > 1) {
				ErrorHandler.errorPanel(BUNDLE.getString("multTrackError"));
			}
			TierGlyph current_tier = current_tiers.get(0);
			addMisMatchTier(current_tier, "mismatch");
		}

		@Override
		public String getText() {
			return BUNDLE.getString("mismatchAction");
		}
	};
	private final Action mismatch_pileup_action = new GenericAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			List<TierGlyph> current_tiers = handler.getSelectedTiers();
			if (current_tiers.size() > 1) {
				ErrorHandler.errorPanel(BUNDLE.getString("multTrackError"));
			}
			TierGlyph current_tier = current_tiers.get(0);
			addMisMatchTier(current_tier, MismatchPileupGlyphProcessor.PILEUP_IDENTIFIER);
		}

		@Override
		public String getText() {
			return BUNDLE.getString("mismatchPileupAction");
		}
	};
	private final Action maximize_track_action = new GenericAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			TierGlyph current_tier = handler.getSelectedTiers().get(0);
			gviewer.focusTrack(current_tier);
		}

		@Override
		public String getText() {
			return BUNDLE.getString("maximizeTrackAction");
		}
	};
/* not ready yet. Add // at the beginnin of the line to enable for now.
	private final Action repack_tracks_action = new GenericAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			gviewer.getSeqMap().repackTheTiers(true, true, true);
		}

		@Override
		public String getText() {
			return BUNDLE.getString("repackTracksAction");
		}
	};
/* */
	private final Action change_expand_max_action = new GenericAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			changeExpandMax(handler.getSelectedTierLabels());
			TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
		}

		@Override
		public String getText() {
			return BUNDLE.getString("changeExpandMaxAction");
		}
	};
	private final Action change_expand_max_all_action = new GenericAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			changeExpandMax(handler.getAllTierLabels());
			TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
		}

		@Override
		public String getText() {
			return BUNDLE.getString("changeExpandMaxAllAction");
		}
	};
	private final Action delete_action = new GenericAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			BioSeq seq = gmodel.getSelectedSeq();

			if (IGB.confirmPanel(MessageFormat.format(BUNDLE.getString("confirmDelete"), seq.getID()), PreferenceUtils.getTopNode(),
									PreferenceUtils.CONFIRM_BEFORE_CLEAR, PreferenceUtils.default_confirm_before_clear)) {
				removeTiers(handler.getSelectedTierLabels());
			}
		}

		@Override
		public String getText() {
			return BUNDLE.getString("deleteAction");
		}
	};
	private final Action change_font_size_action = new GenericAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			super.actionPerformed(e);
			changeFontSize(handler.getSelectedTierLabels());
			TrackstylePropertyMonitor.getPropertyTracker().actionPerformed(e);
		}

		@Override
		public String getText() {
			return BUNDLE.getString("changeFontSizeAction");
		}
	};
	private final Action use_as_reference_seq_action = new GenericAction() {
		private static final long serialVersionUID = 1L;

		public void actionPerformed(ActionEvent e) {
			try {
				super.actionPerformed(e);
				List<TierGlyph> current_tiers = handler.getSelectedTiers();
				if (current_tiers.size() > 1) {
					ErrorHandler.errorPanel(BUNDLE.getString("multTrackError"));
				}
				useTrackAsReferenceSequence(current_tiers.get(0));
			} catch (Exception ex) {
				Logger.getLogger(SeqMapViewPopup.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		@Override
		public String getText() {
			return BUNDLE.getString("useAsReferenceSeqAction");
		}
	};
	
	public SeqMapViewPopup(TierLabelManager handler, SeqMapView smv) {
		this.handler = handler;
		this.gviewer = smv;
		at1 = new ActionToggler(smv.getClass().getSimpleName() + "_SeqMapViewPopup.showPlus", ShowPlusStrandAction.getAction());
		at2 = new ActionToggler(smv.getClass().getSimpleName() + "_SeqMapViewPopup.showMinus", ShowMinusStrandAction.getAction());
//		at3 = new ActionToggler(smv.getSeqMap().show_mixed_action);
	}

	private void showCustomizer() {
		PreferencesPanel pv = PreferencesPanel.getSingleton();
		pv.setTab(PreferencesPanel.TAB_TIER_PREFS_VIEW);
		pv.tpvGUI.tpv.setTier_label_glyphs(handler.getSelectedTierLabels());
		JFrame f = pv.getFrame();
		f.setVisible(true);
	}

	List<ITrackStyleExtended> getStyles(List<TierLabelGlyph> tier_label_glyphs) {
		if (tier_label_glyphs.isEmpty()) {
			return Collections.<ITrackStyleExtended>emptyList();
		}

		// styles is a list of styles with no duplicates, so a Set rather than a List
		// might make sense.  But at the moment it seems faster to use a List
		List<ITrackStyleExtended> styles = new ArrayList<ITrackStyleExtended>(tier_label_glyphs.size());

		for (TierLabelGlyph tlg : tier_label_glyphs) {
			TierGlyph tier = tlg.getReferenceTier();
			ITrackStyleExtended tps = tier.getAnnotStyle();
			if (tps != null && !styles.contains(tps)) {
				styles.add(tps);
			}
		}
		return styles;
	}

	private void setTiersCollapsed(List<TierLabelGlyph> tier_labels, boolean collapsed) {
		handler.setTiersCollapsed(tier_labels, collapsed);
		gviewer.getSeqMap().updateWidget();
	}

	private void changeFontSize(List<TierLabelGlyph> tier_labels) {
		if (tier_labels == null || tier_labels.isEmpty()) {
			ErrorHandler.errorPanel("changeExpandMaxAll called with an empty list");
			return;
		}

		Object initial_value = TrackStyle.default_track_name_size;
		if (tier_labels.size() == 1) {
			TierLabelGlyph tlg = tier_labels.get(0);
			TierGlyph tg = (TierGlyph) tlg.getInfo();
			ITrackStyleExtended style = tg.getAnnotStyle();
			if (style != null && style instanceof TrackStyle) {
				initial_value = ((TrackStyle) style).getTrackNameSize();
			}
		}

		Object input = JOptionPane.showInputDialog(null, BUNDLE.getString("selectFontSize"), BUNDLE.getString("changeSelectedTrackFontSize"), JOptionPane.PLAIN_MESSAGE, null,
				TrackConstants.SUPPORTED_SIZE, initial_value);

		if (input == null) {
			return;
		}

		changeFontSize(tier_labels, (Float) input);
	}

	private void changeFontSize(List<TierLabelGlyph> tier_label_glyphs, float size) {
		for (TierLabelGlyph tlg : tier_label_glyphs) {
			TierGlyph tier = (TierGlyph) tlg.getInfo();
			ITrackStyleExtended style = tier.getAnnotStyle();
			if (style != null && style instanceof TrackStyle) {
				((TrackStyle) style).setTrackNameSize(size);
			}
		}
		gviewer.getSeqMap().updateWidget();
	}

	public void changeExpandMax(List<TierLabelGlyph> tier_labels) {
		if (tier_labels == null || tier_labels.isEmpty()) {
			ErrorHandler.errorPanel("changeExpandMaxAll called with an empty list");
			return;
		}

		String initial_value = "0";
		if (tier_labels.size() == 1) {
			TierLabelGlyph tlg = tier_labels.get(0);
			TierGlyph tg = (TierGlyph) tlg.getInfo();
			ITrackStyleExtended style = tg.getAnnotStyle();
			if (style != null) {
				initial_value = "" + style.getMaxDepth();
			}
		}

		String input =
				(String) JOptionPane.showInputDialog(null,
						BUNDLE.getString("maxHeight"),
				BUNDLE.getString("changeMaxHeight"), JOptionPane.PLAIN_MESSAGE,
				null, null, initial_value);

		if (input == null || input.equals(JOptionPane.UNINITIALIZED_VALUE)) {
			return;
		}

		int newmax;
		try {
			newmax = Integer.parseInt(input);
		} catch (NumberFormatException ex) {
			ErrorHandler.errorPanel("Couldn't parse new track max '" + input + "'");
			return;
		}

		changeExpandMax(tier_labels, newmax);
	}

	private void changeExpandMax(List<TierLabelGlyph> tier_label_glyphs, int max) {
		for (TierLabelGlyph tlg : tier_label_glyphs) {
			TierGlyph tier = (TierGlyph) tlg.getInfo();
			ITrackStyleExtended style = tier.getAnnotStyle();
			style.setMaxDepth(max);
		}
		repack(true);
	}

	private void setTwoTiers(List<TierLabelGlyph> tier_label_glyphs, boolean b) {
		for (TierLabelGlyph tlg : tier_label_glyphs) {
			TierGlyph tier = (TierGlyph) tlg.getInfo();
			ITrackStyleExtended style = tier.getAnnotStyle();
			if (!style.isGraphTier()) {
				(style).setSeparate(b);
			}
		}
		refreshMap(false, true);
		handler.sortTiers();
	}

	public void showAllTiers() {
		List<TierLabelGlyph> tiervec = handler.getAllTierLabels();

		for (TierLabelGlyph label : tiervec) {
			TierGlyph tier = (TierGlyph) label.getInfo();
			ITrackStyleExtended style = tier.getAnnotStyle();
			if (style != null) {
				style.setShow(true);
				tier.setVisibility(true);
			}
		}
		showMenu.removeAll();
		handler.sortTiers();
		gviewer.getSeqMap().updateWidget();
		//refreshMap(false, true); // when re-showing all tier, do strech_to_fit in the y-direction
	}

	/** Hides one tier and creates a JMenuItem that can be used to show it again.
	 *  Does not re-pack the given tier, or any other tiers.
	 */
	public void hideOneTier(final TierGlyph tier) {
		final ITrackStyleExtended style = tier.getAnnotStyle();
		// if style.getShow() is already false, there is likely a bug somewhere!
		if (style == null) {
			return;
		}
		if (style.getShow()) {
			style.setShow(false);
			final JMenuItem show_tier = new JMenuItem() {

				private static final long serialVersionUID = 1L;
				// override getText() because the HumanName of the style might change

				@Override
				public String getText() {
					String name = style.getTrackName();
					if (name == null) {
						name = "<unnamed>";
					}
					if (name.length() > 30) {
						name = name.substring(0, 30) + "...";
					}
					return name;
				}
			};
			show_tier.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					style.setShow(true);
					showMenu.remove(show_tier);
					handler.sortTiers();
					repack(false);
				}
			});
			showMenu.add(show_tier);
		}
		tier.setVisibility(false);
	}

	/** Hides one tier and creates a JMenuItem that can be used to show it again.
	 *  Does not re-pack the given tier, or any other tiers.
	 */
	//Called from LodeModeTableModel to hide tracks selectively from the eye icon.
	public void hideOneTier(final ITrackStyleExtended style) {
		// if style.getShow() is already false, there is likely a bug somewhere!
		if (style == null) {
			return;
		}
		if (style.getShow()) {
			style.setShow(false);
			final JMenuItem show_tier = new JMenuItem() {

				private static final long serialVersionUID = 1L;
				// override getText() because the HumanName of the style might change

				@Override
				public String getText() {
					String name = style.getTrackName();
					if (name == null) {
						name = "<unnamed>";
					}
					if (name.length() > 30) {
						name = name.substring(0, 30) + "...";
					}
					return name;
				}
			};
			show_tier.setName(style.getMethodName());
			show_tier.setAction(new AbstractAction() {
				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
					style.setShow(true);
					showMenu.remove(show_tier);
					handler.sortTiers();
					repack(false);
				}
			});
			showMenu.add(show_tier);
		}
		style.setShow(false);
	}

	/** Hides multiple tiers and then repacks.
	 *  @param tiers  a List of GlyphI objects for each of which getInfo() returns a TierGlyph.
	 */
	void hideTiers(List<TierLabelGlyph> tiers) {
		for (TierLabelGlyph g : tiers) {
			if (g.getInfo() instanceof TierGlyph) {
				TierGlyph tier = (TierGlyph) g.getInfo();
				hideOneTier(tier);
			}
		}

		repack(false);

		/** Possible bug : When all strands are hidden.
		 * tier label and tier do appear at same position.
		 **/
		// NOTE: Below call to stretchToFit is not redundancy. It is there
		//       to solve above mentioned bug.
		repack(false);
	}

	private void changeColor(final List<TierLabelGlyph> tier_label_glyphs, final boolean fg) {
		if (tier_label_glyphs.isEmpty()) {
			return;
		}

		final JColorChooser chooser = new JColorChooser();

		TierLabelGlyph tlg_0 = tier_label_glyphs.get(0);
		TierGlyph tier_0 = (TierGlyph) tlg_0.getInfo();
		ITrackStyleExtended style_0 = tier_0.getAnnotStyle();
		if (style_0 != null) {
			if (fg) {
				chooser.setColor(style_0.getForeground());
			} else {
				chooser.setColor(style_0.getBackground());
			}
		}

		ActionListener al = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				for (TierLabelGlyph tlg : tier_label_glyphs) {
					TierGlyph tier = (TierGlyph) tlg.getInfo();
					ITrackStyleExtended style = tier.getAnnotStyle();

					if (style != null) {
						if (fg) {
							style.setForeground(chooser.getColor());
						} else {
							style.setBackground(chooser.getColor());
						}
					}
				  for (GraphGlyph gg : TierLabelManager.getContainedGraphs(tier_label_glyphs)) {
					  if (fg) {
						  gg.setColor(chooser.getColor());
						  gg.getGraphState().getTierStyle().setForeground(chooser.getColor());
					  } else {
						  gg.getGraphState().getTierStyle().setBackground(chooser.getColor());
					  }
				  }
				}
			}
		};

		JDialog dialog = JColorChooser.createDialog((java.awt.Component) null, // parent
				"Pick a Color",
				true, //modal
				chooser,
				al, //OK button handler
				null); //no CANCEL button handler
		dialog.setVisible(true);

		refreshMap(false, false);
	}

	public void renameTier(final TierGlyph tier) {
		if (tier == null) {
			return;
		}
		ITrackStyleExtended style = tier.getAnnotStyle();

		String new_label = JOptionPane.showInputDialog(BUNDLE.getString("label") + ": ", style.getTrackName());
		if (new_label != null && new_label.length() > 0) {
			style.setTrackName(new_label);
			tier.setLabel(new_label);
			gviewer.getSeqMap().setTierLabels();
		}
		gviewer.getSeqMap().updateWidget();
	}

	private void setColorByScore(List<TierLabelGlyph> tier_labels, boolean b) {
		for (TierLabelGlyph tlg : tier_labels) {
			ITrackStyleExtended style = tlg.getReferenceTier().getAnnotStyle();
			style.setColorByScore(b);
		}

		refreshMap(false, false);
	}

	private static void saveAsBedFile(TierGlyph atier) {
		int childcount = atier.getChildCount();
		List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>(childcount);
		for (int i = 0; i < childcount; i++) {
			GlyphI child = atier.getChild(i);
			if (child.getInfo() instanceof SeqSymmetry) {
				syms.add((SeqSymmetry) child.getInfo());
			}
		}
		System.out.println("Saving symmetries as BED file: " + syms.size());

		JFileChooser chooser = UniFileChooser.getFileChooser("Bed file", "bed");
		chooser.setCurrentDirectory(FileTracker.DATA_DIR_TRACKER.getFile());

		int option = chooser.showSaveDialog(null);
		if (option == JFileChooser.APPROVE_OPTION) {
			FileTracker.DATA_DIR_TRACKER.setFile(chooser.getCurrentDirectory());
			BioSeq aseq = gmodel.getSelectedSeq();
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

	private static void saveAsWigFile(TierGlyph atier) {
		int childcount = atier.getChildCount();
		List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>(childcount);
		for (int i = 0; i < childcount; i++) {
			GlyphI child = atier.getChild(i);
			if (child.getInfo() instanceof SeqSymmetry) {
				syms.add((SeqSymmetry) child.getInfo());
			}
		}
		System.out.println("Saving symmetries as WIG file: " + syms.size());

		JFileChooser chooser = UniFileChooser.getFileChooser("Wig file", "wig");
		chooser.setCurrentDirectory(FileTracker.DATA_DIR_TRACKER.getFile());

		int option = chooser.showSaveDialog(null);
		if (option == JFileChooser.APPROVE_OPTION) {
			FileTracker.DATA_DIR_TRACKER.setFile(chooser.getCurrentDirectory());
			BioSeq aseq = gmodel.getSelectedSeq();
			DataOutputStream dos = null;
			try {
				File fil = chooser.getSelectedFile();
				dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fil)));
				Wiggle.writeAnnotations(syms,aseq,dos);
			} catch (Exception ex) {
				ErrorHandler.errorPanel("Problem saving file", ex);
			} finally {
				GeneralUtils.safeClose(dos);
			}
		}
	}

	public static void addMisMatchTier(final TierGlyph atier, final String prefix) {
		final BioSeq aseq = gmodel.getSelectedSeq();
		
		SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {

			@Override
			protected Boolean doInBackground() throws Exception {
				boolean pileup = MismatchPileupGlyphProcessor.PILEUP_IDENTIFIER.equals(prefix);		
				String human_name = prefix + ": " + atier.getLabel();
				String unique_name = TrackStyle.getUniqueName(human_name);
				String method = atier.getAnnotStyle().getMethodName();
				SeqSymmetry tsym = aseq.getAnnotation(method);
				if (tsym == null || tsym.getChildCount() == 0) {
					ErrorHandler.errorPanel("Empty Track",
							"The selected track is empty.  Can not make a coverage track for an empty track.");
					return false;
				}

				int[] startEnd = DependentData.getStartEnd(tsym, aseq);
				SeqSpan loadSpan = new SimpleSeqSpan(startEnd[0], startEnd[1], aseq);

				LoadResidueAction loadResidue = new LoadResidueAction(loadSpan, true);
				loadResidue.actionPerformed(null);

				if (!aseq.isAvailable(loadSpan)) {
					ErrorHandler.errorPanel("Sequence Not Loaded",
							"Unable to load sequence. Cannot create mismatch graph.");
					return false;
				}

				DependentData dd = new DependentData(unique_name, pileup ? DependentType.MISMATCH_PILEUP : DependentType.MISMATCH, method);
				SymWithProps wrapperSym = TrackView.getInstance().addToDependentList(dd);

				if (wrapperSym == null) {
					ErrorHandler.errorPanel("Empty Track",
							"The selected track is empty.  Can not make a coverage track for an empty track.");
					return false;
				}

				// Generate a non-persistent style.
				// Factory will be CoverageSummarizerFactory because name starts with "coverage:"
				TrackStyle style = TrackStyle.getInstance(unique_name, false);
				style.setTrackName(human_name);
				style.setGlyphDepth(1);
				style.setSeparate(false); // there are not separate (+) and (-) strands
				style.setExpandable(false); // cannot expand and collapse
				style.setCustomizable(false); // the user can change the color, but not much else is meaningful
				style.setFeature(atier.getAnnotStyle().getFeature());

				return true;
			}
			
			@Override
			public void done(){
				try {
					if(get()){
						IGB.getSingleton().getMapView().setAnnotatedSeq(aseq, true, true);
					}
				} catch (Exception ex) {
					Logger.getLogger(SeqMapViewPopup.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		};
		worker.execute();
	}

	private void addSymSummaryTier(TierGlyph atier, boolean bothDirection) {
		// not sure best way to collect syms from tier, but for now,
		//   just recursively descend through child glyphs of the tier, and if
		//   childA.getInfo() is a SeqSymmetry, add to symmetry list and prune recursion
		//   (don't descend into childA's children)


//    List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>();
//    collectSyms(atier, syms);

//	  TODO: If tierglyph is empty then it is never displayed. So check when below mentioned condition is met.
		//if (syms.size() == 0) {
		//ErrorHandler.errorPanel("Nothing to Summarize",
		//    "The selected track is empty. It contains nothing to summarize");
		//return;
		//}

		BioSeq aseq = gmodel.getSelectedSeq();
		String human_name = BUNDLE.getString("depth") + ": " + atier.getLabel();
		String id = TrackStyle.getUniqueName(human_name);
		DependentData dd = null;
		String method = atier.getAnnotStyle().getMethodName();
		if (bothDirection) {
			human_name += getSymbol(Direction.BOTH);
			dd = new DependentData(id, DependentType.SUMMARY, method, Direction.BOTH);
		} else {
			human_name += getSymbol(atier.getDirection());
			dd = new DependentData(id, DependentType.SUMMARY, method, atier.getDirection());
		}

		GraphSym gsym = (GraphSym) TrackView.getInstance().addToDependentList(dd);

		if (gsym == null) {
			ErrorHandler.errorPanel("Nothing to Summarize",
					"The selected track is empty. It contains nothing to summarize");
			return;
		}

		gsym.setGraphName(human_name);
		gsym.getGraphState().setGraphStyle(GraphType.STAIRSTEP_GRAPH);
		gsym.getGraphState().getTierStyle().setForeground(atier.getForegroundColor());
		gsym.getGraphState().getTierStyle().setBackground(atier.getBackgroundColor());
		gsym.getGraphState().getTierStyle().setFeature(atier.getAnnotStyle().getFeature());
		gviewer.setAnnotatedSeq(aseq, true, true);
//    GraphGlyph gl = (GraphGlyph)gviewer.getSeqMap().getItem(gsym);
//    gl.setGraphStyle(GraphType.STAIRSTEP_GRAPH);
//    gl.setColor(atier.getForegroundColor());
	}

	public void refreshMap(boolean stretch_vertically, boolean stretch_horizonatally) {
		if (gviewer != null) {
			// if an AnnotatedSeqViewer is being used, ask it to update itself.
			// later this can be made more specific to just update the tiers that changed
			boolean preserve_view_x = !stretch_vertically;
			boolean preserve_view_y = !stretch_horizonatally;
			gviewer.setAnnotatedSeq(gviewer.getAnnotatedSeq(), true, preserve_view_x, preserve_view_y);
		} else {
			// if no AnnotatedSeqViewer (as in simple test programs), update the tiermap itself.
			handler.repackTheTiers(false, stretch_vertically);
		}
	}

	public void repack(boolean full_repack){
		gviewer.getSeqMap().clearSelected();
		handler.repackTheTiers(full_repack, true);
	}

	private JMenu addOperationMenu(List<SeqSymmetry> syms) {
		JMenu operationsMenu = null;
		for (Operator operator : ExtensionPointHandler
				.getExtensionPoint(Operator.class)
				.getExtensionPointImpls()) {
			if (TrackUtils.getInstance().checkCompatible(syms, operator)) {
				String name = operator.getName();
				String title = name.substring(0, 1).toUpperCase()
						+ name.substring(1);
				JMenuItem operatorMI = new JMenuItem(title);
				operatorMI.addActionListener(new TierOperationAction(
						gviewer, operator));
				if (operationsMenu == null) {
					operationsMenu = new JMenu("Track Operations");
				}
				operationsMenu.add(operatorMI);
			}
		}
		return operationsMenu;
	}

	public void popupNotify(javax.swing.JPopupMenu popup, TierLabelManager handler) {
		final List<TierLabelGlyph> labels = handler.getSelectedTierLabels();
		int num_selections = labels.size();
		boolean not_empty = !handler.getAllTierLabels().isEmpty();

		boolean any_are_collapsed = false;
		boolean any_are_expanded = false;
		boolean any_are_color_on = false; // whether any allow setColorByScore()
		boolean any_are_color_off = false; // whether any allow setColorByScore()
		boolean any_are_separate_tiers = false;
		boolean any_are_single_tier = false;
		boolean add_maximize = false;
		boolean any_view_mode = false;

		for (TierLabelGlyph label : labels) {
			TierGlyph glyph = label.getReferenceTier();
			ITrackStyleExtended astyle = glyph.getAnnotStyle();
			any_are_color_on = any_are_color_on || astyle.getColorByScore();
			any_are_color_off = any_are_color_off || (!astyle.getColorByScore());
			if (!astyle.isGraphTier()) {
				any_are_separate_tiers = any_are_separate_tiers || astyle.getSeparate();
				any_are_single_tier = any_are_single_tier || (!astyle.getSeparate());
			}
			any_view_mode = any_view_mode || (!astyle.isGraphTier());

			if (astyle.getExpandable()) {
				any_are_collapsed = any_are_collapsed || astyle.getCollapsed();
				any_are_expanded = any_are_expanded || !astyle.getCollapsed();
			}
		}


		customize_action.setEnabled(true);

		hide_action.setEnabled(num_selections > 0);
		delete_action.setEnabled(num_selections > 0);
		show_all_action.setEnabled(not_empty);

		change_color_action.setEnabled(num_selections > 0);
		change_bg_color_action.setEnabled(num_selections > 0);
		change_font_size_action.setEnabled(num_selections > 0);
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
		viewModeMenu.setEnabled(false);
		transformMenu.setEnabled(false);
		
		
		viewModeMenu.removeAll();
		transformMenu.removeAll();

		if (num_selections == 1) {
			// Check whether this selection is a graph or an annotation
			final TierLabelGlyph label = labels.get(0);
			final TierGlyph glyph = (TierGlyph) label.getInfo();
			
			final ITrackStyleExtended style = glyph.getAnnotStyle();
			boolean is_annotation_type = !style.isGraphTier();
			summaryMenu.setEnabled(is_annotation_type);
			sym_summarize_single_action.putValue(Action.NAME, glyph.getLabel() + getSymbol(glyph.getDirection()));
			sym_summarize_both_action.putValue(Action.NAME, glyph.getLabel() + getSymbol(Direction.BOTH));
			//sym_summarize_single_action.setEnabled(is_annotation_type);
			//coverage_action.setEnabled(is_annotation_type);
			//save_menu.setEnabled(is_annotation_type);
			if (glyph.getDirection() != Direction.AXIS) {
				add_maximize = true;
			}
	
			
			if (glyph.getInfo() != null && glyph.getInfo() instanceof RootSeqSymmetry) {
				RootSeqSymmetry rootSym = (RootSeqSymmetry) glyph.getInfo();
				
				Map<String, Action> actions = new HashMap<String, Action>();
				for (final Object mode : MapViewModeHolder.getInstance().getAllViewModesFor(rootSym.getCategory())) {
					Action action = new GenericAction() {
						private static final long serialVersionUID = 1L;

						@Override
						public void actionPerformed(ActionEvent ae) {
							(style).setViewMode(mode.toString());
							gviewer.addAnnotationTrackFor(style);
							//refreshMap(false, false);
						}

						@Override
						public String getText() {
							return mode.toString();
						}
					};
					actions.put(mode.toString(), action);
					viewModeMenu.add(new JCheckBoxMenuItem(action));
				}

				if (actions.size() > 0) {
					Action action = actions.get(style.getViewMode());
					if (action != null) {
						action.putValue(Action.SELECTED_KEY, true);
					}
					viewModeMenu.setEnabled(true);
				
				}
				
				if (style instanceof TrackStyle) {
					Map<String, Action> transform_actions = new HashMap<String, Action>();
					for (final Object transform : TransformHolder.getInstance().getAllTransformFor(((TrackStyle)style).getFileTypeCategory())) {
						Action action = new GenericAction() {
							private static final long serialVersionUID = 1L;

							@Override
							public void actionPerformed(ActionEvent ae) {
								style.setOperator(transform.toString());
								//For now now do not preserve selection
								gviewer.getSeqMap().clearSelected();
								//gviewer.addAnnotationTrackFor(style);
								refreshMap(false, false);
							}

							@Override
							public String getText() {
								return transform.toString();
							}
						};
						transform_actions.put(transform.toString(), action);
						transformMenu.add(new JCheckBoxMenuItem(action));
					}

					if (transform_actions.size() > 0) {
						Action action = transform_actions.get(style.getOperator());
						if (action != null) {
							action.putValue(Action.SELECTED_KEY, true);
						}
						transformMenu.setEnabled(true);
					}
				}

			}
		} else {
			summaryMenu.setEnabled(false);
			//sym_summarize_single_action.setEnabled(false);
			//coverage_action.setEnabled(false);
		}

		changeMenu.removeAll();
		changeMenu.add(change_color_action);
		changeMenu.add(change_bg_color_action);
		changeMenu.add(rename_action);
		changeMenu.add(change_font_size_action);
		changeMenu.add(change_expand_max_action);
		changeMenu.add(new JSeparator());
		changeMenu.add(show_two_tiers);
		changeMenu.add(show_single_tier);
		changeMenu.add(new JSeparator());
		changeMenu.add(color_by_score_on_action);
		changeMenu.add(color_by_score_off_action);
		popup.add(save_track_action);
		if (num_selections == 1) {
			// Check whether this selection is a graph or an annotation
			TierLabelGlyph label = labels.get(0);
			final TierGlyph glyph = (TierGlyph) label.getInfo();
			ITrackStyleExtended style = glyph.getAnnotStyle();
			GenericFeature feature = style.getFeature();
			if (feature != null) {
				String file_type = style.getFileType();
				if ("bam".equalsIgnoreCase(file_type) || "sam".equalsIgnoreCase(file_type)) {
					popup.add(mismatch_action);
					popup.add(mismatch_pileup_action);
				}

				if(FileTypeHolder.getInstance().isSequence(file_type)){
					popup.add(new JSeparator());
					popup.add(use_as_reference_seq_action);
				}
				
				if (feature.friendlyURL != null) {
					popup.add(new FeatureInfoAction(feature.friendlyURL.toString()));
				}
			}
		}
		popup.add(new JSeparator());
		popup.add(customize_action);
		popup.add(changeMenu);
		popup.add(viewModeMenu);
		popup.add(new JSeparator());
		popup.add(hide_action);
		popup.add(showMenu);
		popup.add(show_all_action);
		strandsMenu.removeAll();
		strandsMenu.add(at1);
		strandsMenu.add(at2);
		popup.add(strandsMenu);
		popup.add(new JSeparator());
		popup.add(CenterAtHairlineAction.getAction());
		if (add_maximize) {
			popup.add(maximize_track_action);
		}
		popup.add(collapse_action);
		popup.add(expand_action);
		popup.add(new JSeparator());
		popup.add(transformMenu);
		JMenu operationsMenu = addOperationMenu(TrackUtils.getInstance().getSymsFromLabelGlyphs(labels));
		if (operationsMenu != null) {
			popup.add(operationsMenu);
		}
		summaryMenu.removeAll();
		summaryMenu.add(sym_summarize_single_action);
		if (!show_two_tiers.isEnabled()) // If showing both track then give a option to create
		{
			summaryMenu.add(sym_summarize_both_action); // depth graph in both direction.
		}
		popup.add(summaryMenu);
		popup.add(new JSeparator());
//		popup.add(repack_tracks_action);
		popup.add(delete_action);
				
//	strandsMenu.add(at3);
		
				
//		popup.add(change_expand_max_action);
		
		
		
		//popup.add(coverage_action);

		
		if(gviewer.getAutoLoad() != null){
			popup.add(new JSeparator());
			popup.add(new JMenuItem(gviewer.getAutoLoad()));
		}

		if (DEBUG) {
			popup.add(new AbstractAction("DEBUG") {

				private static final long serialVersionUID = 1L;

				public void actionPerformed(ActionEvent e) {
					doDebugAction();
				}
			});
		}
	}

	private void removeTiers(List<TierLabelGlyph> tiers) {
		for (TierLabelGlyph tlg : tiers) {
			ITrackStyleExtended style = tlg.getReferenceTier().getAnnotStyle();
			String method = style.getMethodName();
			if (method != null) {
				TrackView.getInstance().delete(gviewer.getSeqMap(), method, style);
			} else {
				for (GraphGlyph gg : TierLabelManager.getContainedGraphs(tiers)) {
					style = gg.getGraphState().getTierStyle();
					method = style.getMethodName();
					TrackView.getInstance().delete(gviewer.getSeqMap(), method, style);
				}
			}
		}
		gviewer.dataRemoved();	// refresh
	}

	private void useTrackAsReferenceSequence(TierGlyph tier) throws Exception{
		ITrackStyleExtended style = tier.getAnnotStyle();
		GenericFeature feature = style.getFeature();
		GeneralLoadView.getLoadView().useAsRefSequence(feature);
	}
	
	// purely for debugging
	private void doDebugAction() {
		for (TierGlyph tg : handler.getSelectedTiers()) {
			ITrackStyleExtended style = tg.getAnnotStyle();
			System.out.println("Track: " + tg);
			System.out.println("Style: " + style);
		}
	}

	static private String getSymbol(Direction direction) {
		return TierLabelGlyph.getDirectionSymbol(direction);
	}

	SeqMapView getSeqMapView() {
		return gviewer;
	}

	public JMenu getShowMenu() {
		return showMenu;
	}

	public TierLabelManager getHandler() {
		return handler;
	}
}
