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
import com.affymetrix.igb.genometry.SimpleSymWithProps;
import com.affymetrix.igb.genometry.TypedSym;

public class SimpleDas2Feature extends SimpleSymWithProps implements TypedSym  {

  String id;
  String type;  // eventually replace with Das2Type
  String name;
  String parent_id;
  String created;
  String modified;
  String doc_href;

  public SimpleDas2Feature(String feat_id, String feat_type, String feat_name, String feat_parent_id,
			   String feat_created, String feat_modified, String feat_doc_href, Map feat_props) {
    id = feat_id;
    type = feat_type;
    name = feat_name;
    parent_id = feat_parent_id;
    created = feat_created;
    modified = feat_modified;
    doc_href = feat_doc_href;
    setProperties(feat_props);   // feat_props should be null if no feature XML had no <PROP> elements
  }

  /** implementing TypedSym interface */
  public String getType() { return type; }
  public String getID() { return id; }
  public String getName() { return name; }

  public Object getProperty(String prop) {
    if (prop.equals("id")) { return id; }
    else if (prop.equals("name")) { return name; }
    else if (prop.equals("type")) { return type; }
    else if (prop.equals("link")) { return doc_href; }
    else { return super.getProperty(prop); }
  }

  public boolean setProperty(String tag, Object val) {
    if (tag == null)  { return false; }
    return super.setProperty(tag, val);
  }

  public Map cloneProperties() {
    Map cprops = super.cloneProperties();
    if (cprops == null) {
      cprops = new LinkedHashMap();
    }
    if (id != null)  { cprops.put("id", id); }  // should never be null though
    if (type != null)  { cprops.put("type", type); }  // shuold never be null though
    if (name != null)  { cprops.put("name", name); }  
    if (doc_href != null) { cprops.put("link", doc_href); }
    return cprops;
  }

  public Map getProperties() {
    return cloneProperties();
  }

}
