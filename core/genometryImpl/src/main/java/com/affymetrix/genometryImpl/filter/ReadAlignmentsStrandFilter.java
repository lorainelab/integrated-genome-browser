/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

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
 * @author tkanapar
 */
public class ReadAlignmentsStrandFilter extends SymmetryFilter {
	private static enum READ_ALIGNMENT {
		POSITIVE("+ only"), NEGATIVE("- only");
		
		String name;
		READ_ALIGNMENT(String name){
			this.name = name;
		}
		
		@Override
		public String toString(){
			return name;
		}
	}
	private final static String COMPARATOR = "show";
	private final static List<READ_ALIGNMENT> COMPARATOR_VALUES = new LinkedList<READ_ALIGNMENT>();
	
	static {
		COMPARATOR_VALUES.add(READ_ALIGNMENT.POSITIVE);
		COMPARATOR_VALUES.add(READ_ALIGNMENT.NEGATIVE);
	}
		private Parameter<READ_ALIGNMENT> comparator = new BoundedParameter<READ_ALIGNMENT>(COMPARATOR_VALUES);
	
	public ReadAlignmentsStrandFilter(){
		parameters.addParameter(COMPARATOR, READ_ALIGNMENT.class, comparator);
	}
	@Override
    public String getName() {
        return "filter_by_strand";
    }
	
	@Override
	public boolean isFileTypeCategorySupported(FileTypeCategory fileTypeCategory){
		return fileTypeCategory == FileTypeCategory.Alignment;
	}
	
    @Override
    public boolean filterSymmetry(BioSeq bioseq, SeqSymmetry ss) {
		if(ss instanceof BAMSym ) {
			switch (comparator.get()) {
					case POSITIVE:
						return ss.getSpan(bioseq).isForward();

					case NEGATIVE:
						return !ss.getSpan(bioseq).isForward();
				}
		}
		return false;

	}
  
	
}
