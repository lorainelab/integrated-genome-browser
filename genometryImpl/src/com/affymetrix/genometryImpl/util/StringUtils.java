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
package com.affymetrix.genometryImpl.util;

public class StringUtils {

	public static boolean isAllDigits(CharSequence cseq) {
		int char_count = cseq.length();
		boolean all_digits = true;
		for (int i=0; i<char_count; i++) {
			char ch = cseq.charAt(i);
			if (! Character.isDigit(ch)) {
				all_digits = false;
				break;
			}
		}
		return all_digits;
	}

}
