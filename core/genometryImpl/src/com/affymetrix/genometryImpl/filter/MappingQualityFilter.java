package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.general.Parameter;
import com.affymetrix.genometryImpl.operator.comparator.GreaterThanEqualMathComparisonOperator;
import com.affymetrix.genometryImpl.operator.comparator.MathComparisonOperator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.BAMSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

/**
 *
 * @author hiralv
 */
public class MappingQualityFilter extends SymmetryFilter {
	private final static String QUALITY_SCORE = "quality_score";
	private final static String COMPARATOR = "comparator";
	private final static int DEFAULT_MIN_QUALITY_SCORE = 30;
	private final static MathComparisonOperator DEFAULT_COMPARATOR = new GreaterThanEqualMathComparisonOperator();
	
    private Parameter<Integer> qualityScore = new Parameter<Integer>(DEFAULT_MIN_QUALITY_SCORE);
	private Parameter<MathComparisonOperator> comparator = new Parameter<MathComparisonOperator>(DEFAULT_COMPARATOR);
	
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
		StringBuilder sb = new StringBuilder();
		sb.append(QUALITY_SCORE).append(" ").append(comparator.get().getSymbol()).append(" ").append(qualityScore.get());
		return sb.toString();
	}
}
