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
import java.net.*;
import org.w3c.dom.*;

import com.affymetrix.igb.das.DasLoader;

/**
 *
 * @author Marc Carlson
 *
 */
public class Das2AssayServerInfo extends com.affymetrix.igb.das2.Das2ServerInfo{

    String root_ontologyUrl;
    protected com.affymetrix.igb.das2.Das2Source dasSource;
    protected Das2VersionedSource dasVersionedSource;
    
    static String URID = "uri";
    static String ID = "uri";
    static String TITLE = "title";
    static String NAME = "name";
    static String TYPE = "type";
    static String QUERY_URI = "query_uri";
    static String QUERY_ID = "query_id";

    /** Creates a new instance of Das2AssayServerInfo */
    public Das2AssayServerInfo(String url, String name, boolean init, String ontologyUrl)
    throws URISyntaxException {
        super(url, name, init);
        this.root_ontologyUrl = ontologyUrl;
    }

    //New Methods
    public String getRootOntologyUrl() {
        return root_ontologyUrl;
    }

    //overridden methods
    /**
     * Return true if successfully initialized.
     */
    public boolean initialize() {
      //FIXME: The assay namespace has not been updated to the DAS2 spec 300
      //       so this doc doesn't have capabilities, uri, etc.
      //FIXME: This code is almost identical to the initialize method in 
      //       Das2OntologyServerInfo.  It should be consolidated in the superclass.
      try {
        String das_query = server_uri.toString();
        URL das_request = new URL(das_query);
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

        // FIXME: all these URIs need to be resolved against the base!!
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
          //DEBUG!!! FIXME!!!!!
          //The docs returned for celsius are too large and don't fit in
          //memory so I skip these while I'm testing
          //if (source_uri.toString().indexOf("celsius") < 0 && source_uri.toString().indexOf("mouse") < 0) {
          //if (source_uri.toString().indexOf("human") > -1 && source_uri.toString().indexOf("mogdie") < 0) {
          if(true) {
          Das2AssaySource dasSource = new Das2AssaySource(this, source_uri, true);

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
              Das2AssayVersionedSource vsource = new Das2AssayVersionedSource(dasSource, version_uri, false);
              dasSource.addVersion(vsource);
            }
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
    
    protected void addDataSource(Das2Source ds) {
        this.sources.put(ds.getID(), (Das2AssaySource) ds);
    }

    protected void setDasSource(Das2ServerInfo _D2SI, URI source_uri, boolean _init){
        Das2AssaySource D2S = new Das2AssaySource( (Das2AssayServerInfo) _D2SI, source_uri, _init);
        dasSource =  (com.affymetrix.igb.das2.Das2Source)D2S;
    }

    protected void setDasVersionedSource(Das2Source _D2S, String _version_id, boolean _init ){
      try  {
        URI vers_uri = new URI(_version_id);
        Das2AssayVersionedSource D2VS = new Das2AssayVersionedSource( (Das2AssaySource) _D2S, vers_uri, _init);
        dasVersionedSource = D2VS;
      }
      catch (Exception ex)  { ex.printStackTrace(); }
    }

}
