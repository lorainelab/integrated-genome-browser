package com.affymetrix.genometry.filter;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.Scored;
import com.affymetrix.genometry.general.BoundedParameter;
import com.affymetrix.genometry.general.Parameter;
import com.affymetrix.genometry.operator.comparator.MathComparisonOperator;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
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
	private final static List<MathComparisonOperator> COMPARATOR_VALUES = new LinkedList<>();
	static {
		COMPARATOR_VALUES.add(new com.affymetrix.genometry.operator.comparator.GreaterThanEqualMathComparisonOperator());
		COMPARATOR_VALUES.add(new com.affymetrix.genometry.operator.comparator.GreaterThanMathComparisonOperator());
		COMPARATOR_VALUES.add(new com.affymetrix.genometry.operator.comparator.EqualMathComparisonOperator());
		COMPARATOR_VALUES.add(new com.affymetrix.genometry.operator.comparator.LessThanMathComparisonOperator());
		COMPARATOR_VALUES.add(new com.affymetrix.genometry.operator.comparator.LessThanEqualMathComparisonOperator());
		COMPARATOR_VALUES.add(new com.affymetrix.genometry.operator.comparator.NotEqualMathComparisonOperator());
	}
	
    private Parameter<Integer> score = new Parameter<>(DEFAULT_SCORE);
	private Parameter<MathComparisonOperator> comparator = new BoundedParameter<>(COMPARATOR_VALUES);
	
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
		return SCORE + " " + comparator.get().getDisplay() + " " + score.get();
	}
}

