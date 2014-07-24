package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.Scored;
import com.affymetrix.genometryImpl.general.BoundedParameter;
import com.affymetrix.genometryImpl.general.Parameter;
import com.affymetrix.genometryImpl.operator.comparator.MathComparisonOperator;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author hiralv
 */
public class ScoreFilter extends SymmetryFilter {
	private final static String SCORE = "score";
	private final static String COMPARATOR = "comparator";
	private final static int DEFAULT_SCORE = 10;
	private final static List<MathComparisonOperator> COMPARATOR_VALUES = new LinkedList<MathComparisonOperator>();
	static {
		COMPARATOR_VALUES.add(new com.affymetrix.genometryImpl.operator.comparator.GreaterThanEqualMathComparisonOperator());
		COMPARATOR_VALUES.add(new com.affymetrix.genometryImpl.operator.comparator.GreaterThanMathComparisonOperator());
		COMPARATOR_VALUES.add(new com.affymetrix.genometryImpl.operator.comparator.EqualMathComparisonOperator());
		COMPARATOR_VALUES.add(new com.affymetrix.genometryImpl.operator.comparator.LessThanMathComparisonOperator());
		COMPARATOR_VALUES.add(new com.affymetrix.genometryImpl.operator.comparator.LessThanEqualMathComparisonOperator());
		COMPARATOR_VALUES.add(new com.affymetrix.genometryImpl.operator.comparator.NotEqualMathComparisonOperator());
	}
	
    private Parameter<Integer> score = new Parameter<Integer>(DEFAULT_SCORE);
	private Parameter<MathComparisonOperator> comparator = new BoundedParameter<MathComparisonOperator>(COMPARATOR_VALUES);
	
	public ScoreFilter(){
		super();
		parameters.addParameter(SCORE, Integer.class, score);
		parameters.addParameter(COMPARATOR, MathComparisonOperator.class, comparator);
	}
	
	@Override
    public String getName() {
        return "score";
    }
	
    @Override
    public boolean filterSymmetry(BioSeq bioseq, SeqSymmetry sym) {
		if(sym instanceof Scored){
			float s = ((Scored)sym).getScore();
			return Float.compare(s, Scored.UNKNOWN_SCORE) != 0 && comparator.get().operate(s, (float)score.get());
		}
		return false;
	}
	
	@Override
	public String getPrintableString() {
		StringBuilder sb = new StringBuilder();
		sb.append(SCORE).append(" ").append(comparator.get().getDisplay()).append(" ").append(score.get());
		return sb.toString();
	}
}

