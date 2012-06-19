package com.affymetrix.genometryImpl.operator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryConstants;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.TypeContainerAnnot;

public class CopyAlignmentOperator implements Operator, ICopy {

	@Override
	public String getName() {
		return "copyalignment";
	}

	@Override
	public String getDisplay() {
		return GenometryConstants.BUNDLE.getString("operator_" + getName());
	}

	@Override
	public SeqSymmetry operate(BioSeq aseq, List<SeqSymmetry> symList) {
		if (symList.size() != 1 || !(symList.get(0) instanceof TypeContainerAnnot)) {
			return null;
		}
		TypeContainerAnnot result = null;
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
		return result;
	}

	@Override
	public int getOperandCountMin(FileTypeCategory category) {
		return category == FileTypeCategory.Alignment ? 1 : 0;
	}

	@Override
	public int getOperandCountMax(FileTypeCategory category) {
		return category == FileTypeCategory.Alignment ? 1 : 0;
	}

	@Override
	public Map<String, Class<?>> getParameters() {
		return null;
	}

	@Override
	public boolean setParameters(Map<String, Object> obj) {
		return false;
	}

	@Override
	public boolean supportsTwoTrack() {
		return false;
	}

	@Override
	public FileTypeCategory getOutputCategory() {
		return FileTypeCategory.Alignment;
	}
}
