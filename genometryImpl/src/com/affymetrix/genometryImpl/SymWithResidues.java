package com.affymetrix.genometryImpl;

/**
 *
 * @author hiralv
 */
public interface SymWithResidues {

	public void setResidues(String residues);

	public String getResidues();

	public String getResidues(int start, int end);
}
