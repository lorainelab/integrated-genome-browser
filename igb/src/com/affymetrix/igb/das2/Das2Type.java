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

public class Das2Type {
  Das2VersionedSource versioned_source;
  String id;
  String ontology;
  String derivation;  // in DAS2 XML, this is source attribute
  String info_url;
  Map props;
  Map formats; // formats is a map of format names ("bed", "psl", etc.) to mime-type Strings 

  public Das2Type(Das2VersionedSource version, String id, String ontology, String derivation, String href, Map formats, Map props) {
    this.versioned_source = version;
    this.id = id;
    this.ontology = ontology;
    this.derivation = derivation;
    this.info_url = href;
    this.formats = formats;
    this.props = props;
  }

  public Das2VersionedSource getVersionedSource() { return versioned_source; }
  public String getID() { return id; }
  public String getOntology() { return ontology; }
  public String getDerivation() { return derivation; }  // source attribute, but getSource() clashes with getVersionedSource();
  public String getInfoUrl() { return info_url; }
  public Map getProps() { return props; }
  public Map getFormats() { return formats; }
}
