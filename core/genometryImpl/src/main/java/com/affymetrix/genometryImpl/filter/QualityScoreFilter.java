package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.general.BoundedParameter;
import com.affymetrix.genometryImpl.general.Parameter;
import com.affymetrix.genometryImpl.operator.comparator.MathComparisonOperator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithBaseQuality;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author hiralv
 */
public class QualityScoreFilter extends SymmetryFilter {
	private final static String AVG_QUALITY_SCORE = "average_quality";
	private final static String COMPARATOR = "comparator";
	private final static int DEFAULT_AVG_QUALITY_SCORE = 30;
	private final static List<MathComparisonOperator> COMPARATOR_VALUES = new LinkedList<MathComparisonOperator>();
	static {
		COMPARATOR_VALUES.add(new com.affymetrix.genometryImpl.operator.comparator.GreaterThanEqualMathComparisonOperator());
		COMPARATOR_VALUES.add(new com.affymetrix.genometryImpl.operator.comparator.GreaterThanMathComparisonOperator());
		COMPARATOR_VALUES.add(new com.affymetrix.genometryImpl.operator.comparator.EqualMathComparisonOperator());
		COMPARATOR_VALUES.add(new com.affymetrix.genometryImpl.operator.comparator.LessThanMathComparisonOperator());
		COMPARATOR_VALUES.add(new com.affymetrix.genometryImpl.operator.comparator.LessThanEqualMathComparisonOperator());
		COMPARATOR_VALUES.add(new com.affymetrix.genometryImpl.operator.comparator.NotEqualMathComparisonOperator());
	}
	
    private Parameter<Integer> averageQuality = new Parameter<Integer>(DEFAULT_AVG_QUALITY_SCORE);
	private Parameter<MathComparisonOperator> comparator = new BoundedParameter<MathComparisonOperator>(COMPARATOR_VALUES);
	
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
