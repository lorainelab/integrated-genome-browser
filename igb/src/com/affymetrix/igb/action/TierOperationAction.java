package com.affymetrix.igb.action;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GenericAction;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleSymWithProps;
import com.affymetrix.genometryImpl.symmetry.UcscBedSym;
import com.affymetrix.igb.shared.TierGlyph;
import com.affymetrix.igb.tiers.TrackStyle;
import com.affymetrix.igb.util.TrackUtils;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.viewmode.MapViewModeHolder;

public class TierOperationAction extends GenericAction {
	private static final long serialVersionUID = 1L;
	private static final GenometryModel gmodel = GenometryModel.getGenometryModel();
	private final SeqMapView gviewer;
	private final Operator operator;

	public TierOperationAction(SeqMapView gviewer, Operator operator) {
		super();
		this.gviewer = gviewer;
		this.operator = operator;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		super.actionPerformed(e);
		addTier();
	}

	public Operator getOperator() {
		return operator;
	}

	private void addTier() {
		List<TierGlyph> selected = gviewer.getTierManager().getSelectedTiers();
/////		if (selected.size() < operator.getOperandCountMin() || selected.size() > operator.getOperandCountMax()) {
/////			ErrorHandler.errorPanel(GeneralUtils.getOperandMessage(selected.size(), operator.getOperandCountMin(), operator.getOperandCountMax(), "annotation"));
/////		}
		BioSeq aseq = gmodel.getSelectedSeq();
		SeqSymmetry result_sym = null;
		TrackStyle preferredStyle = null;
		List<SeqSymmetry> seqSymList = new ArrayList<SeqSymmetry>();
		for (TierGlyph tier : selected) {
			SeqSymmetry rootSym = (SeqSymmetry)tier.getInfo();
			if (rootSym == null && tier.getChildCount() > 0) {
				rootSym = (SeqSymmetry)tier.getChild(0).getInfo();
			}
			if (rootSym != null) {
				seqSymList.add(rootSym);
				if (rootSym instanceof SimpleSymWithProps && preferredStyle == null && ((SimpleSymWithProps)rootSym).getProperty("method") != null) {
					preferredStyle = TrackStyle.getInstance(((SimpleSymWithProps)rootSym).getProperty("method").toString(), false);
				}
			}
		}
		result_sym = operator.operate(aseq, seqSymList);
		if (result_sym != null) {
			StringBuilder meth = new StringBuilder();
			if (result_sym instanceof UcscBedSym) {
				meth.append(((UcscBedSym)result_sym).getType());
			}
			else {
				meth.append(operator.getName() + ": ");
				for (TierGlyph tier : selected) {
					meth.append(tier.getLabel()).append(", ");
				}
			}
			preferredStyle.setViewMode(MapViewModeHolder.getInstance().getDefaultFactoryFor(operator.getOutputCategory()).getName());
			TrackUtils.getInstance().addTrack(result_sym, meth.toString(), preferredStyle);
		}
	}

	public boolean isEnabled() {
		return true;
	}

	@Override
	public String getText() {
		return null;
	}
}
