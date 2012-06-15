/*  Licensed under the Common Public License, Version 1.0 (the "License").
 *  A copy of the license must be included
 *  with any distribution of this source code.
 *  Distributions from Genentech, Inc. place this in the IGB_LICENSE.html file.
 * 
 *  The license is also available at
 *  http://www.opensource.org/licenses/CPL
 */
package com.affymetrix.genoviz.swing;

/**
 * A three valued logic variable
 * to support the inclusion of action icons in a tool bar.
 * Some actions are not appropriate for a tool bar.
 * Others can be set to is or is not to appear in the tool bar.
 * @author Eric Blossom
 */
public enum ExistentialTriad {
	IS,
	ISNOT,
	CANNOTBE;

	public Boolean booleanValue() {
		return this == IS;
	}

	public static ExistentialTriad valueOf(Boolean b) {
		if (b) {
			return IS;
		}
		return ISNOT;
	}

}
