/*
 * Das2Assay.java
 *
 * Created on September 15, 2005, 2:03 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.affymetrix.igb.das2.assay;

import java.util.*;
import com.affymetrix.igb.das2.*;

/**
 *
 * @author boconnor
 */
public class Das2Assay {

    Das2VersionedSource versioned_source;
    String id;
    Map images;
    Map biomaterials;
    Map platforms;

    /** Creates a new instance of Das2Assay */
    public Das2Assay() {
        //nothing
    }

    public Das2Assay(Das2VersionedSource version, String id, Map images, Map biomaterials, Map platforms) {
      this.versioned_source = version;
      this.id = id;
      this.images = images;
      this.biomaterials = biomaterials;
      this.platforms = platforms;
    }

    public Das2VersionedSource getVersionedSource() { return versioned_source; }
    public String getID() { return id; }
    public Map getImages() { return images; }
    public Map getBiomaterials() { return biomaterials; }
    public Map getPlatforms() { return platforms; }

}
