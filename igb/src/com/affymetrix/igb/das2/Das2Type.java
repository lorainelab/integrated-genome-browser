/**
*   Copyright (c) 2001-2005 Affymetrix, Inc.
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

import java.util.*;
import java.net.*;

public final class Das2Type {
  private Das2VersionedSource versioned_source;
  private URI type_uri;
  private String name;
  private String short_name;
  private Map<String, String> props;
  private Map<String,String> formats; // formats is a map of format names ("bed", "psl", etc.) to mime-type Strings

  public Das2Type(Das2VersionedSource version, URI type_uri, String name,
		  Map<String,String> formats, Map<String, String> props) {
    this.versioned_source = version;
    this.type_uri = type_uri;
    this.formats = formats;
    this.props = props;
    this.name = name;
    int sindex = name.lastIndexOf("/");
    if (sindex >= 0) { short_name = name.substring(sindex+1); }
    else { short_name = name; }
  }

  public Das2VersionedSource getVersionedSource() { return versioned_source; }
  public String getID() { return type_uri.toString(); }
  public String getName() { return name; }
  public String getShortName() { return short_name; }
  public String toString() {
    if (getName() == null) { return getID(); }
    else { return getName(); }
  }

  public Map<String, String> getProps() { return props; }
 
  Map<String,String> getFormats() { return formats; }
}
