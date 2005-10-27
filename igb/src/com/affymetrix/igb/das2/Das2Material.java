/*
 * Das2Assay.java
 *
 * Created on September 15, 2005, 2:03 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.affymetrix.igb.das2;

import java.util.*;

/**
 *
 * @author boconnor
 */
public class Das2Material {
    
    Das2VersionedSource versioned_source;
    String id;
    String name;
    Map types;
    Map contacts;
    
    /** Creates a new instance of Das2Assay */
    public Das2Material() {
        //nothing
    }
    
    public Das2Material(Das2VersionedSource version, String id, String name, Map types, Map contacts) {
      this.versioned_source = version;
      this.id = id;
      this.name = name;
      this.types = types;
      this.contacts = contacts;
    }
    
    public Das2VersionedSource getVersionedSource() { return versioned_source; }
    public String getID() { return id; }
    public Map getTypes() { return types; }
    public Map getContacts() { return contacts; }
    
}
