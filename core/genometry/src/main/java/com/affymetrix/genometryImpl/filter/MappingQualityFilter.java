package com.affymetrix.genometry.filter;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.general.BoundedParameter;
import com.affymetrix.genometry.general.Parameter;
import com.affymetrix.genometry.operator.comparator.MathComparisonOperator;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.symmetry.impl.BAMSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author hiralv
 */
public class MappingQualityFilter extends SymmetryFilter {
	private final static String QUALITY_SCORE = "quality_score";
	private final static String COMPARATOR = "comparator";
	private final static int DEFAULT_MIN_QUALITY_SCORE = 30;
	private final static List<MathComparisonOperator> COMPARATOR_VALUES = new LinkedList<>();
	static {
		COMPARATOR_VALUES.add(new com.affymetrix.genometry.operator.comparator.GreaterThanEqualMathComparisonOperator());
		COMPARATOR_VALUES.add(new com.affymetrix.genometry.operator.comparator.GreaterThanMathComparisonOperator());
		COMPARATOR_VALUES.add(new com.affymetrix.genometry.operator.comparator.EqualMathComparisonOperator());
		COMPARATOR_VALUES.add(new com.affymetrix.genometry.operator.comparator.LessThanMathComparisonOperator());
		COMPARATOR_VALUES.add(new com.affymetrix.genometry.operator.comparator.LessThanEqualMathComparisonOperator());
		COMPARATOR_VALUES.add(new com.affymetrix.genometry.operator.comparator.NotEqualMathComparisonOperator());
	}
	
    private Parameter<Integer> qualityScore = new Parameter<>(DEFAULT_MIN_QUALITY_SCORE);
	private Parameter<MathComparisonOperator> comparator = new BoundedParameter<>(COMPARATOR_VALUES);
	
	public MappingQualityFilter(){
		super();
		parameters.addParameter(QUALITY_SCORE, Integer.class, qualityScore);
		parameters.addParameter(COMPARATOR, MathComparisonOperator.class, comparator);
	}
	
	@Override
    public String getName() {
        return "mapping_quality_score";
    }
	
	@Override
	public boolean isFileTypeCategorySupported(FileTypeCategory fileTypeCategory){
		return fileTypeCategory == FileTypeCategory.Alignment;
	}
	
    @Override
    public boolean filterSymmetry(BioSeq bioseq, SeqSymmetry sym) {
		if(sym instanceof BAMSym) {
			int score = ((BAMSym) sym).getMapq();
			return score != BAMSym.NO_MAPQ && comparator.get().operate(score, qualityScore.get());
		}
		return false;
	}
	
	@Override
	public String getPrintableString() {
		return QUALITY_SCORE + " " + comparator.get().getDisplay() + " " + qualityScore.get();
	}
}
