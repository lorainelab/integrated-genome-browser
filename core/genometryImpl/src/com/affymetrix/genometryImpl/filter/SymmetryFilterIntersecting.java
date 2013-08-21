
package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.util.SeqUtils;

/**
 * Filters out symmetry that intersects with provided symmetry as parameter.
 * @author hiralv
 */
public class SymmetryFilterIntersecting extends AbstractFilter{
	private Object param;
	private SeqSymmetry original_sym;
	private final MutableSeqSymmetry dummySym = new SimpleMutableSeqSymmetry();
	
	public String getName() {
		return "existing";
	}

	@Override
	public String getDisplay() {
		return getName();
	}
	
	public boolean setParam(Object param) {
		this.param = param;
		if(!(param instanceof SeqSymmetry)){
			return false;
		}
		
		original_sym = (SeqSymmetry)param;
		return true;
	}

	public Object getParam() {
		return param;
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
		
		return !SeqUtils.intersection(sym, original_sym, dummySym, seq);
	}
	
}
