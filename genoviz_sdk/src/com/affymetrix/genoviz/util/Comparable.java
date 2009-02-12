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

package com.affymetrix.genoviz.util;

/**
 * for comparing two objects of the same type
 * for sorting etc.
 */
public interface Comparable {
	/**
	 * compares this object with another.
	 *
	 * @param obj the object to which this object should be compared.
	 * @return &lt; 0 if this Compararble preceedes the argument
	 * ( "is less than" obj ),
	 * &gt; 0 if this compares follows the argument
	 * ( "is greater than" obj ),
	 * and 0 if the ordering can't be decided.
	 */
	int compare(Object obj);
}
