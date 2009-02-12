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

import com.affymetrix.genoviz.util.Debug;

import java.lang.String;
import java.lang.StringBuffer;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Vector;

/**
 * a data model for an annotated sequence.
 * It contains a sequence,
 * a set of identifiers,
 * descriptive text,
 * and a list (Vector) of features.
 * Since the sequence is observable (an Observable),
 * multiple observers can be notified of any changes.
 */
public class AnnotatedSequence extends Observable {

	private Vector<Identifier> identifiers = new Vector<Identifier>();
	private String description;
	private SequenceI sequence;
	private Vector<SeqFeatureI> features = new Vector<SeqFeatureI>();

	/**
	 * associates a name, synonym, or other identifier with this sequence.
	 *
	 * @param theId identifies the sequence.
	 * @see #identifiers
	 */
	public void addIdentifier(Identifier theId) {
		identifiers.addElement(theId);
	}

	/**
	 * convenience function to automatically create an identifier
	 * from a String.
	 */
	public void addIdentifier(String theId) {
		identifiers.addElement(new Identifier(theId));
	}

	/**
	 * gets identifiers of this sequence.
	 *
	 * @return (an Enumeration of) the identifiers.
	 * Each identifier is a <code>java.lang.String</code>.
	 * @see #addIdentifier
	 */
	public Enumeration identifiers() {
		return identifiers.elements();
	}

	/**
	 * attaches a description to the sequence.
	 *
	 * @param theDescription describes the sequence.
	 * @see #getDescription
	 */
	public void setDescription(String theDescription) {
		this.description = new String(theDescription);
	}
	/**
	 * gets the description set with setDescription.
	 *
	 * @return a description of the sequence.
	 * @see #setDescription
	 */
	public String getDescription() {
		if (null == this.description) return null;
		return new String(this.description);
	}

	/**
	 * sets the Sequence component to be annotated.
	 *
	 * @param theSequence to be identified, described, and annotated.
	 * @see #getSequence
	 */
	public void setSequence(SequenceI theSequence) {
		this.sequence = theSequence;
	}
	/**
	 * gets the Sequence component
	 * set with setSequence.
	 *
	 * @return the Sequence component.
	 * @see #setSequence
	 */
	public SequenceI getSequence() {
		return this.sequence;
	}

	/**
	 * adds a feature (annotation) to the sequence.
	 *
	 * @param theFeature of the sequence.
	 * @see #features
	 * @see #featureCount
	 */
	public void addFeature(SeqFeatureI theFeature) {
		features.addElement(theFeature);
	}
	/**
	 * can be used to iterate over the features
	 * of this sequence.
	 *
	 * @return an Enumeration of the features annotating this sequence.
	 * @see #addFeature
	 * @see #featureCount
	 */
	public Enumeration features() {
		return features.elements();
	}
	/**
	 * counts the features of this sequence.
	 *
	 * @return the number of features annotating this sequence.
	 * @see #addFeature
	 * @see #features
	 */
	public int featureCount() {
		return features.size();
	}

	/**
	 * @return a string representing this annotated sequence.
	 */
	public String toString() {

		StringBuffer sb = new StringBuffer("AnnotatedSequence:");

		Enumeration it = identifiers();
		while (it.hasMoreElements()) {
			Object o = it.nextElement();
			sb.append(" " + o);
		}
		sb.append("\n");

		if (null != this.description) {
			sb.append(this.description + "\n");
		}

		String residues = null;
		if (null != this.sequence) {
			residues = this.sequence.getResidues();
		}
		if (null != residues) {
			sb.append("residues: " + residues + "\n");
		}

		it = features.elements();
		if (null != it)
			while (it.hasMoreElements()) {
				Object o = it.nextElement();
				if (null != o)
					sb.append("\n" + o.toString());
			}

		return sb.toString();
	}

}
