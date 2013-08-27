package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.general.Parameter;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.BAMSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

/**
 *
 * @author hiralv
 */
public class MappingQualityFilter extends AbstractFilter {
	private final static String MIN_QUALITY_SCORE = "minimum_quality";
	private final static int DEFAULT_MIN_QUALITY_SCORE = 30;
	
    private Parameter<Integer> minimumQuality = new Parameter<Integer>(DEFAULT_MIN_QUALITY_SCORE);
	
	public MappingQualityFilter(){
		super();
		parameters.addParameter(MIN_QUALITY_SCORE, Integer.class, minimumQuality);
	}
	
	@Override
    public String getName() {
        return "Mapping Quality Score";
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
    public boolean filterSymmetry(BioSeq bioseq, SeqSymmetry sym) {
		if(sym instanceof BAMSym) {
			int score = ((BAMSym) sym).getMapq();
			return score != BAMSym.NO_MAPQ && score > minimumQuality.get();
		}
		return false;
	}
	
}
