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

package com.affymetrix.igb.das2.ontology;

import java.util.*;
import java.net.*;
import com.affymetrix.igb.das2.*;

public class Das2OntologyType extends Das2Type {
  /*Das2OntologyVersionedSource versioned_source;
  //  String id;
  URI type_uri;
  String name;
  String ontology;
  String derivation;  // in DAS2 XML, this is source attribute
  String info_url;    // doc_href
  Map props;
  Map formats; // formats is a map of format names ("bed", "psl", etc.) to mime-type Strings
   */
  Map parents; // the das2 server will change soon and parent/child relationships will be moved to another ontology namespace
  String termAccession;
  String termDef;

  //  public Das2Type(Das2VersionedSource version, String id, String ontology, String derivation, String href, Map formats, Map props, Map parents) {
  public Das2OntologyType(Das2OntologyVersionedSource version, URI type_uri, String name, String ontology,
		  String termAccession, String termDef, Map parents) {
    //super((Das2VersionedSource)version, type_uri, name, ontology,
		  //derivation, href, formats, props, parents);
    super((Das2VersionedSource)version, type_uri, name, ontology,
		  null, null, null, null, parents);
    this.parents = parents;
    this.termAccession = termAccession;
    this.termDef = termDef;
  }

  public Das2OntologyVersionedSource getVersionedSource() { return (Das2OntologyVersionedSource)versioned_source; }
  public URI getURI() { return type_uri; }
  public String getID() { return type_uri.toString(); }
  public String getName() { return name; }
  public String getOntology() { return ontology; }
  public String getDerivation() { return derivation; }  // source attribute, but getSource() clashes with getVersionedSource();
  public String getInfoUrl() { return info_url; }
  public Map getProps() { return props; }
  public Map getFormats() { return formats; }
  public Map getParents() { return parents; }
}
