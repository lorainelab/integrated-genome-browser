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

import java.util.Enumeration;

/**
 * all the things that can be done with something
 * representing a feature of a biological sequence.
 * Such features can be used to annotate a sequence.
 */
public interface SeqFeatureI extends TypedI {

	/**
	 * associates a contiguous section of the sequence with this feature.
	 *
	 * @param thePiece the contiguous section of sequence.
	 */
	public void addPiece(Range thePiece);

	/**
	 * can be used to iterate over all the pieces of this feature.
	 *
	 * @return an enumeration of the pieces.
	 * Each piece is a <code>Range</code>
	 * @see Range
	 */
	public Enumeration pieces();

	/**
	 * associates a named attribute with this feature.
	 *
	 * @param theName a name for the attribute.
	 * @param theValue a value for the attribute.
	 */
	public void addAttribute(String theName, String theValue);

	/**
	 * replaces one named attribute with another.
	 *
	 * @param theName names the attribute being replaced.
	 * @param theValue gives the new value.
	 */
	public void replaceAttribute(String theName, String theValue);

	/**
	 * gets a named attribute of this feature.
	 *
	 * @param theName names the attribute requested.
	 * @return the value of the named attribute.
	 */
	public Object getAttribute(String theName);

	/**
	 * can be used to iterate over all the attributes of a feature.
	 * It returns the names of the attributes.
	 * You would then call getAttribute with each name enumerated.
	 *
	 * @return an enumeration of the names of the attributes
	 *         of this feature.
	 * Each name is a <code>java.lang.String</code>.
	 */
	public Enumeration attributes();


}
