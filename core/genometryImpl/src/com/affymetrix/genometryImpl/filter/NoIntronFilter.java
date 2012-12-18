/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

/**
 * This is a filter which is used to filter out the symmetries with no children
 * @author Anuj
 */
public class NoIntronFilter implements SymmetryFilterI{

    @Override
    public String getName() {
        return null;
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
		if(ss.getChildCount() <= 1)
            return false;
        return true;
    }
    
}
