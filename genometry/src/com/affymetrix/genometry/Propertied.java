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

package com.affymetrix.genometry;

import java.util.*;

public interface Propertied {
	public Map<String,Object> getProperties();
	public Map<String,Object> cloneProperties();
	public Object getProperty(String key);
	public boolean setProperty(String key, Object val);
}
