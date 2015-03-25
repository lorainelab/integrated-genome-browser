package com.affymetrix.genometry.filter;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.general.BoundedParameter;
import com.affymetrix.genometry.general.Parameter;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.symmetry.impl.BAMSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author hiralv
 */
public class PairedFilter extends SymmetryFilter {

	private static enum SHOW {
		READ_WITH_MATES("Read with mates only", Boolean.TRUE), READ_WITHOUT_MATES("Read without mates only", Boolean.FALSE);
		
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
		COMPARATOR_VALUES.add(SHOW.READ_WITH_MATES);
		COMPARATOR_VALUES.add(SHOW.READ_WITHOUT_MATES);
	}
	private Parameter<SHOW> comparator = new BoundedParameter<>(COMPARATOR_VALUES);
	
	public PairedFilter(){
		super();
		parameters.addParameter(COMPARATOR, SHOW.class, comparator);
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
			return comparator.get().value() == ((BAMSym)ss).getReadPairedFlag();
		}
		return true;
    }

}

