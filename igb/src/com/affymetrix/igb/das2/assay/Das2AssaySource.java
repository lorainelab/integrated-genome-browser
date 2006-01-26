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

import com.affymetrix.igb.IGB;
import com.affymetrix.igb.util.DasUtils;
import com.affymetrix.igb.util.SynonymLookup;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;
import com.affymetrix.igb.genometry.SingletonGenometryModel;
import com.affymetrix.igb.util.ErrorHandler;

/**
 *
 * @author Marc Carlson
 *
 */

public class Das2AssaySource extends Das2Source {
    
    /** Creates a new instance of Das2AssaySource */
    public Das2AssaySource(Das2AssayServerInfo source_server, String source_id, boolean init) {
        super(source_server, source_id, init);
    }     
    
    //overridden methods
    public void addVersion(Das2AssayVersionedSource version) {
        this.versions.put(version.getID(), version);
    }    
    
}
