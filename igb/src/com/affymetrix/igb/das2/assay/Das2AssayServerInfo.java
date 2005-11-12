/*
 * Das2AssayServerInfo.java
 *
 * Created on November 8, 2005, 11:13 AM
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
import org.w3c.dom.*;

import com.affymetrix.igb.das.DasLoader;
import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;

/**
 *
 * @author Marc Carlson
 *
 */
public class Das2AssayServerInfo extends Das2ServerInfo{
    
    String root_ontologyUrl;
  
    
    /** Creates a new instance of Das2AssayServerInfo */
    public Das2AssayServerInfo(String url, String name, boolean init, String ontologyUrl) {
        super(url, name, init);
        this.root_ontologyUrl = ontologyUrl;
    }

    //New Methods
    public String getRootOntologyUrl() {
        return root_ontologyUrl;
    }    

    
    //overridden methods
    protected void addDataSource(Das2Source ds) {
        this.sources.put(ds.getID(), (Das2AssaySource) ds);
    }
  
    protected void setDasSource(Das2ServerInfo _D2SI, String _source_id, boolean _init){
        Das2AssaySource D2S = new Das2AssaySource( (Das2AssayServerInfo) _D2SI, _source_id, _init);
        dasSource =  D2S;
    }

    protected void setDasVersionedSource(Das2Source _D2S, String _version_id, boolean _init ){
        Das2AssayVersionedSource D2VS = new Das2AssayVersionedSource( (Das2AssaySource) _D2S, _version_id, _init);
        dasVersionedSource = D2VS;
    }
    
}
