package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.general.Parameter;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithBaseQuality;

/**
 *
 * @author hiralv
 */
public class QualityScoreFilter extends AbstractFilter {
	private final static String AVG_QUALITY_SCORE = "average_quality";
	private final static int DEFAULT_AVG_QUALITY_SCORE = 30;
	
    private Parameter<Integer> averageQuality = new Parameter<Integer>(DEFAULT_AVG_QUALITY_SCORE);
	
	public QualityScoreFilter(){
		super();
		parameters.addParameter(AVG_QUALITY_SCORE, Integer.class, averageQuality);
	}
	
	@Override
    public String getName() {
        return "quality_score";
    }
	
	@Override
	public boolean isFileTypeCategorySupported(FileTypeCategory fileTypeCategory){
		return fileTypeCategory == FileTypeCategory.Alignment;
	}
	
    @Override
    public boolean filterSymmetry(BioSeq bioseq, SeqSymmetry ss) {
		if (ss instanceof SymWithBaseQuality) {
			return ((SymWithBaseQuality)ss).getAverageQuality() > averageQuality.get();
		}
		return false;
    }
}
