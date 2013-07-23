
package com.affymetrix.genometryImpl.symmetry;

/**
 *
 * @author hiralv
 */
public class CdsSeqSymmetry extends SimpleMutableSeqSymmetry {
	
	SeqSymmetry property_sym;
	
	public SeqSymmetry getPropertySymmetry(){
		return property_sym;
	}
	
	public void setPropertySymmetry(SeqSymmetry sym){
		this.property_sym = sym;
	}
}
