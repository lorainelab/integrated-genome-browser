package com.affymetrix.genometry.filter;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.general.BoundedParameter;
import com.affymetrix.genometry.general.Parameter;
import com.affymetrix.genometry.operator.comparator.MathComparisonOperator;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.SymWithBaseQuality;
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
	private final static List<MathComparisonOperator> COMPARATOR_VALUES = new LinkedList<>();
	static {
		COMPARATOR_VALUES.add(new com.affymetrix.genometry.operator.comparator.GreaterThanEqualMathComparisonOperator());
		COMPARATOR_VALUES.add(new com.affymetrix.genometry.operator.comparator.GreaterThanMathComparisonOperator());
		COMPARATOR_VALUES.add(new com.affymetrix.genometry.operator.comparator.EqualMathComparisonOperator());
		COMPARATOR_VALUES.add(new com.affymetrix.genometry.operator.comparator.LessThanMathComparisonOperator());
		COMPARATOR_VALUES.add(new com.affymetrix.genometry.operator.comparator.LessThanEqualMathComparisonOperator());
		COMPARATOR_VALUES.add(new com.affymetrix.genometry.operator.comparator.NotEqualMathComparisonOperator());
	}
	
    private Parameter<Integer> averageQuality = new Parameter<>(DEFAULT_AVG_QUALITY_SCORE);
	private Parameter<MathComparisonOperator> comparator = new BoundedParameter<>(COMPARATOR_VALUES);
	
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
		return AVG_QUALITY_SCORE + " " + comparator.get().getDisplay() + " " + averageQuality.get();
	}
}
