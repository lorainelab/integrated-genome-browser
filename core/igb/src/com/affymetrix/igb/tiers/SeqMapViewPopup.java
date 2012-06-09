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
import com.affymetrix.genometryImpl.event.GenericActionHolder;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.operator.OperatorComparator;
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
import com.affymetrix.igb.viewmode.TransformHolder;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.ResourceBundle;
import java.util.TreeSet;

import javax.swing.*;

public final class SeqMapViewPopup implements TierLabelManager.PopupListener {

	private static final boolean DEBUG = false;
	private ResourceBundle BUNDLE = IGBConstants.BUNDLE;
	private final SeqMapView gviewer;
	private final TierLabelManager handler;
	private final JMenu strandsMenu = new JMenu(BUNDLE.getString("strandsMenu"));
	private final ActionToggler at1;
	private final ActionToggler at2;
	private final RepackTiersAction repackStub;

	public SeqMapViewPopup(TierLabelManager handler, SeqMapView smv) {
		this.handler = handler;
		this.gviewer = smv;
		this.repackStub = new RepackTiersAction(null, null, null) {
			private static final long serialVersionUID = 1L;
		};
		at1 = new ActionToggler(smv.getClass().getSimpleName() + "_SeqMapViewPopup.showPlus", ShowPlusStrandAction.getAction());
		at2 = new ActionToggler(smv.getClass().getSimpleName() + "_SeqMapViewPopup.showMinus", ShowMinusStrandAction.getAction());
	}

