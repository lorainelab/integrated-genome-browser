
package com.affymetrix.genometry.operator;

import java.util.ArrayList;
import java.util.List;

import com.affymetrix.genometry.GenometryConstants;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractAnnotationOperator implements Operator {
	final protected FileTypeCategory category;
        private static final Logger logger = LoggerFactory.getLogger(AbstractAnnotationOperator.class);
	
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
                    logger.error("Error while cloning Operator ", ex);
		}
		return null;
	}
	
	protected static List<SeqSymmetry> findChildSyms(SeqSymmetry sym) {
		List<SeqSymmetry> childSyms = new ArrayList<>();
		for (int i = 0; i < sym.getChildCount(); i++) {
			childSyms.add(sym.getChild(i));
		}
		return childSyms;
	}
}
