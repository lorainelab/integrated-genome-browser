package com.affymetrix.genometryImpl.symmetry;

import com.affymetrix.genometryImpl.BioSeq;

/**
 *
 * @author hiralv
 */
public class SimpleSymWithResidues extends UcscBedSym implements SymWithResidues  {

	private final String residues;
	
	public SimpleSymWithResidues(String type, BioSeq seq, int txMin, int txMax, String name, float score,
			boolean forward, int cdsMin, int cdsMax, int[] blockMins, int[] blockMaxs, String residues){
		super(type,seq,txMin,txMax,name,score,forward,cdsMin,cdsMax,blockMins,blockMaxs);
		this.residues = residues;
	}
	
	public String getResidues() {
		return residues;
	}

	public String getResidues(int start, int end) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
}
