
package com.affymetrix.igb.view;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.operator.Operator;
import com.affymetrix.genometry.symmetry.impl.SeqSymSummarizer;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.igb.IGBConstants;
import java.util.List;

/**
 *
 * @author hiralv
 */
public class MismatchOperator extends AbstractMismatchOperator implements Operator {

	@Override
	public String getName() {
		return "mismatch";
	}

	@Override
	public String getDisplay() {
		return IGBConstants.BUNDLE.getString("operator_" + getName());
	}

	@Override
	public SeqSymmetry getMismatch(List<SeqSymmetry> syms, BioSeq seq, boolean binary_depth, String id, int start, int end) {
		return SeqSymSummarizer.getMismatchGraph(syms, seq, false, id, start, end, false);
	}

}
