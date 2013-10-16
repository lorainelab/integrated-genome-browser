package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.general.BoundedParameter;
import com.affymetrix.genometryImpl.general.Parameter;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.BAMSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author hiralv
 */
public class DuplicateFilter extends SymmetryFilter {
	private final static String COMPARATOR = "comparator";
	private final static List<Boolean> COMPARATOR_VALUES = new LinkedList<Boolean>();
	static {
		COMPARATOR_VALUES.add(Boolean.TRUE);
		COMPARATOR_VALUES.add(Boolean.FALSE);
	}
	private Parameter<Boolean> comparator = new BoundedParameter<Boolean>(COMPARATOR_VALUES);
	
	public DuplicateFilter(){
		super();
		parameters.addParameter(COMPARATOR, Boolean.class, comparator);
	}
	
	@Override
    public String getName() {
        return "duplicate";
    }
	
	@Override
	public boolean isFileTypeCategorySupported(FileTypeCategory fileTypeCategory){
		return fileTypeCategory == FileTypeCategory.Alignment;
	}
	
    @Override
    public boolean filterSymmetry(BioSeq bioseq, SeqSymmetry ss) {
		if (ss instanceof BAMSym) {
			return comparator.get() == ((BAMSym)ss).getDuplicateReadFlag();
		}
		return false;
    }

}