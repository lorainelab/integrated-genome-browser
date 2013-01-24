
package com.affymetrix.genometryImpl.operator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

public abstract class AbstractAnnotationOperator implements Operator {
	final protected FileTypeCategory category;
	
	protected AbstractAnnotationOperator(){
		this.category = FileTypeCategory.Annotation;
	}
	
	protected AbstractAnnotationOperator(FileTypeCategory category){
		this.category = category;
	}
	
	@Override
	public int getOperandCountMin(FileTypeCategory category) {
		return category == this.category ? 2 : 0;
	}

	@Override
	public int getOperandCountMax(FileTypeCategory category) {
		return category == this.category ? 2 : 0;
	}

	@Override
	public Map<String, Class<?>> getParameters() {
		return null;
	}

	@Override
	public boolean setParameters(Map<String, Object> parms) {
		return false;
	}

	@Override
	public boolean supportsTwoTrack() {
		return false;
	}

	@Override
	public FileTypeCategory getOutputCategory() {
		return this.category;
	}

	protected static List<SeqSymmetry> findChildSyms(SeqSymmetry sym) {
		List<SeqSymmetry> childSyms = new ArrayList<SeqSymmetry>();
		for (int i = 0; i < sym.getChildCount(); i++) {
			childSyms.add(sym.getChild(i));
		}
		return childSyms;
	}
}
