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
  String id;
  Das2VersionedSource versioned_source;
  Map props;
  String ontology;
  String derivation;
  String info_url;

  public Das2Type(String id, Das2VersionedSource version) {
    this.id = id;
    this.versioned_source = version;
  }

  public String getID() { return id; }
  public Das2VersionedSource getVersionedSource() { return versioned_source; }
  public Map getProps() { return props; }
  public String getOntology() { return ontology; }
  public String getDerivation() { return derivation; }  // source attribute, but getSource() clashes with getVersionedSource();
  public String getInfoUrl() { return info_url; }
}
