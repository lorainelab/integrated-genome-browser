/**
 * Copyright (c) 2001-2006 Affymetrix, Inc.
 * 
* Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 * 
* The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.genometryImpl;

import java.util.ResourceBundle;

/**
 * Some global constants.
 */
public interface GenometryConstants {
	// note - translations should not be put here, they should be in the IGB project
    // only put translations here if there is no other option

    public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("genometry");
}
