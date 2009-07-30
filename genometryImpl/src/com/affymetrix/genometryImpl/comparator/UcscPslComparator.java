/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
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

package com.affymetrix.genometryImpl.comparator;

import com.affymetrix.genometryImpl.*;
import com.affymetrix.genometry.SeqSymmetry;
import java.util.Comparator;

/**
 *  Sorts based on UcscPslSym.getTargetMin().
 */
public final class UcscPslComparator implements Comparator<UcscPslSym> {

	/** Sorts two instances of UcscPslSym based on UcscPslSym.getTargetMin() */
	public int compare(UcscPslSym sym1, UcscPslSym sym2) {
		if (sym1.getTargetMin() < sym2.getTargetMin()) {
			return -1;
		}
		else if (sym1.getTargetMin() > sym2.getTargetMin()) {
			return 1;
		}
		else { return 0; }
	}
}

