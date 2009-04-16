/**
 *   Copyright (c) 2001-2004 Affymetrix, Inc.
 *    
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.  
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */

package com.affymetrix.genometry.seq;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.MutableBioSeq;
import com.affymetrix.genometry.util.DNAUtils;

public class SimpleBioSeq implements BioSeq, MutableBioSeq {

	protected String id;
	protected int length;
	protected String residues;

	/*public SimpleBioSeq() {
		length = 0;
	}*/

	public SimpleBioSeq(String id, int length) {
		this.id = id;
		this.length = length;
	}

	public SimpleBioSeq(String id, String residues) {
		this.id = id;
		this.residues = residues;
		this.length = residues.length();
	}

	/*public SimpleBioSeq(String id) {
		this.id = id;
		this.length = 0;
	}*/

	public String getID() { return id; }
	//  public int getMin() { return 0; }
	//  public int getMax() { return getLength(); }
	public int getLength() { return length; }
	public String getResidues() { 
		return residues; 
	}

	/**
	 *  Get residues.
	 *  Assumptions. Start at 0, end at start + length, e.g. 0, 1 is 1 character.
	 *
	 *  @return string containing characters between start and end inclusive. 
	 *          If end < start, the reverse complement string will be returned. 
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

	public boolean isComplete() { 
		return (residues != null);
	}
	public boolean isComplete(int start, int end) { 
		if (residues == null) { return false; }
		return (residues.length() >= end);
	}

	//public void setID(String id) { this.id = id; }

	public void setLength(int length) { 
		this.length = length; 
		// if length does not agree with length of residues, null out residues
		if ((residues != null) && (residues.length() != length)) {
			residues = null;
		}
	}

	public void setResidues(String residues) {
		this.residues = residues;
		this.length = residues.length();
	}

}
