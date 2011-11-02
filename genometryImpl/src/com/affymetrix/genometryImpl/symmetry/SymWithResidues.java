package com.affymetrix.genometryImpl.symmetry;

/**
 *
 * @author hiralv
 */
public interface SymWithResidues extends SymWithProps {

	public String getResidues();

	public String getResidues(int start, int end);
}
