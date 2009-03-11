/**
*   Copyright (c) 2005-2006 Affymetrix, Inc.
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
package com.affymetrix.igb.das2;

import java.net.*;
import java.util.*;

public final class Das2WritebackVersionedSource extends Das2VersionedSource  {

  static final String WRITEBACK_CAP_QUERY = "writeback";

  public Das2WritebackVersionedSource(Das2Source das_source, URI vers_uri, String name,
				      String href, String description, boolean init) {
    super(das_source, vers_uri, name, href, description, init);
  }

  public Das2WritebackVersionedSource(Das2Source das_source, URI vers_uri, URI coords_uri, String name,
				      String href, String description, boolean init) {
    super(das_source, vers_uri, coords_uri, name, href, description, init);
  }

  public boolean writeAnnotations(ArrayList annots) {
    Das2Capability writecap = getCapability(WRITEBACK_CAP_QUERY);
    String write_url = writecap.getRootURI().toString();
    return false;
  }

}
