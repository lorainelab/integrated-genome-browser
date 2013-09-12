package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.Scored;
import com.affymetrix.genometryImpl.general.Parameter;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

/**
 *
 * @author hiralv
 */
public class ScoreFilter extends SymmetryFilter {
	private final static String SCORE = "score";
	private final static int DEFAULT_SCORE = 10;
	
    private Parameter<Integer> score = new Parameter<Integer>(DEFAULT_SCORE);
	
	public ScoreFilter(){
		super();
		parameters.addParameter(SCORE, Integer.class, score);
	}
	
	@Override
    public String getName() {
        return "score";
    }
	
    @Override
    public boolean filterSymmetry(BioSeq bioseq, SeqSymmetry sym) {
		if(sym instanceof Scored){
			float s = ((Scored)sym).getScore();
			return Float.compare(s, Scored.UNKNOWN_SCORE) != 0 && s >= score.get();
		}
		return false;
	}
	
	@Override
	public String getPrintableString() {
		StringBuilder sb = new StringBuilder();
		sb.append(SCORE).append(" >= ").append(score.get());
		return sb.toString();
	}
}

