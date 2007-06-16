/*
 * Das2AssayVersionedSource.java
 *
 * Created on November 3, 2005, 3:42 PM
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
import java.lang.Object.*;
import java.net.URI.*;
import java.util.regex.*;

import com.affymetrix.igb.util.ErrorHandler;
import com.affymetrix.igb.das.DasLoader;
import com.affymetrix.igb.das2.Das2VersionedSource;
import com.affymetrix.igb.das2.assay.sax.*;

/**
 *
 * @author Marc Carlson
 *
 */
public class Das2AssayVersionedSource extends Das2VersionedSource {

  boolean platforms_initialized = false;
  LinkedList platforms = new LinkedList();
  Map assays = new LinkedHashMap();
  Map materials = new LinkedHashMap();
  Map results = new LinkedHashMap();
  boolean assays_initialized = false;
  boolean materials_initialized = false;
  boolean results_initialized = false;
  String TYPES_QUERY = "types";
  // hash to convert ID to name
  HashMap typeIdToName = new HashMap();

  /** Creates a new instance of Das2AssayVersionedSource */
  public Das2AssayVersionedSource(Das2Source das_source, URI vers_uri, boolean init) {
        super((Das2Source)das_source, vers_uri, vers_uri.toString(), null, null, init);
    }
    
  // Add Methods
  public void addAssay(Das2Assay assay) {
      assays.put(assay.getID(), assay);
  }

  public void addResult(Das2Result result) {
      results.put(result.getID(), result);
  }

  public void addMaterial(Das2Material material) {
      materials.put(material.getID(), material);
  }

  public void addPlatform(Das2Platform _platform){
      platforms.add(_platform);
  }

  // Get Methods
  public Map getTypes(String filter) {
     if (! types_initialized || !filter.equals(types_filter)) {
       initTypes(filter, true);
    }
    return types;
  }
  
  public Map getTypes() {
    if (! this.types_initialized || this.types_filter != null) {
        initTypes(null, false);
    }
    return this.types;
  }

  public Map getAssays() {
      if(! assays_initialized) {
          initAssays();
      }
      return(assays);
  }

  public Map getResults() {
      if(! results_initialized) {
          initResults();
      }
      return(results);
  }

  public Map getMaterials() {
      if(! materials_initialized) {
          initMaterials();
      }
      return(materials);
  }

  public LinkedList getPlatforms(){
      if(! platforms_initialized) {
          initPlatforms();
      }
    return platforms;
  }

  // Clear Methods
  public void clearAssays() {
      this.assays = new LinkedHashMap();
  }

  public void clearResults() {
      this.results = new LinkedHashMap();
  }

  public void clearMaterials() {
      this.materials = new LinkedHashMap();
  }

  public void clearPlatforms() {
      this.platforms = new LinkedList();
  }

  // Init Methods
  protected void initMaterials() {
    //example get:
    //http://das.biopackages.net/das/assay/human/17/material
    this.clearMaterials();
    try {
      //FIXME: Hack, the affy server can't handle trailing slash but URI resolution won't work with out it
      URI materialsURI = new URI(this.getURI().toString()+"/");
      //FIXME: the material request is hard coded
      String materials_request = DasLoader.getCachedDocumentURL(materialsURI.resolve("material").toString());

      System.out.println("Das Materials Request: " + materials_request);
      
      Das2MaterialSaxParser materialParser = new Das2MaterialSaxParser(this);
      javax.xml.parsers.SAXParserFactory saxParserFactory = javax.xml.parsers.SAXParserFactory.newInstance();
      javax.xml.parsers.SAXParser saxParser = saxParserFactory.newSAXParser();
      org.xml.sax.XMLReader parser = saxParser.getXMLReader();
      parser.setContentHandler(materialParser);
      parser.parse( new org.xml.sax.InputSource(materials_request));
      
    } catch (Exception ex) {
      ErrorHandler.errorPanel("Error initializing DAS materials\n", ex);
    }
    //TODO should types_initialized be true after an exception?
    materials_initialized = true;
  }
  
