package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.general.BoundedParameter;
import com.affymetrix.genometryImpl.general.Parameter;
import com.affymetrix.genometryImpl.operator.comparator.MathComparisonOperator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.BAMSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author hiralv
 */
public class PairedFilter extends SymmetryFilter {
	private final static String COMPARATOR = "show";
	private final static List<Boolean> COMPARATOR_VALUES = new LinkedList<Boolean>();
	static {
		COMPARATOR_VALUES.add(Boolean.TRUE);
		COMPARATOR_VALUES.add(Boolean.FALSE);
	}
	private Parameter<Boolean> comparator = new BoundedParameter<Boolean>(COMPARATOR_VALUES);
	
	public PairedFilter(){
		super();
		parameters.addParameter(COMPARATOR, Boolean.class, comparator);
	}
	
	@Override
    public String getName() {
        return "paired";
    }
	
	@Override
	public boolean isFileTypeCategorySupported(FileTypeCategory fileTypeCategory){
		return fileTypeCategory == FileTypeCategory.Alignment;
	}
	
    @Override
    public boolean filterSymmetry(BioSeq bioseq, SeqSymmetry ss) {
		if (ss instanceof BAMSym) {
			return comparator.get() == ((BAMSym)ss).getReadPairedFlag();
		}
		return false;
    }

}

