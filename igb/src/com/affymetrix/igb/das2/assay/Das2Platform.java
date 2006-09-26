/*
 * Das2Platform.java
 *
 * Created on October 10, 2005, 3:56 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.affymetrix.igb.das2.assay;

import com.affymetrix.igb.das2.*;

/**
 *
 * @author Marc Carlson Need a small object to hold the platform information and
 * even though there is only one tiny string inside this right now, I am betting
 * that there will be more...
 */

public class Das2Platform {
    Das2AssayVersionedSource versioned_source;
    String platform;

    /** Creates a new instance of Das2Platform */
    public Das2Platform(Das2AssayVersionedSource _version, String _platform) {
        this.versioned_source = _version;
        this.platform = _platform;
    }

    public Das2AssayVersionedSource getVersionedSource() { return versioned_source; }
    public String getPlatform(){ return platform; }
}
