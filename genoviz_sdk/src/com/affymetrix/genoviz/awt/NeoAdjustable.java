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

package com.affymetrix.genoviz.awt;

import java.awt.Adjustable;

/**
 * Emulation of JDK1.1 java.awt.Adjustable to provide JDK1.0 compatible 
 * delegation-based event handling (pseudo-1.1 event handling).
 * NeoAdjustable is an interface for objects which have an adjustable 
 * numeric value contained within a bounded range of values.
 *
 * @deprecated use java.awt.Adjustable
 */
public interface NeoAdjustable extends Adjustable {
}    
