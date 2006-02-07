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

import java.io.*;
import java.net.*;
import java.util.*;
import org.xml.sax.*;
import org.w3c.dom.*;
import java.lang.Object.*;
import java.net.URI.*;
import java.util.regex.*;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.util.DasUtils;
import com.affymetrix.igb.util.SynonymLookup;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;
import com.affymetrix.igb.genometry.SingletonGenometryModel;
import com.affymetrix.igb.util.ErrorHandler;
import com.affymetrix.igb.das.DasLoader;

/**
 *
 * @author Marc Carlson
 *
 */
public class Das2OntologyVersionedSource extends Das2VersionedSourcePlus {

    /** Creates a new instance of Das2OntologyVersionedSource */
    public Das2OntologyVersionedSource(Das2OntologySource das_source, String version_id, boolean init) {
        super(das_source, version_id, init);
    }

    // get annotation types from das server
    protected void initTypes(String filter) {
    this.types_filter = filter;
    this.clearTypes();

    //ontology_types_request should look like:
    //String ontologyRequest = "http://das.biopackages.net/das/ontology/obo/1/ontology/";
    String ontologyTypesRequest = getSource().getServerInfo().getRootUrl();

    if (filter != null) {
      //next I need a string like:
      //example of onto request:  http://das.biopackages.net/das/ontology/obo/1/ontology/MA
      ontologyTypesRequest = ontologyTypesRequest+"/"+filter;
    }

    try {
      System.out.println("Das Ontology Request: " + ontologyTypesRequest);
      Document doc = DasLoader.getDocument(ontologyTypesRequest);
      Element top_element = doc.getDocumentElement();
      NodeList typeList = doc.getElementsByTagName("TERM");
      System.out.println("Count of all Ontology Terms: " + typeList.getLength());

      HashMap curOntology = new HashMap();
      HashMap parentHoldingHash = new HashMap();

      //this is for tracking whether or not a node has been included in the list of nodes to draw/use later on...
      HashMap uberTrackingHash = new HashMap();
      String ontoRootNodeId = new String() ;
      int typeCounter = 0;

      //Loop through the entire ontology and put the relevant reationships into this map
      for (int m=0; m< typeList.getLength(); m++)  {
        Element typeNode = (Element)typeList.item(m);
        String termID = typeNode.getAttribute("id");

        // temporary workaround for getting type ending, rather than full URI
        if (termID.startsWith("./")) { termID = termID.substring(2); }

        //the pattern to look for gets compiled in here
        Pattern p1 = Pattern.compile("/");
        //init the matcher object
        Matcher m1 = p1.matcher("");
        //reset the matcher with the string in question
        m1.reset(termID);
        //Replace found characters with an empty string.
        termID = m1.replaceAll(":");

        String ontid = typeNode.getAttribute("ontology");
	String type_source = typeNode.getAttribute("source");
	String href = typeNode.getAttribute("doc_href");

	NodeList flist = typeNode.getElementsByTagName("FORMAT");
	LinkedHashMap formats = new LinkedHashMap();
	HashMap props = new HashMap();
	for (int k=0; k<flist.getLength(); k++) {
	  Element fnode = (Element)flist.item(k);
	  String formatid = fnode.getAttribute("id");
	  String mimetype = fnode.getAttribute("mimetype");
	  if (mimetype == null || mimetype.equals("")) { mimetype = "unknown"; }
          //	  System.out.println("alternative format for annot type " + typeid +
          //": format = " + formatid + ", mimetype = " + mimetype);
          formats.put(formatid, mimetype);
	}

	NodeList plist = typeNode.getElementsByTagName("PROP");
	for (int k=0; k<plist.getLength(); k++) {
	  Element pnode = (Element)plist.item(k);
	  String key = pnode.getAttribute("key");
	  String val = pnode.getAttribute("value");
	  props.put(key, val);
	}

        HashMap parents = new HashMap();

        //do not assign real vals to these
        NodeList parentsList = typeNode.getElementsByTagName("PARENT");
        for (int l=0; l<parentsList.getLength(); l++) {
           Element pnode = (Element)parentsList.item(l);
           String key = pnode.getAttribute("id");

           //the pattern to look for gets compiled in here
           Pattern p2 = Pattern.compile("/");
           //init the matcher object
           Matcher m2 = p2.matcher("");
           //reset the matcher with the string in question
           m2.reset(key);
           //Replace found characters with an empty string.
           key = m2.replaceAll(":");

           parents.put(key, key);
        }

        //add each node as you ID them and their parents
        Das2Type type = new Das2Type(this, termID, ontid, type_source, href, formats, props, parents);
        this.addType(type);

        typeCounter++;
      }

    }
    catch (Exception ex) {
        ErrorHandler.errorPanel("Error initializing DAS types for\n"+ontologyTypesRequest, ex);
    }
    //TODO should types_initialized be true after an exception?
        this.types_initialized = true;
    }

    /**
     *  initRegions() is to remove the need for this function to HAVE to do
     *  anything productive in this class where its not needed or wanted.
     *  This is a FIXME: the correct way to deal with this is to have a proper abstract class
     *  and then have everyone inherit from that.
    **/
    protected void initRegions() {
        this.regions_initialized = true;
    }

}
