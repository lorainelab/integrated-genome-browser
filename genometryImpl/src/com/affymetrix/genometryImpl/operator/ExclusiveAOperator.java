
package com.affymetrix.genometryImpl.operator;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

/**
 *
 * @author lfrohman
 */
public class ExclusiveAOperator extends ExclusiveOperator implements Operator {

	@Override
	public String getName() {
		return "A not B:";
	}

	@Override
	public SeqSymmetry operate(BioSeq seq, java.util.List<SeqSymmetry> symList) {
		return operate(seq, symList.get(0), symList.get(1));
	}
	
}
