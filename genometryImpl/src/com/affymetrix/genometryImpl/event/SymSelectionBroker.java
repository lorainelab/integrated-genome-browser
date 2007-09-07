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

package com.affymetrix.genometryImpl.event;

import java.util.*;
import com.affymetrix.genometry.*;

public class SymSelectionBroker implements SymSelectionListener, SymSelectionSource   {

  List<SymSelectionListener> selection_clients = new Vector<SymSelectionListener>();

  public void symSelectionChanged(SymSelectionEvent evt) {
    for (int i=0; i<selection_clients.size(); i++) {
      selection_clients.get(i).symSelectionChanged(evt);
    }
  }

  public void addSymSelectionListener(SymSelectionListener listener) {
    selection_clients.add(listener);
  }

  public void removeSymSelectionListener(SymSelectionListener listener) {
    selection_clients.remove(listener);
  }

}
