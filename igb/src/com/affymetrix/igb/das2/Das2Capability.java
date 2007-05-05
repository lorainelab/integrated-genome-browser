package com.affymetrix.igb.das2;

import java.net.*;
import java.util.*;

public class Das2Capability {
  String type;
  URI root_uri;
  Map formats;

  public Das2Capability(String cap_type, URI cap_root, Map cap_formats) {
    type = cap_type;
    root_uri = cap_root;
    cap_formats = formats;
  }

  public String getType() { return type; }
  public URI getRootURI() { return root_uri; }
  public Map getFormats() { return formats; }

}
