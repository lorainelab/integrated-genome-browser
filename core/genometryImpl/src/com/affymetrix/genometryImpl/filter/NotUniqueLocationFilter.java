package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.BAMSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

/**
 *
 * @author hiralv
 */
public class NotUniqueLocationFilter extends SymmetryFilter {

    @Override
    public String getName() {
        return "multi_mapper";
    }
	
	@Override
	public boolean isFileTypeCategorySupported(FileTypeCategory fileTypeCategory){
		return fileTypeCategory == FileTypeCategory.Alignment;
	}
	
    @Override
    public boolean filterSymmetry(BioSeq bioseq, SeqSymmetry ss) {
        if (!(ss instanceof BAMSym)) {
            return false;
        }
		if((((BAMSym)ss).getProperty("NH")) == null){
			return false;
		}
		int currentNH = (Integer)(((BAMSym)ss).getProperty("NH"));
        if(currentNH <= 1) {
			return false;
		}
        return true;
    }
    
}

