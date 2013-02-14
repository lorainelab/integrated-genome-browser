package com.affymetrix.genometryImpl.operator;

import java.util.HashMap;
import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.TypeContainerAnnot;

public class CopyXOperator extends AbstractAnnotationTransformer implements Operator, ICopy{

	public CopyXOperator(FileTypeCategory category){
		super(category);
	}
			
	@Override
	public String getName() {
		return fileTypeCategory.toString().toLowerCase() + "_copy";
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
	public FileTypeCategory getOutputCategory() {
		return this.fileTypeCategory;
	}
}
