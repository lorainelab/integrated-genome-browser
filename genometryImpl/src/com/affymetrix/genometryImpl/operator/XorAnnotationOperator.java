/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/*package com.affymetrix.genometryImpl.operator;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.util.List;

/**
 *
 * @author auser
 */
/*
public class XorAnnotationOperator extends ExclusiveAnnotationOperator{
	@Override
	public String getName() {
		return "xor";
	}

	@Override
	public SeqSymmetry operate(BioSeq aseq, List<List<SeqSymmetry>> symList) {
		if (symList.size() < getOperandCountMin(FileTypeCategory.Annotation) || symList.size() > getOperandCountMax(FileTypeCategory.Annotation)) {
			return null;
		}
		return getXor(symList.get(0), symList.get(1), aseq);
	}

	@Override
	public int getOperandCountMin(FileTypeCategory category) {
		return category == FileTypeCategory.Annotation ? 2 : 0;
	}

	@Override
	public int getOperandCountMax(FileTypeCategory category) {
		return category == FileTypeCategory.Annotation ? 2 : 0;
	}
	
}*/
