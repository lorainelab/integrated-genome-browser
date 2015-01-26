package com.affymetrix.genometry.operator;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;

/**
 *
 * @author lfrohman
 */
public class ExclusiveBOperator extends ExclusiveOperator implements Operator {
	
	public ExclusiveBOperator(FileTypeCategory fileTypeCategory) {
		super(fileTypeCategory);
	}
	
	@Override
	public String getName() {
		return category.toString().toLowerCase() + "_b_not_a";
	}

	@Override
	public SeqSymmetry operate(BioSeq seq, java.util.List<SeqSymmetry> symList) {
		return operate(seq, symList.get(1), symList.get(0));
	}
}
