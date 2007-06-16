/*
 * Das2AssaySource.java
 *
 * Created on November 8, 2005, 11:24 AM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.affymetrix.igb.das2.assay;

import com.affymetrix.igb.das2.*;

import java.io.*;
import java.net.*;
import java.util.*;
import org.xml.sax.*;
import org.w3c.dom.*;

/**
 *
 * @author Marc Carlson
 *
 */

public class Das2AssaySource extends com.affymetrix.igb.das2.Das2Source {

  boolean init;

    /** Creates a new instance of Das2AssaySource */
    public Das2AssaySource(Das2ServerInfo source_server, URI source_uri, boolean init) {
        super(source_server, source_uri, null, null, null, null);
        this.init = init;
    }

    //overridden methods
    public void addVersion(Das2AssayVersionedSource version) {
        this.versions.put(version.getID(), version);
    }

}
