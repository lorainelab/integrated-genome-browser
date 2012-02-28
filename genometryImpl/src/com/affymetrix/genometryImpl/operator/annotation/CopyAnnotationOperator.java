package com.affymetrix.genometryImpl.operator.annotation;

import java.util.HashMap;
import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.TypeContainerAnnot;

public class CopyAnnotationOperator implements AnnotationOperator {

	@Override
	public String getName() {
		return "copy";
	}

	@Override
	public SeqSymmetry operate(BioSeq seq, List<List<SeqSymmetry>> symList) {
		return null;
	}

	@Override
	public SeqSymmetry operate(List<SeqSymmetry> symList) {
		if (symList.size() < getOperandCountMin() || symList.size() > getOperandCountMax()) {
			return null;
		}
		TypeContainerAnnot result = null;
		if (symList.get(0) instanceof TypeContainerAnnot) {
			TypeContainerAnnot t = (TypeContainerAnnot)symList.get(0);
			result = new TypeContainerAnnot(t.getType());
			// copy children
			for (int i = 0; i < t.getChildCount(); i++) {
				result.addChild(t.getChild(i));
			}
			// copy spans
			for (int i = 0; i < t.getSpanCount(); i++) {
				result.addSpan(t.getSpan(i));
			}
			// copy properties
			result.setProperties(new HashMap<String,Object>(t.getProperties()));
		}
		return result;
	}

	@Override
	public int getOperandCountMin() {
		return 1;
	}

	@Override
	public int getOperandCountMax() {
		return 1;
	}
}
