package com.affymetrix.genometryImpl.symmetry;

import java.util.BitSet;

/**
 *
 * @author hiralv
 */
public interface SymWithResidues extends SymWithProps {

	public String getResidues();

	public String getResidues(int start, int end);
	
	public BitSet getResidueMask();
	
	public void setResidueMask(BitSet bitset);
}
