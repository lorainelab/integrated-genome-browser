/**
 *   Copyright (c) 1998-2005 Affymetrix, Inc.
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

package com.affymetrix.genoviz.datamodel;

/**
 * represents a feature on a sequence of Nucleic Acids.
 * So it has a notion of which strand it is on.
 */
public class NASeqFeature extends AdHocSeqFeature  {

	private int strand;
	/**
	 * indicates that it is not known (or unimportant)
	 * on which strand the feature lies.
	 */
	public static final int UNKNOWN = 0;
	/** indicates that the feature lies on the forward strand. */
	public static final int FORWARD = 1;
	/**
	 * indicates that the feature lies on the reverse strand
	 * and is associated with the reverse compliment
	 * of the sequence.
	 */
	public static final int REVERSE = 2;

	/**
	 * constructs a feature on an unknown (at first) strand.
	 */
	public NASeqFeature(String theType) {
		super(theType);
		setStrand(UNKNOWN);
	}

	/**
	 * @return a string representation of this feature.
	 */
	public String toString() {
		switch (this.strand) {
			case FORWARD:
				return super.toString() + "\nStrand: forward";
			case REVERSE:
				return super.toString() + "\nStrand: reverse";
		}
		return super.toString();
	}

	/**
	 * sets the strand on which this feature lies.
	 *
	 * @param theStrand must be one of UNKNOWN, FORWARD, or REVERSE.
	 */
	public void setStrand(int theStrand) {
		switch (theStrand) {
			case UNKNOWN:
			case FORWARD:
			case REVERSE:
				this.strand = theStrand;
				break;
			default:
				throw new IllegalArgumentException(
						"Strand must be UNKNOWN, FORWARD, or REVERSE.");
		}
	}
	public int getStrand() {
		return this.strand;
	}

}
