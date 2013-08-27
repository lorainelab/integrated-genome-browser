package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithResidues;
import java.util.BitSet;

/**
 *
 * @author hiralv
 */
public class MismatchFilter extends AbstractFilter {
	
	@Override
    public String getName() {
        return "Mismatch";
    }

	@Override
	public String getDisplay() {
		return getName();
	}
	
	@Override
	public boolean isFileTypeCategorySupported(FileTypeCategory fileTypeCategory){
		return fileTypeCategory == FileTypeCategory.Alignment;
	}
	
    @Override
    public boolean filterSymmetry(BioSeq bioseq, SeqSymmetry ss) {
		boolean filter = true;
		int child_count = ss.getChildCount();
		if (child_count > 0) {
			SeqSymmetry child;
			for (int i = 0; i < child_count; i++) {
				child = ss.getChild(i);
				if (child instanceof SymWithResidues) {
					filter &= filter((SymWithResidues)child);
				}
			}
		} else if (ss instanceof SymWithResidues) {
			filter &= filter((SymWithResidues)ss);
		}
		return filter;
    }
  
	private boolean filter(SymWithResidues swr) {
		BitSet bitSet = swr.getResidueMask();
		if (bitSet != null && bitSet.cardinality() > 0) {
			return false;
		}
		return true;
	}
}