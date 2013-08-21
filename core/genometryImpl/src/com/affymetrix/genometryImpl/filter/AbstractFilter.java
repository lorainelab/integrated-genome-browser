package com.affymetrix.genometryImpl.filter;

/**
 *
 * @author hiralv
 */
public abstract class AbstractFilter implements SymmetryFilterI {
	
	@Override
	public SymmetryFilterI newInstance(){
		try {
			return getClass().getConstructor().newInstance();
		} catch (Exception ex) {
		}
		return null;
	}
}
