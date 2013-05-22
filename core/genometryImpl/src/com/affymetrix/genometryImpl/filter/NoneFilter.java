package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

/**
 *
 * @author hiralv
 */
public class NoneFilter implements SymmetryFilterI {
	
	@Override
	public String getName(){
		return "None";
	}
	
	@Override
	public boolean setParam(Object param){
		return false;
	}
	
	@Override
	public Object getParam(){
		return null;
	}
	
	@Override
	public boolean filterSymmetry(BioSeq seq, SeqSymmetry sym){
		return false;
	}
}
