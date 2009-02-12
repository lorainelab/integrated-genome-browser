/**
 *   Copyright (c) 2007 Affymetrix, Inc.
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

package com.affymetrix.genometryImpl;

import java.util.Comparator;
import com.affymetrix.genometry.*;

/**
 *  Sorts SeqSymmetries based on lexicographic ordering of IDs 
 */
public class SeqSymIdComparator implements Comparator<SeqSymmetry> {

	public int compare(SeqSymmetry sym1, SeqSymmetry sym2) {
		String id1 = sym1.getID();
		String id2 = sym2.getID();
		if (id1 == null || id2 == null) {
			if (id1 == null && id2 == null) { return 0; }
			else if (id1 == null) { return 1; }
			else { return -1; }
		}
		else {
			return id1.compareTo(id2);
		}
	}

}
