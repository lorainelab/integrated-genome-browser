package com.affymetrix.igb.das2;

import java.net.*;
import java.util.*;

public final class Das2Capability {
  String type;
  URI root_uri;
  Map formats;
  Das2VersionedSource version;

  public Das2Capability(String cap_type, URI cap_root, Map cap_formats) {
    type = cap_type;
    root_uri = cap_root;
    formats = cap_formats;
  }

  protected void setVersionedSource(Das2VersionedSource vsource) {
    version = vsource;
  }

  public Das2VersionedSource getVersionedSource() { return version; }
  public String getType() { return type; }
  public URI getRootURI() { return root_uri; }
  public Map getFormats() { return formats; }

}
