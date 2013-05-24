/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.filter.SymmetryFilterI;
import com.affymetrix.genometryImpl.symmetry.BAMSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SymWithProps;

/**
 *
 * @author Anuj
 * 
 * This class is used to filter out the non-unique BAM symmetries
 * 
 */
public class UniqueLocationFilter implements SymmetryFilterI{

    @Override
    public String getName() {
        return null;
    }

	@Override
	public String getDisplay() {
		return getName();
	}
	
    @Override
    public boolean setParam(Object o) {
        return false;
    }

    @Override
    public Object getParam() {
        return null;
    }

    @Override
    public boolean filterSymmetry(BioSeq bioseq, SeqSymmetry ss) {
        if (!(ss instanceof BAMSym)) {
            return false;
        }
		if((((BAMSym)ss).getProperty("NH")) == null){
			return false;
		}
		int currentNH = (Integer)(((BAMSym)ss).getProperty("NH"));
        if(currentNH != 1)
            return false;
        return true;
    }
    
}
