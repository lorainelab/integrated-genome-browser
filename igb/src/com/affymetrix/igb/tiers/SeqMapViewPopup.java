/**
 * Copyright (c) 2005-2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.tiers;

import com.affymetrix.common.ExtensionPointHandler;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.action.*;
import com.affymetrix.igb.shared.TierGlyph.Direction;
import com.affymetrix.igb.shared.*;
import com.affymetrix.igb.tiers.AffyTieredMap.ActionToggler;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.TrackView;
import com.affymetrix.igb.viewmode.MapViewModeHolder;
import com.affymetrix.igb.viewmode.TransformHolder;
import java.awt.event.ActionEvent;
import java.util.*;
import javax.swing.*;

public final class SeqMapViewPopup implements TierLabelManager.PopupListener {

	private static final boolean DEBUG = false;
	private ResourceBundle BUNDLE = IGBConstants.BUNDLE;
	private final SeqMapView gviewer;
	private final TierLabelManager handler;
	private final JMenu showMenu = new JMenu(BUNDLE.getString("showMenu"));
	private final JMenu changeMenu = new JMenu(BUNDLE.getString("changeMenu"));
	private final JMenu strandsMenu = new JMenu(BUNDLE.getString("strandsMenu"));
	private final JMenu viewModeMenu = new JMenu(BUNDLE.getString("viewModeMenu"));
	private final JMenu transformMenu = new JMenu(BUNDLE.getString("transformMenu"));
//	private final JMenu summaryMenu = new JMenu(BUNDLE.getString("summaryMenu"));
	private final ActionToggler at1;
	private final ActionToggler at2;
//  private final ActionToggler at3;
	private final RepackTiersAction repackStub;
	private final Action save_track_action = ExportFileAction.getAction();
	private final Action save_selected_annotations_action = ExportSelectedAnnotationFileAction.getAction();
	private final Action rename_action = RenameAction.getAction();
	private final Action customize_action = CustomizeAction.getAction();
	private final Action hide_action = HideAction.getAction();
	private final Action show_all_action = ShowAllAction.getAction();
	private final Action color_by_score_action = ColorByScoreAction.getAction();

	private final Action maximize_track_action = MaximizeTrackAction.getAction();

	/*
	 *
	 */
	private final Action change_expand_max_action = ChangeExpandMaxAction.getAction();
	private final Action change_expand_max_all_action = ChangeExpandMaxAllAction.getAction();
	private final Action set_color_by_score_action = SetColorByScoreAction.getAction();
	private final Action change_font_size_action = ChangeFontSizeAction.getAction();
	private final Action use_as_reference_seq_action = UseAsReferenceSeqAction.getAction();

	public SeqMapViewPopup(TierLabelManager handler, SeqMapView smv) {
		this.handler = handler;
		this.gviewer = smv;
		this.repackStub = new RepackTiersAction(null, null) {
			private static final long serialVersionUID = 1L;
		};
		at1 = new ActionToggler(smv.getClass().getSimpleName() + "_SeqMapViewPopup.showPlus", ShowPlusStrandAction.getAction());
		at2 = new ActionToggler(smv.getClass().getSimpleName() + "_SeqMapViewPopup.showMinus", ShowMinusStrandAction.getAction());
//		at3 = new ActionToggler(smv.getSeqMap().show_mixed_action);
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

	private void setTwoTiers(List<TierLabelGlyph> tier_label_glyphs, boolean b) {
		for (TierLabelGlyph tlg : tier_label_glyphs) {
			TierGlyph tier = (TierGlyph) tlg.getInfo();
			ITrackStyleExtended style = tier.getAnnotStyle();
			if (!b || MapViewModeHolder.getInstance().styleSupportsTwoTrack(style)) {
				style.setSeparate(b);
			}
		}
		refreshMap(false, true);
		handler.sortTiers();
	}

	public boolean containHiddenTiers() {
		for (TierLabelGlyph label : handler.getAllTierLabels()) {
			TierGlyph tier = (TierGlyph) label.getInfo();
			ITrackStyleExtended style = tier.getAnnotStyle();
			if (!style.getShow()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Hides one tier and creates a JMenuItem that can be used to show it again.
	 * Does not re-pack the given tier, or any other tiers.
	 */
	//Called from LodeModeTableModel to hide tracks selectively from the eye icon.
	public void hideOneTier(final ITrackStyleExtended style) {
		// if style.getShow() is already false, there is likely a bug somewhere!
		if (style == null) {
			return;
		}
		if (style.getShow()) {
			style.setShow(false);
			rebuildShowMenu();
		}
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

	public void repack(final boolean full_repack) {
		repackStub.repack(full_repack);
	}

	private JMenu addOperationMenu(List<SeqSymmetry> syms) {
		JMenu operationsMenu = null;
		for (Operator operator : ExtensionPointHandler.getExtensionPoint(Operator.class).getExtensionPointImpls()) {
			if (TrackUtils.getInstance().checkCompatible(syms, operator)) {
				String title = operator.getDisplay();
				JMenuItem operatorMI = new JMenuItem(title);
				operatorMI.addActionListener(new TrackOperationAction(
						gviewer, operator));
				if (operationsMenu == null) {
					operationsMenu = new JMenu("Track Operations");
				}
				operationsMenu.add(operatorMI);
			}
		}
		return operationsMenu;
	}

	@Override
	public void popupNotify(javax.swing.JPopupMenu popup, final TierLabelManager handler) {
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
				any_are_single_tier = any_are_single_tier || (!astyle.getSeparate() && MapViewModeHolder.getInstance().styleSupportsTwoTrack(astyle));
			}
			any_view_mode = any_view_mode || (!astyle.isGraphTier());

			if (astyle.getExpandable()) {
				any_are_collapsed = any_are_collapsed || astyle.getCollapsed();
				any_are_expanded = any_are_expanded || !astyle.getCollapsed();
			}
		}

		customize_action.setEnabled(true);

		hide_action.setEnabled(num_selections > 0);
		RemoveDataFromTracksAction.getAction().setEnabled(num_selections > 0);
		show_all_action.setEnabled(containHiddenTiers());

		ChangeForegroundColorAction.getAction().setEnabled(num_selections > 0);
		ChangeBackgroundColorAction.getAction().setEnabled(num_selections > 0);
		change_font_size_action.setEnabled(num_selections > 0);
		rename_action.setEnabled(num_selections == 1);

		color_by_score_action.putValue(ColorByScoreAction.SELECTED_KEY,any_are_color_off);

		CollapseAction.getAction().setEnabled(any_are_expanded);
		ExpandAction.getAction().setEnabled(any_are_collapsed);
		change_expand_max_action.setEnabled(any_are_expanded);
		ShowOneTierAction.getAction().setEnabled(any_are_separate_tiers);
		ShowTwoTiersAction.getAction().setEnabled(any_are_single_tier);
		change_expand_max_all_action.setEnabled(not_empty);
		rebuildShowMenu();
		viewModeMenu.setEnabled(false);
		transformMenu.setEnabled(false);
		RepackSelectedTiersAction.getAction().setEnabled(0 < this.handler.getSelectedTierLabels().size());

		viewModeMenu.removeAll();
		transformMenu.removeAll();

		if (num_selections == 1) {
			// Check whether this selection is a graph or an annotation
			final TierLabelGlyph label = labels.get(0);
			final TierGlyph glyph = (TierGlyph) label.getInfo();

			final ITrackStyleExtended style = glyph.getAnnotStyle();
//			String meth = style.getMethodName();
//			boolean is_annotation_type = !style.isGraphTier();
//			summaryMenu.setEnabled(is_annotation_type);
//			sym_summarize_single_action.putValue(Action.NAME, glyph.getLabel() + getSymbol(glyph.getDirection()));
//			sym_summarize_both_action.putValue(Action.NAME, glyph.getLabel() + getSymbol(Direction.BOTH));
			//sym_summarize_single_action.setEnabled(is_annotation_type);
			//coverage_action.setEnabled(is_annotation_type);
			//save_menu.setEnabled(is_annotation_type);
			color_by_score_action.setEnabled(true);
			if (glyph.getDirection() != Direction.AXIS) {
				add_maximize = true;
			}


			if (glyph.getInfo() != null && glyph.getInfo() instanceof RootSeqSymmetry) {
				final RootSeqSymmetry rootSym = (RootSeqSymmetry) glyph.getInfo();

				final boolean isSeparate = style.getSeparate();
				for (final MapViewGlyphFactoryI mode : MapViewModeHolder.getInstance().getAllViewModesFor(rootSym.getCategory(), style.getMethodName())) {
					Action action = new GenericAction(mode.getDisplayName(), null) {
						private static final long serialVersionUID = 1L;

						@Override
						public void actionPerformed(ActionEvent ae) {
							if (isSeparate && ! mode.supportsTwoTrack()) {
								setTwoTiers(handler.getSelectedTierLabels(), false);
							}
							ITrackStyleExtended comboStyle = (glyph.getViewModeGlyph() instanceof AbstractGraphGlyph) ? ((AbstractGraphGlyph)glyph.getViewModeGlyph()).getGraphState().getComboStyle() : null;
							TrackView.getInstance().changeViewMode(gviewer, style, mode.getName(), rootSym, comboStyle);
							refreshMap(false, false);
						}
					};
					if(mode.getName().equals(style.getViewMode())){
						action.putValue(Action.SELECTED_KEY, true);
					}
					viewModeMenu.add(new JCheckBoxMenuItem(action));
				}
				viewModeMenu.setEnabled(viewModeMenu.getMenuComponentCount() > 0);
				
				if (style instanceof TrackStyle) {
					for (final Object transform : TransformHolder.getInstance().getAllTransformFor(((TrackStyle) style).getFileTypeCategory())) {
						Operator operator = TransformHolder.getInstance().getOperator(transform.toString());
						final String text = (operator == null) ? transform.toString() : operator.getDisplay();
						Action action = new GenericAction(text, null) {

							private static final long serialVersionUID = 1L;

							@Override
							public void actionPerformed(ActionEvent ae) {
								style.setOperator(transform.toString());
								//gviewer.addAnnotationTrackFor(style);
								refreshMap(false, false);
							}
						};
						if(transform.toString().equals(style.getOperator())){
							action.putValue(Action.SELECTED_KEY, true);
						}
						transformMenu.add(new JCheckBoxMenuItem(action));
					}
					transformMenu.setEnabled(transformMenu.getMenuComponentCount() > 0);
				}
			}
		} else {
			color_by_score_action.setEnabled(false);
			color_by_score_action.putValue(ColorByScoreAction.SELECTED_KEY, false);
//			summaryMenu.setEnabled(false);
			//sym_summarize_single_action.setEnabled(false);
			//coverage_action.setEnabled(false);
		}

		// Gray out several options, if the selected track is coordinates.
		for (TierLabelGlyph label : labels) {
			String name = label.getReferenceTier().getAnnotStyle().getTrackName();
			if (name.equals(TrackConstants.NAME_OF_COORDINATE_INSTANCE)) {
				save_track_action.setEnabled(false);
				strandsMenu.setEnabled(false);
				RepackSelectedTiersAction.getAction().setEnabled(false);
				RepackAllTiersAction.getAction().setEnabled(false);
				RemoveDataFromTracksAction.getAction().setEnabled(false);
				ShowTwoTiersAction.getAction().setEnabled(false);
				color_by_score_action.setEnabled(false);
				break;
			}
		}

		changeMenu.removeAll();
		changeMenu.add(ChangeForegroundColorAction.getAction());
		changeMenu.add(ChangeBackgroundColorAction.getAction());
		changeMenu.add(rename_action);
		changeMenu.add(change_font_size_action);
		changeMenu.add(change_expand_max_action);
		changeMenu.add(new JSeparator());
		changeMenu.add(ShowTwoTiersAction.getAction());
		changeMenu.add(ShowOneTierAction.getAction());
		changeMenu.add(new JSeparator());
		changeMenu.add(set_color_by_score_action);
		changeMenu.add(new JCheckBoxMenuItem(color_by_score_action));
		popup.add(save_track_action);
		save_track_action.setEnabled(false);
		popup.add(save_selected_annotations_action);
		save_selected_annotations_action.setEnabled(false);
		if (num_selections == 1) {
			// Check whether this selection is a graph or an annotation
			TierLabelGlyph label = labels.get(0);
			final TierGlyph glyph = (TierGlyph) label.getInfo();
			ITrackStyleExtended style = glyph.getAnnotStyle();
			GenericFeature feature = style.getFeature();
			save_track_action.setEnabled(true);
			if(!glyph.getSelected().isEmpty()){
				save_selected_annotations_action.setEnabled(true);
			}
			
			if (feature != null) {
				String file_type = style.getFileType();
//				if ("bam".equalsIgnoreCase(file_type) || "sam".equalsIgnoreCase(file_type)) {
//					popup.add(mismatch_action);
//					popup.add(mismatch_pileup_action);
//				}

				if (FileTypeHolder.getInstance().isSequence(file_type)) {
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
		popup.add(CollapseAction.getAction());
		popup.add(ExpandAction.getAction());
		popup.add(new JSeparator());
		popup.add(transformMenu);
		JMenu operationsMenu = addOperationMenu(TrackUtils.getInstance().getSymsFromLabelGlyphs(labels));
		if (operationsMenu != null) {
			popup.add(operationsMenu);
		}
//		summaryMenu.removeAll();
//		summaryMenu.add(sym_summarize_single_action);
//		if (!show_two_tiers.isEnabled()) // If showing both track then give a option to create
//		{
//			summaryMenu.add(sym_summarize_both_action); // depth graph in both direction.
//		}
//		popup.add(summaryMenu);
		popup.add(new JSeparator());
		popup.add(RemoveDataFromTracksAction.getAction()); // Remove data from selected tracks.
		popup.add(RepackSelectedTiersAction.getAction());
		popup.add(RepackAllTiersAction.getAction());

//	strandsMenu.add(at3);


//		popup.add(change_expand_max_action);



		//popup.add(coverage_action);


		if (gviewer.getAutoLoad() != null) {
			popup.add(new JSeparator());
			popup.add(new JMenuItem(gviewer.getAutoLoad()));
		}

		if (DEBUG) {
			popup.add(new AbstractAction("DEBUG") {

				private static final long serialVersionUID = 1L;

				@Override
				public void actionPerformed(ActionEvent e) {
					doDebugAction();
				}
			});
		}
	}

	// purely for debugging
	private void doDebugAction() {
		for (TierGlyph tg : handler.getSelectedTiers()) {
			ITrackStyleExtended style = tg.getAnnotStyle();
			System.out.println("Track: " + tg);
			System.out.println("Style: " + style);
		}
	}

	SeqMapView getSeqMapView() {
		return gviewer;
	}

	public JMenu getShowMenu() {
		rebuildShowMenu();
		return showMenu;
	}

	public TierLabelManager getHandler() {
		return handler;
	}

	private void rebuildShowMenu() {
		showMenu.removeAll();
		showMenu.setEnabled(false);
		List<TierLabelGlyph> tiervec = handler.getAllTierLabels();

		for (TierLabelGlyph label : tiervec) {
			TierGlyph tier = (TierGlyph) label.getInfo();
			final ITrackStyleExtended style = tier.getAnnotStyle();
			if (style != null && !style.getShow() && tier.getDirection() != Direction.REVERSE) {
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

					@Override
					public void actionPerformed(ActionEvent e) {
						style.setShow(true);
						showMenu.remove(show_tier);
						handler.sortTiers();
						repack(false);
					}
				});
				showMenu.add(show_tier);
				showMenu.setEnabled(true);
			}
		}
		show_all_action.setEnabled(containHiddenTiers());
	}
}
