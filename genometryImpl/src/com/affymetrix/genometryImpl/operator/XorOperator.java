
package com.affymetrix.genometryImpl.operator;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

/**
 *
 * @author lfrohman
 */
public class XorOperator extends ExclusiveOperator implements Operator {

	@Override
	public String getName() {
		return "xor";
	}

	@Override
	public SeqSymmetry operate(BioSeq aseq, java.util.List<SeqSymmetry> symList) {
		return getXor(aseq, findChildSyms(symList.get(0)), findChildSyms(symList.get(1)));
	}
	
}
