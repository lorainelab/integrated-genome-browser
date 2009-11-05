package com.affymetrix.igb.das2;

import java.net.*;
import java.util.*;

public final class Das2Capability {
  private String type;
  private URI root_uri;
  private static final Map<String,Das2VersionedSource> cap2version = new LinkedHashMap<String,Das2VersionedSource>();

  public Das2Capability(String cap_type, URI cap_root) {
    type = cap_type;
    root_uri = cap_root;
  }

  public static Map<String,Das2VersionedSource> getCapabilityMap() { return cap2version; }
  public String getType() { return type; }
  public URI getRootURI() { return root_uri; }

}
