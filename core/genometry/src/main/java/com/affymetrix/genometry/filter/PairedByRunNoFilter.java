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
public class PairedByRunNoFilter extends SymmetryFilter {
	private static enum RUN_NO {
		FIRST("First"), SECOND("Second");
		
		String name;
		RUN_NO(String name){
			this.name = name;
		}
		
		@Override
		public String toString(){
			return name;
		}
	}
	
	private final static String COMPARATOR = "run_number";
	private final static List<RUN_NO> COMPARATOR_VALUES = new LinkedList<>();
	
	static {
		COMPARATOR_VALUES.add(RUN_NO.FIRST);
		COMPARATOR_VALUES.add(RUN_NO.SECOND);
	}
	private Parameter<RUN_NO> comparator = new BoundedParameter<>(COMPARATOR_VALUES);
	
	public PairedByRunNoFilter(){
		parameters.addParameter(COMPARATOR, RUN_NO.class, comparator);
	}
	
	@Override
    public String getName() {
        return "paired_run_no";
    }
	
	@Override
	public boolean isFileTypeCategorySupported(FileTypeCategory fileTypeCategory){
		return fileTypeCategory == FileTypeCategory.Alignment;
	}
	
    @Override
   public boolean filterSymmetry(BioSeq bioseq, SeqSymmetry ss) {
		if (ss instanceof BAMSym) {
			if (((BAMSym) ss).getReadPairedFlag()) {
				switch (comparator.get()) {
					case FIRST:
						return ((BAMSym) ss).getFirstOfPairFlag();

					case SECOND:
						return ((BAMSym) ss).getSecondOfPairFlag();
				}
			}
		}
		return false;
	}

}

