package com.affymetrix.genometry.seq;

import java.util.*;

import com.affymetrix.genometry.util.DNAUtils;
import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.genometry.SeqSymmetry;

public final class SimpleAnnotatedBioSeq
	implements MutableAnnotatedBioSeq {

	protected List<SeqSymmetry> annots;
	protected String id;
	protected int length;
	protected String residues;

	public SimpleAnnotatedBioSeq(String id, String residues) {
		this.id = id;
		this.setResidues(residues);
	}

	public SimpleAnnotatedBioSeq(String id, int length)  {
		this.id = id;
		this.length = length;
	}

	public String getID() { return id; }
	public int getLength() { return length; }
	public String getResidues() {
		return residues;
	}

	public void addAnnotation(SeqSymmetry annot) {
		if (null == annots) { annots = new ArrayList<SeqSymmetry>(); }
		annots.add(annot);
	}

	public void removeAnnotation(SeqSymmetry annot) {
		if (null != annots) {
			annots.remove(annot);
		}
	}

	public int getAnnotationCount() {
		if (null != annots) {
			return annots.size();
		}
		else return 0;
	}

	public SeqSymmetry getAnnotation(int index) {
		if (null != annots && index < annots.size()) {
			return annots.get(index);
		}
		else {
			return null;
		}
	}

	//public void setID(String id) { this.id = id; }
	public void setLength(int length) {
		this.length = length;
		// if length does not agree with length of residues, null out residues
		if ((residues != null) && (residues.length() != length)) {
			residues = null;
		}
	}

	public boolean isComplete() {
		return residues != null;
	}

	public boolean isComplete(int start, int end) {
		if (residues == null) {
			return false;
		}
		return residues.length() >= end;
	}

	public void setResidues(String residues) {
		this.residues = residues;
		this.length = residues.length();
	}

	/**
	 * Get residues.
	 * Assumptions. Start at 0, end at start + length, e.g. 0, 1 is 1 character.
	 *
	 * @return string containing characters between start and end inclusive.
	 * If end < start, the reverse complement string will be returned.
	 */
	public String getResidues(int start, int end) {
		if (residues == null) {
			return null;
		}
		int residue_length = this.getLength();
		if (start < 0 || residue_length <= 0) {
			return null;
		}
		// Sanity checks on argument size.
		start = Math.min(start, residue_length);
		end = Math.min(end, residue_length);
		if (start <= end) {
			end = Math.min(end, start + residue_length);
		} else {
			start = Math.min(start, end + residue_length);
		}
		if (start <= end) {
			return residues.substring(start, end);
		}
		return DNAUtils.reverseComplement(residues.substring(end, start));
	}
}
