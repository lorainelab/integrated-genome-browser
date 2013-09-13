package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.general.Parameter;
import com.affymetrix.genometryImpl.operator.comparator.GreaterThanEqualMathComparisonOperator;
import com.affymetrix.genometryImpl.operator.comparator.MathComparisonOperator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithBaseQuality;

/**
 *
 * @author hiralv
 */
public class QualityScoreFilter extends SymmetryFilter {
	private final static String AVG_QUALITY_SCORE = "average_quality";
	private final static String COMPARATOR = "comparator";
	private final static int DEFAULT_AVG_QUALITY_SCORE = 30;
	private final static MathComparisonOperator DEFAULT_COMPARATOR = new GreaterThanEqualMathComparisonOperator();
	
    private Parameter<Integer> averageQuality = new Parameter<Integer>(DEFAULT_AVG_QUALITY_SCORE);
	private Parameter<MathComparisonOperator> comparator = new Parameter<MathComparisonOperator>(DEFAULT_COMPARATOR);
	
	public QualityScoreFilter(){
		super();
		parameters.addParameter(AVG_QUALITY_SCORE, Integer.class, averageQuality);
		parameters.addParameter(COMPARATOR, MathComparisonOperator.class, comparator);
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
			int score = ((SymWithBaseQuality)ss).getAverageQuality();
			return comparator.get().operate(score, averageQuality.get());
		}
		return false;
    }
	
	@Override
	public String getPrintableString() {
		StringBuilder sb = new StringBuilder();
		sb.append(AVG_QUALITY_SCORE).append(" ").append(comparator.get().getDisplay()).append(" ").append(averageQuality.get());
		return sb.toString();
	}
}
