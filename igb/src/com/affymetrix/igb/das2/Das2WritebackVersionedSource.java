package com.affymetrix.igb.das2;

import java.net.*;
import java.util.*;

public class Das2WritebackVersionedSource extends Das2VersionedSource  {

  static String WRITEBACK_CAP_QUERY = "writeback";

  public Das2WritebackVersionedSource(Das2Source das_source, URI vers_uri, String name,
				      String href, String description, boolean init) {
    super(das_source, vers_uri, name, href, description, init);
  }

  public boolean writeAnnotations(ArrayList annots) {
    Das2Capability writecap = (Das2Capability)getCapability(WRITEBACK_CAP_QUERY);
    String write_url = writecap.getRootURI().toString();
    return false;
  }

}
