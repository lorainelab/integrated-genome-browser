/**
*   Copyright (c) 2001-2006 Affymetrix, Inc.
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
  Map parents; // the das2 server will change soon and parent/child relationships will be moved to another ontology namespace
  String termAccession;
  String termDef;
  
  public Das2OntologyType(Das2OntologyVersionedSource version, URI type_uri, String name, String ontology,
      String termAccession, String termDef, Map parents) {
    super((Das2VersionedSource)version, type_uri, name, ontology,
      null, null, null, null, parents);
    this.parents = parents;
    this.termAccession = termAccession;
    this.termDef = termDef;
  }
  
  public Map getParents() {
    return(parents);
  }
}
