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
public class Das2Result {
    
    Das2VersionedSource versioned_source;
    String id;
    String assayId;
    String imageId;
    String protocolId;
    
    /** Creates a new instance of Das2Assay */
    public Das2Result() {
        //nothing
    }
    
    public Das2Result(Das2VersionedSource version, String id, String assayId, String imageId, String protocolId) {
      this.versioned_source = version;
      this.id = id;
      this.assayId = assayId;
      this.imageId = imageId;
      this.protocolId = protocolId;
    }
    
    public Das2VersionedSource getVersionedSource() { return versioned_source; }
    public String getID() { return id; }
    public String getAssayId() { return assayId; }
    public String getImageId() { return imageId; }
    public String getProtocolId() { return protocolId; }
    
}
