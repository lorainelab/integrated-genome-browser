package com.affymetrix.genometry.filter;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.symmetry.impl.BAMSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;

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
    public boolean isFileTypeCategorySupported(FileTypeCategory fileTypeCategory) {
        return fileTypeCategory == FileTypeCategory.Alignment;
    }

    @Override
    public boolean filterSymmetry(BioSeq bioseq, SeqSymmetry ss) {
        if (!(ss instanceof BAMSym)) {
            return false;
        }
        if ((((BAMSym) ss).getProperty("NH")) == null) {
            return false;
        }
        int currentNH = (Integer) (((BAMSym) ss).getProperty("NH"));
        if (currentNH <= 1) {
            return false;
        }
        return true;
    }

}