  protected void initResults() {
    // Example URL
    // http://das.biopackages.net/das/assay/human/17/result
    this.clearResults();
    
    try {
      //FIXME: Hack, the affy server can't handle trailing slash but URI resolution won't work with out it
      URI resultsURI = new URI(this.getURI().toString()+"/");
      String results_request = DasLoader.getCachedDocumentURL(resultsURI.resolve("result").toString());
      
      System.out.println("Das Results Request: " + results_request);
      
      Das2ResultSaxParser resultParser = new Das2ResultSaxParser(this);
      javax.xml.parsers.SAXParserFactory saxParserFactory = javax.xml.parsers.SAXParserFactory.newInstance();
      javax.xml.parsers.SAXParser saxParser = saxParserFactory.newSAXParser();
      org.xml.sax.XMLReader parser = saxParser.getXMLReader();
      parser.setContentHandler(resultParser);
      parser.parse( new org.xml.sax.InputSource(results_request));
      
    } catch (Exception ex) {
      ErrorHandler.errorPanel("Error initializing DAS types\n", ex);
    }
    //TODO should types_initialized be true after an exception?
    results_initialized = true;
  }
  
  protected void initAssays() {
    // Example URL
    // http://das.biopackages.net/das/assay/human/17/assay
    this.clearAssays();
    try {
      //FIXME: Hack, the affy server can't handle trailing slash but URI resolution won't work with out it
      URI assayURI = new URI(this.getURI().toString()+"/");
      String assays_request = DasLoader.getCachedDocumentURL(assayURI.resolve("assay").toString());
      
      Das2AssaySaxParser assayParser = new Das2AssaySaxParser(this, assays_request);
      javax.xml.parsers.SAXParserFactory saxParserFactory = javax.xml.parsers.SAXParserFactory.newInstance();
      javax.xml.parsers.SAXParser saxParser = saxParserFactory.newSAXParser();
      org.xml.sax.XMLReader parser = saxParser.getXMLReader();
      parser.setContentHandler(assayParser);
      parser.parse( new org.xml.sax.InputSource(assays_request));
      
    } catch (Exception ex) {
      ErrorHandler.errorPanel("Error initializing DAS types\n", ex);
    }
    //TODO should types_initialized be true after an exception?
    assays_initialized = true;
  }    
    
  protected void initPlatforms(){
    // Example URL
    // http://das.biopackages.net/das/assay/human/17/platform
    this.clearPlatforms();

    try {
      //FIXME: Hack, the affy server can't handle trailing slash but URI resolution won't work with out it
      URI platformURI = new URI(this.getURI().toString()+"/");
      String plat_request = DasLoader.getCachedDocumentURL(platformURI.resolve("platform").toString());
      
      System.out.println("Current DAS platform Request: " + plat_request);
      Document doc = DasLoader.getDocument(plat_request);
      NodeList platlist = doc.getElementsByTagName("ArrayDesign");
      System.out.println("platforms: " + platlist.getLength());
      for (int i=0; i< platlist.getLength(); i++)  {
          Element platnode = (Element)platlist.item(i);
          String platId = platnode.getAttribute("identifier");
          this.addPlatform(new Das2Platform(this, platId));
      }
  } catch (Exception ex) {
      ErrorHandler.errorPanel("Error initializing DAS types\n", ex);
  }
      platforms_initialized = true;
  }

  // get annotation types from das server
  protected void initTypes(String filter, boolean getParents) {
    this.types_filter = filter;
    this.clearTypes();

    // String types_request = "file:/C:/data/das2_responses/alan_server/types_short.xml";
    try {
      //FIXME: Hack, the affy server can't handle trailing slash but URI resolution won't work with out it
      URI typeURI = new URI(this.getURI().toString()+"/");
      String types_request = typeURI.resolve("type").toString();
      
      Document doc = DasLoader.getDocument(types_request);
      NodeList list = doc.getElementsByTagName("TYPE");
      System.out.println("types: " + list.getLength());
      for (int i=0; i< list.getLength(); i++)  {
          Element node = (Element)list.item(i);
          String id = node.getAttribute("id");
          String ontology = node.getAttribute("ontology");
          
          String[] ontTokens = ontology.split("/");
          String ontName = ontTokens[ontTokens.length-2];
          String ontAccession = ontTokens[ontTokens.length-1];
          String name = node.getAttribute("name");
          String def = node.getAttribute("definition");
          
          // FIXME: need to be consistent about the URI here w/ ontology namespace
          
          this.addType(new Das2Type((Das2VersionedSource)this, new URI(ontName+"/"+ontAccession), name, 
                  ontName+"/"+ontAccession, id, def, null, null, null));
      }
    
    }
    catch (Exception ex) {
      ErrorHandler.errorPanel("Error initializing DAS types\n", ex);
    }
    //TODO should types_initialized be true after an exception?
    this.types_initialized = true;
  }


}
