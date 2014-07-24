
package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.general.Parameter;
import com.affymetrix.genometryImpl.symmetry.impl.GraphSym;
import com.affymetrix.genometryImpl.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.util.SeqUtils;

/**
 * Filters out symmetry that intersects with provided symmetry as parameter.
 * @author hiralv
 */
public class SymmetryFilterIntersecting extends SymmetryFilter{
	private final MutableSeqSymmetry dummySym = new SimpleMutableSeqSymmetry();
	
	private final static String SEQSYMMETRY = "seqsymmetry";
	private final static SeqSymmetry DEFAULT_SEQSYMMETRY = null;
	
    private Parameter<SeqSymmetry> original_sym = new Parameter<SeqSymmetry>(DEFAULT_SEQSYMMETRY);
			
	public SymmetryFilterIntersecting(){
		super();
		parameters.addParameter(SEQSYMMETRY, SeqSymmetry.class, original_sym);
	}
	
	public String getName() {
		return "existing";
	}

	@Override
	public String getDisplay() {
		return getName();
	}
	
	public boolean filterSymmetry(BioSeq seq, SeqSymmetry sym) {
		/**
		* Since GraphSym is only SeqSymmetry containing all points.
		* The intersection may find some points intersecting and
		* thus not add whole GraphSym at all. So if GraphSym is encountered
		* the it's not checked if it is intersecting. 
		*/
		if (sym instanceof GraphSym) {
			return true;
		}
		
		dummySym.clear();
		
		return !SeqUtils.intersection(sym, original_sym.get(), dummySym, seq);
	}
	
}
