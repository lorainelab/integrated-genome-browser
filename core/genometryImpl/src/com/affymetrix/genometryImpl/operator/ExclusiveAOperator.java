
package com.affymetrix.genometryImpl.operator;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryConstants;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

/**
 *
 * @author lfrohman
 */
public class ExclusiveAOperator extends ExclusiveOperator implements Operator {
	
	public ExclusiveAOperator(FileTypeCategory fileTypeCategory) {
		super(fileTypeCategory);
	}

	@Override
	public String getName() {
		return category.toString().toLowerCase() + "_a_not_b";
	}

	@Override
	public String getDisplay() {
		return GenometryConstants.BUNDLE.getString("operator_" + getName());
	}

	@Override
	public SeqSymmetry operate(BioSeq seq, java.util.List<SeqSymmetry> symList) {
		return operate(seq, symList.get(0), symList.get(1));
	}
	
}
