package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.general.BoundedParameter;
import com.affymetrix.genometryImpl.general.Parameter;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.impl.BAMSym;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author hiralv
 */
public class DuplicateFilter extends SymmetryFilter {

	private static enum SHOW {
		DUPLICATES("Duplicates only", Boolean.TRUE), NON_DUPLICATES("Non-Duplicates only", Boolean.FALSE);
		
		String name;
		boolean value;
		SHOW(String name, boolean value){
			this.name = name;
			this.value = value;
		}
		
		public boolean value(){
			return value;
		}
		
		@Override
		public String toString(){
			return name;
		}
	}
	
	private final static String COMPARATOR = "show";
	private final static List<SHOW> COMPARATOR_VALUES = new LinkedList<>();
	static {
		COMPARATOR_VALUES.add(SHOW.DUPLICATES);
		COMPARATOR_VALUES.add(SHOW.NON_DUPLICATES);
	}
	private Parameter<SHOW> comparator = new BoundedParameter<>(COMPARATOR_VALUES);
	
	public DuplicateFilter(){
		super();
		parameters.addParameter(COMPARATOR, SHOW.class, comparator);
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
			return comparator.get().value() == ((BAMSym)ss).getDuplicateReadFlag();
		}
		return false;
    }

}