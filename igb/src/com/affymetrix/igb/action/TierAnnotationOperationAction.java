package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.operator.annotation.AnnotationOperator;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.tiers.TierGlyph;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import com.affymetrix.igb.tiers.TrackStyle;
import com.affymetrix.igb.view.SeqMapView;

public class TierAnnotationOperationAction extends AbstractAction {
	private static final long serialVersionUID = 1L;
	private static final GenometryModel gmodel = GenometryModel.getGenometryModel();
	private final SeqMapView gviewer;
	private final AnnotationOperator annotationOperator;

	public TierAnnotationOperationAction(SeqMapView gviewer, AnnotationOperator annotationOperator) {
		super();
		this.gviewer = gviewer;
		this.annotationOperator = annotationOperator;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		addTier(annotationOperator);
	}

	public AnnotationOperator getAnnotationOperator() {
		return annotationOperator;
	}

	private void addTier(AnnotationOperator annotationOperator) {
		List<TierGlyph> selected = gviewer.getTierManager().getSelectedTiers();
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

	public boolean isEnabled() {
		List<TierLabelGlyph> labels = gviewer.getTierManager().getSelectedTierLabels();
		int num_selected = labels.size();
		return num_selected >= getAnnotationOperator().getOperandCountMin() &&
			num_selected <= getAnnotationOperator().getOperandCountMax();
	}
}
