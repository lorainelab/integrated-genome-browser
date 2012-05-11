package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.symmetry.SeqSymSummarizer;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.igb.IGBConstants;

import java.util.List;

/**
 *
 * @author hiralv
 */
public class MismatchPileupOperator extends AbstractMismatchOperator implements Operator {

	@Override
	public String getName() {
		return "mismatchpileup";
	}

	@Override
	public String getDisplay() {
		return IGBConstants.BUNDLE.getString("operator_" + getName());
	}

	@Override
	public SeqSymmetry getMismatch(List<SeqSymmetry> syms, BioSeq seq, boolean binary_depth, String id, int start, int end) {
		return SeqSymSummarizer.getMismatchGraph(syms, seq, false, id, start, end, true);
	}
}
