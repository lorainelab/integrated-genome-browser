
package com.affymetrix.sequenceviewer;


/**
 *
 * @author vbishnoi
 */
public class SequenceViewerItems {
	private String residues= null;
	private String reverseResidues = null;
	Boolean isCDS = false;
	//0 would be the first for both front and reverse
	//the idea is to have cds start and end calcuated from start of a child i.e always relative to 0
	//from start of the child and for the reverse complement, it would be relative to end of a child
	private int cdsStart=-1, cdsEnd=-1, reverseCdsStart=-1, reverseCdsEnd=-1, type;
	public static enum TYPE {EXON, INTRON};

	public int getCdsEnd() {
		return cdsEnd;
	}

	public void setCdsEnd(int cdsEnd) {
		this.cdsEnd = cdsEnd;
	}

	public int getCdsStart() {
		return cdsStart;
	}

	public void setCdsStart(int cdsStart) {
		this.cdsStart = cdsStart;
	}

	public int getReverseCdsEnd() {
		return reverseCdsEnd;
	}

	public void setReverseCdsEnd(int reverseCdsEnd) {
		this.reverseCdsEnd = reverseCdsEnd;
	}

	public int getReverseCdsStart() {
		return reverseCdsStart;
	}

	public void setReverseCdsStart(int reverseCdsStart) {
		this.reverseCdsStart = reverseCdsStart;
	}

	public Boolean getIsCDS() {
		return isCDS;
	}

	public void setIsCDS(Boolean isCDS) {
		this.isCDS = isCDS;
	}

	public String getResidues() {
		return residues;
	}

	public void setResidues(String residues) {
		this.residues = residues;
	}

	public String getReverseResidues() {
		return reverseResidues;
	}

	public void setReverseResidues(String reverseResidues) {
		this.reverseResidues = reverseResidues;
	}

	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}
}
