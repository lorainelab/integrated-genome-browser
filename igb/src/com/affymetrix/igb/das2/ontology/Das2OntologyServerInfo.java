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

import com.affymetrix.genometryImpl.*;
import com.affymetrix.igb.das.DasLoader;

/**
 *
 * @author Marc Carlson
 *
 */
public class Das2OntologyServerInfo extends Das2ServerInfo{
  protected Das2Source dasSource;
  protected Das2VersionedSource dasVersionedSource;

  static String URID = "uri";
  static String ID = "id";
  static String TITLE = "title";
  static String NAME = "name";
  static String TYPE = "type";
  static String QUERY_URI = "query_uri";
  static String QUERY_ID = "query_id";
  
    /** Creates a new instance of Das2OntologyServerInfo */
    public Das2OntologyServerInfo(String url, String name, boolean init) 
    throws URISyntaxException {
        super(url, name, init);
    }
    
  /**
   * Return true if successfully initialized.
   */
  public boolean initialize() {
    //FIXME: The ontology namespace has not been updated to the DAS2 spec 300
    //       so this doc doesn't have capabilities, uri, etc.
    try {
      URL das_request = server_uri.toURL();
      String das_query = das_request.toExternalForm();
      System.out.println("Das Request: " + das_request);
      URLConnection request_con = das_request.openConnection();
      String content_type = request_con.getHeaderField("Content-Type");
      String das_version = content_type.substring(content_type.indexOf("version=")+8, content_type.length());
      
      //HACK: Affy das server barfs w/ a trailing slash, URI resolution
      //      doesn't work without trailing slash, so adding it back in
      //      here.
      das_query = das_query+"/";
      
      setDasVersion(das_version);

      System.out.println("DAS server version: " + das_version);
      Document doc = DasLoader.getDocument(request_con);

      Element top_element = doc.getDocumentElement();
      NodeList sources= doc.getElementsByTagName("SOURCE");
      System.out.println("source count: " + sources.getLength());
      for (int i=0; i< sources.getLength(); i++)  {
        Element source = (Element)sources.item(i);
        String source_id = source.getAttribute(URID);
	    if (source_id.length() == 0) { source_id = source.getAttribute(ID); }
	    String source_name = source.getAttribute(TITLE);
	    if (source_name.length() == 0) { source_name = source.getAttribute(NAME); }
	    System.out.println("title: " + source_name + ",  length: " + source_name.length());
	    if (source_name == null || source_name.length() == 0) { source_name = source_id; }
	    System.out.println("source_name: " + source_name);
        String source_info_url = source.getAttribute("doc_href");
        String source_description = source.getAttribute("description");
        String source_taxon = source.getAttribute("taxid");

        URI source_uri = getBaseURI(das_query, source).resolve(source_id);

        // Create a new Das2Source
        Das2OntologySource dasSource = new Das2OntologySource(this, source_uri, true);
        
        this.addDataSource(dasSource);
        NodeList slist = source.getChildNodes();
        for (int k=0; k < slist.getLength(); k++) {
          if (slist.item(k).getNodeName().equals("VERSION"))  {
            Element version = (Element)slist.item(k);
            String version_id = version.getAttribute(URID);
            if (version_id.length() == 0) { version_id = version.getAttribute(ID); }
            String version_name = version.getAttribute(TITLE);
            if (version_name.length() == 0) { version_name = version.getAttribute(NAME); }
            if (version_name.length() == 0) { version_name = version_id; }
            System.out.println("version_name: " + version_name);

            String version_desc = version.getAttribute("description");
            String version_info_url = version.getAttribute("doc_href");

            URI version_uri = getBaseURI(das_query, version).resolve(version_id);
            System.out.println("base URI for version element: " + getBaseURI(das_query, version));
            System.out.println("version URI: " + version_uri.toString());

            // Create a new versioned source
            Das2OntologyVersionedSource vsource = new Das2OntologyVersionedSource(dasSource, version_uri, false);
            dasSource.addVersion(vsource);
          }
        }
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    initialized = true;
    return initialized;
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