	public void refreshMap(boolean stretch_vertically, boolean stretch_horizonatally) {
		if (gviewer != null) {
			// if an AnnotatedSeqViewer is being used, ask it to update itself.
			// later this can be made more specific to just update the tiers that changed
			boolean preserve_view_x = !stretch_vertically;
			boolean preserve_view_y = !stretch_horizonatally;
			gviewer.updatePanel(preserve_view_x, preserve_view_y);
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
		TreeSet<Operator> operators = new TreeSet<Operator>(new OperatorComparator());
		operators.addAll(ExtensionPointHandler.getExtensionPoint(Operator.class).getExtensionPointImpls());
		for (Operator operator : operators) {
			if (TrackUtils.getInstance().checkCompatible(syms, operator, false)) { // cannot handle Operators with parameters
				String title = operator.getDisplay();
				JMenuItem operatorMI = new JMenuItem(title);
				operatorMI.addActionListener(new TrackOperationAction(gviewer, operator));
				if (operationsMenu == null) {
					operationsMenu = new JMenu(BUNDLE.getString("operationsMenu"));
				}
				operationsMenu.add(operatorMI);
			}
		}
		return operationsMenu;
	}

	private JMenu addViewModeMenu(final TierGlyph glyph) {
		JMenu viewModeMenu = new JMenu(BUNDLE.getString("viewModeMenu"));
		if (glyph != null && glyph.getInfo() != null && glyph.getInfo() instanceof RootSeqSymmetry) {
			final RootSeqSymmetry rootSym = (RootSeqSymmetry) glyph.getInfo();
			final ITrackStyleExtended style = glyph.getAnnotStyle();
			for (final MapViewGlyphFactoryI mode : MapViewModeHolder.getInstance().getAllViewModesFor(rootSym.getCategory(), style.getMethodName())) {
				Action action = new ChangeViewModeAction(mode);
				if(mode.getName().equals(style.getViewMode())){
					action.putValue(Action.SELECTED_KEY, true);
				}
				viewModeMenu.add(new JCheckBoxMenuItem(action));
			}
		}
		viewModeMenu.setEnabled(viewModeMenu.getMenuComponentCount() > 0);
		return viewModeMenu;
	}

	private JMenu addTransformMenu(TierGlyph glyph) {
		JMenu transformMenu = new JMenu(BUNDLE.getString("transformMenu"));
		if (glyph != null && glyph.getInfo() != null && glyph.getInfo() instanceof RootSeqSymmetry) {
			final ITrackStyleExtended style = glyph.getAnnotStyle();
			if (style instanceof TrackStyle) {
				TreeSet<Operator> operators = new TreeSet<Operator>(new OperatorComparator());
				operators.addAll(TransformHolder.getInstance().getAllTransformFor(((TrackStyle) style).getFileTypeCategory()));
				for (final Operator operator : operators) {
					Action action = new TransformAction(operator);
					if (operator.getName().equals(style.getOperator())) {
						action.putValue(Action.SELECTED_KEY, true);
					}
					transformMenu.add(new JCheckBoxMenuItem(action));
				}
				transformMenu.setEnabled(transformMenu.getMenuComponentCount() > 0);
			}
		}
		transformMenu.setEnabled(transformMenu.getMenuComponentCount() > 0);
		return transformMenu;
	}

	private JMenu addChangeMenu(int num_selections, boolean any_are_expanded, boolean any_are_separate_tiers, boolean any_are_single_tier, boolean any_are_color_off, boolean coordinates_track_selected) {
		JMenu changeMenu = new JMenu(BUNDLE.getString("changeMenu"));
		JMenuItem change_foreground_color = new JMenuItem(ChangeForegroundColorAction.getAction());
		change_foreground_color.setEnabled(num_selections > 0);
		changeMenu.add(change_foreground_color);
		JMenuItem change_background_color = new JMenuItem(ChangeBackgroundColorAction.getAction());
		change_background_color.setEnabled(num_selections > 0);
		changeMenu.add(change_background_color);
		JMenuItem rename = new JMenuItem(RenameAction.getAction());
		rename.setEnabled(num_selections == 1);
		changeMenu.add(rename);
		JMenuItem change_font_size = new JMenuItem(ChangeFontSizeAction.getAction());
		change_font_size.setEnabled(num_selections > 0);
		changeMenu.add(change_font_size);
		JMenuItem change_expand_max = new JMenuItem(ChangeExpandMaxAction.getAction());
		change_expand_max.setEnabled(any_are_expanded);
		changeMenu.add(change_expand_max);
		JMenuItem change_expand_max_all = new JMenuItem(ChangeExpandMaxAllAction.getAction());
		change_expand_max_all.setEnabled(num_selections > 0);
		changeMenu.add(change_expand_max_all);
		changeMenu.add(new JSeparator());
		JMenuItem show_two_tiers = new JMenuItem(ShowTwoTiersAction.getAction());
		GenericActionHolder.getInstance().addGenericAction(ShowTwoTiersAction.getAction());
		show_two_tiers.setEnabled(any_are_single_tier && num_selections > 0 && !coordinates_track_selected);
		changeMenu.add(show_two_tiers);
		JMenuItem show_one_tier = new JMenuItem(ShowOneTierAction.getAction());
		GenericActionHolder.getInstance().addGenericAction(ShowOneTierAction.getAction());
		show_one_tier.setEnabled(any_are_separate_tiers);
		changeMenu.add(show_one_tier);
		changeMenu.add(new JSeparator());
		JMenuItem set_color_by_score = new JMenuItem(SetColorByScoreAction.getAction());
		changeMenu.add(set_color_by_score);
		JCheckBoxMenuItem color_by_score = new JCheckBoxMenuItem(ColorByScoreAction.getAction());
		color_by_score.setSelected(!any_are_color_off && num_selections > 0 && !coordinates_track_selected);
		color_by_score.setEnabled(num_selections == 1);
		changeMenu.add(color_by_score);
		return changeMenu;
	}

	private JMenu addShowMenu(boolean containHiddenTiers) {
		final JMenu showMenu = new JMenu(BUNDLE.getString("showMenu"));
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
							name = "<" + BUNDLE.getString("unnamed") + ">";
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
		showMenu.setEnabled(containHiddenTiers);
		return showMenu;
	}

	@Override
	public void popupNotify(javax.swing.JPopupMenu popup, final TierLabelManager handler) {
		final List<TierLabelGlyph> labels = handler.getSelectedTierLabels();
		int num_selections = labels.size();
		boolean any_are_collapsed = false;
		boolean any_are_expanded = false;
		boolean any_are_color_on = false; // whether any allow setColorByScore()
		boolean any_are_color_off = false; // whether any allow setColorByScore()
		boolean any_are_separate_tiers = false;
		boolean any_are_single_tier = false;
		boolean any_view_mode = false;
		boolean coordinates_track_selected = false;
		boolean containHiddenTiers = false;

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
			String name = label.getReferenceTier().getAnnotStyle().getTrackName();
			if (name.equals(TrackConstants.NAME_OF_COORDINATE_INSTANCE)) {
				coordinates_track_selected = true;
			}
		}

		for (TierLabelGlyph label : handler.getAllTierLabels()) {
			TierGlyph tier = (TierGlyph) label.getInfo();
			ITrackStyleExtended style = tier.getAnnotStyle();
			if (!style.getShow()) {
				containHiddenTiers = true;
			}
		}

		TierGlyph tierGlyph = (num_selections == 1 ? (TierGlyph) labels.get(0).getInfo() : null);
		JMenuItem save_track = new JMenuItem(ExportFileAction.getAction());
		save_track.setEnabled(num_selections == 1 && !coordinates_track_selected);
		popup.add(save_track);
		JMenuItem save_selected_annotations = new JMenuItem(ExportSelectedAnnotationFileAction.getAction());
		save_selected_annotations.setEnabled(tierGlyph != null && !tierGlyph.getSelected().isEmpty());
		popup.add(save_selected_annotations);
		if (tierGlyph != null) {
			// Check whether this selection is a graph or an annotation
			ITrackStyleExtended style = tierGlyph.getAnnotStyle();
			GenericFeature feature = style.getFeature();
			if (feature != null) {
				String file_type = style.getFileType();
				if (FileTypeHolder.getInstance().isSequence(file_type)) {
					popup.add(new JSeparator());
					JMenuItem use_as_reference_seq = new JMenuItem(UseAsReferenceSeqAction.getAction());
					popup.add(use_as_reference_seq);
				}

				if (feature.friendlyURL != null) {
					popup.add(new JMenuItem(new FeatureInfoAction(feature.friendlyURL.toString())));
				}
			}
		}
		popup.add(new JSeparator());
		JMenuItem customize = new JMenuItem(CustomizeAction.getAction());
		popup.add(customize);
		popup.add(addChangeMenu(num_selections, any_are_expanded, any_are_separate_tiers, any_are_single_tier, any_are_color_off, coordinates_track_selected));
		popup.add(addViewModeMenu(labels.size() == 1 ? (TierGlyph) labels.get(0).getInfo() : null));
		popup.add(new JSeparator());
		JMenuItem hide = new JMenuItem(HideAction.getAction());
		hide.setEnabled(num_selections > 0);
		popup.add(hide);
		popup.add(addShowMenu(containHiddenTiers));
		JMenuItem show_all = new JMenuItem(ShowAllAction.getAction());
		show_all.setEnabled(containHiddenTiers);
		popup.add(show_all);
		strandsMenu.removeAll();
		strandsMenu.add(at1);
		strandsMenu.add(at2);
		strandsMenu.setEnabled(!coordinates_track_selected);
		popup.add(strandsMenu);
		popup.add(new JSeparator());
		popup.add(new JMenuItem(CenterAtHairlineAction.getAction()));
		if (num_selections == 1 && ((TierGlyph) labels.get(0).getInfo()).getDirection() != Direction.AXIS) {
			JMenuItem maximize_track = new JMenuItem(MaximizeTrackAction.getAction());
			popup.add(maximize_track);
		}
		JMenuItem collapse = new JMenuItem(CollapseAction.getAction());
		collapse.setEnabled(any_are_expanded);
		popup.add(collapse);
		JMenuItem expand = new JMenuItem(ExpandAction.getAction());
		expand.setEnabled(any_are_collapsed);
		popup.add(expand);
		popup.add(new JSeparator());
		popup.add(addTransformMenu(labels.size() == 1 ? (TierGlyph) labels.get(0).getInfo() : null));
		JMenu operationsMenu = addOperationMenu(TrackUtils.getInstance().getSymsFromLabelGlyphs(labels));
		if (operationsMenu != null) {
			popup.add(operationsMenu);
		}
		popup.add(new JSeparator());
		JMenuItem remove_data_from_tracks = new JMenuItem(RemoveDataFromTracksAction.getAction());
		remove_data_from_tracks.setEnabled(num_selections > 0 && !coordinates_track_selected);
		popup.add(remove_data_from_tracks); // Remove data from selected tracks.
		JMenuItem repack_selected_tiers = new JMenuItem(RepackSelectedTiersAction.getAction());
		repack_selected_tiers.setEnabled(num_selections > 0 && !coordinates_track_selected);
		popup.add(repack_selected_tiers);
		JMenuItem repack_all_tiers = new JMenuItem(RepackAllTiersAction.getAction());
		repack_all_tiers.setEnabled(!coordinates_track_selected);
		popup.add(repack_all_tiers);

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
}
