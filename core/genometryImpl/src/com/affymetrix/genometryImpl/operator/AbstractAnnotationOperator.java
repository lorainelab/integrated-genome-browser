
package com.affymetrix.genometryImpl.operator;

import java.util.ArrayList;
import java.util.List;

import com.affymetrix.genometryImpl.GenometryConstants;
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
	public String getDisplay() {
		return GenometryConstants.BUNDLE.getString("operator_" + getName());
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
	public boolean supportsTwoTrack() {
		return false;
	}

	@Override
	public FileTypeCategory getOutputCategory() {
		return FileTypeCategory.Annotation;
	}

	@Override
	public Operator newInstance(){
		try {
			return getClass().getConstructor(FileTypeCategory.class).newInstance(category);
		} catch (Exception ex) {
		}
		return null;
	}
	
	protected static List<SeqSymmetry> findChildSyms(SeqSymmetry sym) {
		List<SeqSymmetry> childSyms = new ArrayList<SeqSymmetry>();
		for (int i = 0; i < sym.getChildCount(); i++) {
			childSyms.add(sym.getChild(i));
		}
		return childSyms;
	}
}
