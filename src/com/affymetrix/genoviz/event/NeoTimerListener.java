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

package com.affymetrix.genoviz.event;

/**
 * Implementers can monitor NeoTimerEvents.
 * Classes that need to listen for NeoTimerEvents
 * must implement this interface and register with a NeoTimerEventClock.
 * @see com.affymetrix.genoviz.bioviews.NeoTimerEventClock
 */
public interface NeoTimerListener {
  public void heardTimerEvent(NeoTimerEvent evt);
}
