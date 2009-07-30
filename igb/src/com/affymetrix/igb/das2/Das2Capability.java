package com.affymetrix.igb.das2;

import java.net.*;
import java.util.*;

public final class Das2Capability {
  String type;
  URI root_uri;
  Map formats;
  Das2VersionedSource version;
	static Map<String,Das2VersionedSource> cap2version = new LinkedHashMap<String,Das2VersionedSource>();

  public Das2Capability(String cap_type, URI cap_root, Map cap_formats) {
    type = cap_type;
    root_uri = cap_root;
    formats = cap_formats;
  }

  protected void setVersionedSource(Das2VersionedSource vsource) {
    version = vsource;
  }

	public static Map<String,Das2VersionedSource> getCapabilityMap() { return cap2version; }
  public Das2VersionedSource getVersionedSource() { return version; }
  public String getType() { return type; }
  public URI getRootURI() { return root_uri; }
  public Map getFormats() { return formats; }

}
