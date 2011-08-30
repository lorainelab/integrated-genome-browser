package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.operator.annotation.AnnotationOperator;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.shared.TierGlyph;
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
		SeqSymmetry result_sym = null;
		TrackStyle preferredStyle = null;
		List<SeqSymmetry> seqSymList = new ArrayList<SeqSymmetry>();
		for (TierGlyph tier : selected) {	
			if (tier.getInfo() instanceof SeqSymmetry) {
				seqSymList.add((SeqSymmetry)tier.getInfo());
				if (tier.getInfo() instanceof SimpleSymWithProps && preferredStyle == null && ((SimpleSymWithProps)tier.getInfo()).getProperty("method") != null) {
					preferredStyle = TrackStyle.getInstance(((SimpleSymWithProps)tier.getInfo()).getProperty("method").toString(), false);
				}
			}
		}
		result_sym = annotationOperator.operate(seqSymList);
		if (result_sym == null) {
			preferredStyle = null;
			List<List<SeqSymmetry>> symList = new ArrayList<List<SeqSymmetry>>();
			for (TierGlyph tier : selected) {	
				List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>();
				findChildSyms(tier, syms);
				symList.add(syms);
			}
			result_sym = annotationOperator.operate(aseq, symList);
		}
		if (result_sym != null) {
			StringBuilder meth = new StringBuilder();
			meth.append(annotationOperator.getName() + ": ");
			for (TierGlyph tier : selected) {
				meth.append(tier.getLabel()).append(", ");
			}
			addStyleAndAnnotation(result_sym, meth.toString(), aseq, preferredStyle);
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
	
	private void addStyleAndAnnotation(SeqSymmetry sym, String method, BioSeq aseq, TrackStyle preferredStyle) {
		makeNonPersistentStyle((SymWithProps) sym, method, preferredStyle);
		aseq.addAnnotation(sym);
		gviewer.setAnnotatedSeq(aseq, true, true);
	}

	private static TrackStyle makeNonPersistentStyle(SymWithProps sym, String human_name, TrackStyle preferredStyle) {
		// Needs a unique name so that if any later tier is produced with the same
		// human name, it will not automatically get the same color, etc.
		String unique_name = TrackStyle.getUniqueName(human_name);
		sym.setProperty("method", unique_name);
		if (!BioSeq.needsContainer(sym) && sym.getProperty("id") == null) {
			sym.setProperty("id", unique_name);
		}
		TrackStyle style = TrackStyle.getInstance(unique_name, false);
		if (preferredStyle == null) {
			style.setTrackName(human_name);
			style.setGlyphDepth(1);
			style.setSeparate(false); // there are not separate (+) and (-) strands
			style.setCustomizable(false); // the user can change the color, but not much else is meaningful
		}
		else {
			style.copyPropertiesFrom(preferredStyle);
		}
		return style;
	}

	public boolean isEnabled() {
		List<TierLabelGlyph> labels = gviewer.getTierManager().getSelectedTierLabels();
		int num_selected = labels.size();
		return num_selected >= getAnnotationOperator().getOperandCountMin() &&
			num_selected <= getAnnotationOperator().getOperandCountMax();
	}
}
