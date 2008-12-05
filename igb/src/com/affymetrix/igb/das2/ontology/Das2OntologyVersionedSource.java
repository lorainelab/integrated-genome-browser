/*
 * Das2OntologyVersionedSource.java
 *
 * Created on November 3, 2005, 3:43 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.affymetrix.igb.das2.ontology;

import com.affymetrix.igb.das2.*;

//import java.io.*;
import java.net.*;
import java.util.*;
//import org.xml.sax.*;
import org.w3c.dom.*;
import java.lang.Object.*;
import java.net.URI.*;
//import java.util.regex.*;

import com.affymetrix.igb.util.ErrorHandler;
import com.affymetrix.igb.das.DasLoader;

/**
 *
 * @author Marc Carlson
 *
 */
public class Das2OntologyVersionedSource extends com.affymetrix.igb.das2.Das2VersionedSource {

    /** Creates a new instance of Das2OntologyVersionedSource */
    public Das2OntologyVersionedSource(Das2Source das_source, URI vers_uri, boolean init) {
        super((Das2Source)das_source, vers_uri, vers_uri.toString(), null, null, init);
    }

    // get annotation types from das2 server
    protected void initTypes(String filter) {
      this.types_filter = filter;
      this.clearTypes();

      //ontology_types_request should look like:
      //String ontologyRequest = "http://das.biopackages.net/das/ontology/obo/1/ontology/";
      String ontologyTypesRequest = this.getID();

      if (filter != null) {
        //next I need a string like:
        //example of onto request:  http://das.biopackages.net/das/ontology/obo/1/ontology/MA
        //FIXME: this is making an assumption about the URL structure
        ontologyTypesRequest = ontologyTypesRequest+"/ontology/"+filter;
      }

      try {
        System.out.println("Das2 Ontology Request: " + ontologyTypesRequest);
        Document doc = DasLoader.getDocument(ontologyTypesRequest);
        Element top_element = doc.getDocumentElement();
        NodeList typeList = doc.getElementsByTagName("term");
        System.out.println("Count of all Ontology Terms: " + typeList.getLength());

        for (int m=0; m< typeList.getLength(); m++)  {
          Element typeNode = (Element)typeList.item(m);
          String termURI = typeNode.getAttribute("id");
          
          String termAccession = termURI; //FIXME: can get rid of this variable
          
          String termName = "";
          String termDef = "";
          try { 
          if (typeNode.getElementsByTagName("name").getLength() > 0) { termName = ((Element)typeNode.getElementsByTagName("name").item(0)).getFirstChild().getNodeValue(); }
          if (typeNode.getElementsByTagName("def").getLength() > 0) { termDef = ((Element)typeNode.getElementsByTagName("def").item(0)).getFirstChild().getNodeValue(); }
          } catch (Exception ignored) {}
          
          Map parents = new HashMap();
          
          // for "is_a" relationships
          NodeList parentList = typeNode.getElementsByTagName("is_a");
          for(int i=0; i<parentList.getLength(); i++) {
            Element parentNode = (Element)parentList.item(i);
            String parentURI = parentNode.getFirstChild().getNodeValue();
            parents.put(parentURI, parentURI);
          }
          
          //for "part_of" relationships
          parentList = typeNode.getElementsByTagName("part_of");
          for(int i=0; i<parentList.getLength(); i++) {
            Element parentNode = (Element)parentList.item(i);
            String parentURI = parentNode.getFirstChild().getNodeValue();
            parents.put(parentURI, parentURI);
          }

          //add each node as you ID them and their parents
          //Das2OntologyType type = new Das2OntologyType(this, new URI(ontologyTypesRequest).resolve(termURI),
          //                        termName, filter, termAccession, termDef, parents);
          Das2OntologyType type = new Das2OntologyType(this, new URI(termURI),
                                  termName, filter, termAccession, termDef, parents);

          this.addType(type);
        }
      }
      catch (Exception ex) {
          ErrorHandler.errorPanel("Error initializing DAS2 types for\n"+ontologyTypesRequest, ex);
      }
      //TODO should types_initialized be true after an exception?
      this.types_initialized = true;
    }
     
  public Map getTypes(String filter) {
     if (! types_initialized || !filter.equals(types_filter)) {
       initTypes(filter);
    }
    return types;
  }

    /**
     *  initRegions() is to remove the need for this function to HAVE to do
     *  anything productive in this class where its not needed or wanted.
     *  This is a FIXME: the correct way to deal with this is to have a proper abstract class
     *  and then have everyone inherit from that.
    **/
    protected void initSegments() {
        this.regions_initialized = true;
    }
}
