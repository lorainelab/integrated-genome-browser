package com.affymetrix.igb.tiers;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.BioSeq;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.operator.annotation.AnnotationOperator;
import com.affymetrix.genometryImpl.operator.annotation.AnnotationOperatorHolder;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.view.SeqMapView;

/**
 *  A PopupListener that adds the ability to create "union", "intersection", etc.,
 *  tiers based on selected annotation tiers.  Is not used on graph tiers.
 */
public final class TierArithmetic implements TierLabelManager.PopupListener {
	private static final GenometryModel gmodel = GenometryModel.getGenometryModel();
	private final SeqMapView gviewer;
	private final TierLabelManager handler;

	private class AnnotJMenuItem extends JMenuItem {
		private static final long serialVersionUID = 1L;
		private final AnnotationOperator annotationOperator;
		public AnnotJMenuItem(String title, AnnotationOperator annotationOperator_) {
			super(title);
			this.annotationOperator = annotationOperator_;
			addActionListener(
				new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						addTier(annotationOperator);
					}
				}
			);
		}
		public AnnotationOperator getAnnotationOperator() {
			return annotationOperator;
		}
	}

	public TierArithmetic(TierLabelManager handler, SeqMapView gviewer) {
		this.handler = handler;
		this.gviewer = gviewer;
	}

	private void addTier(AnnotationOperator annotationOperator) {
		List<TierGlyph> selected = handler.getSelectedTiers();
		if (selected.size() < annotationOperator.getOperandCountMin() || selected.size() > annotationOperator.getOperandCountMax()) {
			ErrorHandler.errorPanel(GeneralUtils.getOperandMessage(selected.size(), annotationOperator.getOperandCountMin(), annotationOperator.getOperandCountMax(), "annotation"));
		}
		BioSeq aseq = gmodel.getSelectedSeq();
		List<List<SeqSymmetry>> symList = new ArrayList<List<SeqSymmetry>>();
		for (TierGlyph tier : selected) {	
			List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>();
			findChildSyms(tier, syms);
			symList.add(syms);
		}
		SeqSymmetry result_sym = annotationOperator.operate(aseq, symList);
		if (result_sym != null) {
			StringBuilder meth = new StringBuilder();
			meth.append(annotationOperator.getName() + ": ");
			for (TierGlyph tier : selected) {
				meth.append(tier.getLabel()).append(", ");
			}
			addStyleAndAnnotation(result_sym, meth.toString(), aseq);
		}
	}

	private static void findChildSyms(TierGlyph tiers, List<SeqSymmetry> list) {
		for (GlyphI child : tiers.getChildren()) {
			SeqSymmetry csym = (SeqSymmetry) child.getInfo();
			if (csym != null) {
				list.add(csym);
			}
		}
	}
	
	private void addStyleAndAnnotation(SeqSymmetry sym, String method, BioSeq aseq) {
		makeNonPersistentStyle((SymWithProps) sym, method);
		aseq.addAnnotation(sym);
		gviewer.setAnnotatedSeq(aseq, true, true);
	}

	public void popupNotify(JPopupMenu popup, TierLabelManager handler) {
		if (handler != this.handler) {
			throw new RuntimeException("");
		}
		JMenu combineMenu = new JMenu("Combine Selected Tracks");
		for (AnnotationOperator annotationOperator : AnnotationOperatorHolder.getInstance().getAnnotationOperators()) {
			String name = annotationOperator.getName();
			String title = name.substring(0, 1).toUpperCase() + name.substring(1);
			AnnotJMenuItem operatorMI = new AnnotJMenuItem(title, annotationOperator);
			combineMenu.add(operatorMI);
		}
		List<TierLabelGlyph> labels = handler.getSelectedTierLabels();
		int num_selected = labels.size();
		boolean all_are_annotations = areAllAnnotations(labels);

		combineMenu.setEnabled(all_are_annotations && num_selected > 0);
		for (int i = 0; i < combineMenu.getItemCount(); i++) {
			if (combineMenu.getItem(i) instanceof AnnotJMenuItem) {
				AnnotJMenuItem annotJMenuItem = (AnnotJMenuItem)combineMenu.getItem(i);
				annotJMenuItem.setEnabled(
					all_are_annotations &&
					num_selected >= annotJMenuItem.getAnnotationOperator().getOperandCountMin() &&
					num_selected <= annotJMenuItem.getAnnotationOperator().getOperandCountMax()
				);
			}
		}

		popup.add(combineMenu);
	}

	private boolean areAllAnnotations(List<TierLabelGlyph> labels) {
		for (TierLabelGlyph tlg : labels) {
			if (tlg.getReferenceTier().getAnnotStyle().isGraphTier()) {
				return false;
			}
		}
		return true;
	}

	private static TrackStyle makeNonPersistentStyle(SymWithProps sym, String human_name) {
		// Needs a unique name so that if any later tier is produced with the same
		// human name, it will not automatically get the same color, etc.
		String unique_name = TrackStyle.getUniqueName(human_name);
		sym.setProperty("method", unique_name);
		TrackStyle style = TrackStyle.getInstance(unique_name, false);
		style.setHumanName(human_name);
		style.setGlyphDepth(1);
		style.setSeparate(false); // there are not separate (+) and (-) strands
		style.setCustomizable(false); // the user can change the color, but not much else is meaningful
		return style;
	}
}
