package com.affymetrix.genometryImpl;

/**
 *
 * @author hiralv
 */
public interface SymWithResidues extends SymWithProps {

	public void setResidues(String residues);

	public String getResidues();

	public String getResidues(int start, int end);
}
