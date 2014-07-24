package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.impl.BAMSym;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SymWithResidues;
import java.util.BitSet;

/**
 *
 * @author hiralv
 */
public class MismatchFilter extends SymmetryFilter {
	
	@Override
    public String getName() {
        return "mismatch";
    }
	
	@Override
	public boolean isFileTypeCategorySupported(FileTypeCategory fileTypeCategory){
		return fileTypeCategory == FileTypeCategory.Alignment;
	}
	
    @Override
    public boolean filterSymmetry(BioSeq bioseq, SeqSymmetry ss) {
		//If insertion is present then include in mismatch.
		if(ss instanceof BAMSym && ((BAMSym)ss).getInsChildCount() > 0) {
			return true;
		}
	
		int child_count = ss.getChildCount();
		if (child_count > 0) {
			SeqSymmetry child;
			for (int i = 0; i < child_count; i++) {
				child = ss.getChild(i);
				if (child instanceof SymWithResidues) {
					if(filter((SymWithResidues)child)){
						return true;
					}
				}
			}
		} else if (ss instanceof SymWithResidues) {
			return filter((SymWithResidues)ss);
		}
		
		return false;
    }
  
	private boolean filter(SymWithResidues swr) {
		BitSet bitSet = swr.getResidueMask();
		if (bitSet != null && bitSet.cardinality() > 0) {
			return true;
		}
		return false;
	}
}