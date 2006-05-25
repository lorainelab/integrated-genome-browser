/*
 * Das2OntologyServerInfo.java
 *
 * Created on November 8, 2005, 1:30 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.affymetrix.igb.das2.ontology;

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
public class Das2OntologyServerInfo extends Das2ServerInfo{
  protected Das2Source dasSource;
  protected Das2VersionedSource dasVersionedSource;


    /** Creates a new instance of Das2OntologyServerInfo */
    public Das2OntologyServerInfo(String url, String name, boolean init) {
        super(url, name, init);
    }


    //overridden methods
    protected void addDataSource(Das2Source ds) {
        this.sources.put(ds.getID(), (Das2OntologySource) ds);
    }

    /**
     * @param _init boolean  deprecated, doesn't do anything
     */
    protected void setDasSource(Das2ServerInfo _D2SI, String _source_id, boolean _init){
      try  {
        URI source_uri = new URI(_source_id);
        Das2OntologySource D2S = new Das2OntologySource( (
            Das2OntologyServerInfo) _D2SI, source_uri, _init);
        dasSource = D2S;
      }
      catch (Exception ex)  { ex.printStackTrace(); }
    }

    protected void setDasVersionedSource(Das2Source _D2S, String _version_id, boolean _init ){
      try  {
        URI vers_uri = new URI(_version_id);
        Das2OntologyVersionedSource D2VS = new Das2OntologyVersionedSource( (Das2OntologySource) _D2S, vers_uri, _init);
        dasVersionedSource = D2VS;
      }
      catch (Exception ex)  { ex.printStackTrace(); }
    }

}
