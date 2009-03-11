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
  protected Das2VersionedSource versioned_source;
  protected URI type_uri;
  protected String name;
  protected String short_name;
  protected String ontology;
  protected String derivation;  // in DAS2 XML, this is source attribute
  protected String info_url;    // doc_href
  protected Map props;
  protected Map formats; // formats is a map of format names ("bed", "psl", etc.) to mime-type Strings

  //  public Das2Type(Das2VersionedSource version, String id, String ontology, String derivation, String href, Map formats, Map props, Map parents) {
  public Das2Type(Das2VersionedSource version, URI type_uri, String name, String ontology,
		  String derivation, String href, Map formats, Map props, Map parents) {
    this.versioned_source = version;
    //    this.id = id;
    this.type_uri = type_uri;
    this.ontology = ontology;
    this.derivation = derivation;
    this.info_url = href;
    this.formats = formats;
    this.props = props;
    this.name = name;
    int sindex = name.lastIndexOf("/");
    if (sindex >= 0) { short_name = name.substring(sindex+1); }
    else { short_name = name; }
    //    this.parents = parents;
  }

  public Das2VersionedSource getVersionedSource() { return versioned_source; }
  public URI getURI() { return type_uri; }
  public String getID() { return type_uri.toString(); }
  public String getName() { return name; }
  public String getShortName() { return short_name; }
  public String toString() {
    if (getName() == null) { return getID(); }
    else { return getName(); }
  }
  public String getOntology() { return ontology; }
  public String getDerivation() { return derivation; }  // source attribute, but getSource() clashes with getVersionedSource();
  public String getInfoUrl() { return info_url; }
  public Map getProps() { return props; }
  public String getProperty(String key) { 
    String val = null;
    if (props != null) { val = (String)props.get(key); }
    return val;
  }
  public Map getFormats() { return formats; }
  //  public Map getParents() { return parents; }
}
