/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 *//*
package com.affymetrix.genometryImpl.operator;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.util.List;

/**
 *
 * @author auser
 *//*
public class ExclusiveAAnnotationOperator extends ExclusiveAnnotationOperator{
	@Override
	public String getName() {
		return "A not B:";
	}

	@Override
	public SeqSymmetry operate(BioSeq seq, List<SeqSymmetry> symList) {
		if (symList.size() < getOperandCountMin(FileTypeCategory.Annotation) || symList.size(FileTypeCategory.Annotation) > getOperandCountMax()) {
			return null;
		}
		return operate(seq, symList.get(0), symList.get(1));
	}	
}*/
