package com.affymetrix.genometryImpl.event;

/**
 *  Interface for classes that want to know about changes in the 
 *  ID-to-Symmetry Map in IGB.
 */
public interface SymMapChangeListener {
	public void symMapModified(SymMapChangeEvent evt);  
}
