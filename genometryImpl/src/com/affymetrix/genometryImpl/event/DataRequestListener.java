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

package com.affymetrix.genometryImpl.event;

import java.util.EventListener;

public interface DataRequestListener extends EventListener {
  /**
   *  Event listener for DataRequestEvents for more data based on the current range along a sequence that is being viewed.
   *  Initially intended to be a user-initiated event based on current view in main IGB SeqMapView (SeqMapView is source).
   *  The idea is that components that support range-based partial loading of annotation tracks can listen 
   *     for DataRequestEvents and load data based on the current view (and probably what has already been loaded).
   *  Initial listeners: Das2LoadView (DAS/2 annotations), TdbGraphLoadView (graph slices), LoadFileAction (graph slices)
   *     [note that all of these initial listeners end up feeding into calls to DAS/2 servers, to try and avoid
   *        reimplementing range-based data loading and optimizations].
   *
   * @param evt
   * @return  if returns false then guaranteed that no new data added to genometry models
   *  if return true then it's possible that new data was added (but not guaranteed)
   * 
   */
  public boolean dataRequested(DataRequestEvent evt);

}
