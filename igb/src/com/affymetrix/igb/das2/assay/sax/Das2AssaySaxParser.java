/*
 * Das2AssaySaxParser.java
 *
 * Created on May 2, 2006, 2:20 PM
 *
 * To change this template, choose Tools | Options and locate the template under
 * the Source Creation and Management node. Right-click the template and choose
 * Open. You can then make changes to the template in the Source Editor.
 */

package com.affymetrix.igb.das2.assay.sax;

import java.io.*;
import java.util.*;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import com.affymetrix.igb.das2.*;
import com.affymetrix.igb.das2.assay.*;

/**
 *
 * @author boconnor
 */
public class Das2AssaySaxParser extends DefaultHandler {
  
  Das2AssayVersionedSource version;
  String assayRequest = "";
  
  // The current Assay being read
  String id = "";
  boolean readingRecord = false;
  HashMap images = new HashMap();
  HashMap biomaterials = new HashMap();
  HashMap platforms = new HashMap();
  
  /** Creates a new instance of Das2AssaySaxParser */
  public Das2AssaySaxParser(Das2AssayVersionedSource version, String assayRequest) {
    this.version = version;
    this.assayRequest = assayRequest;
  }
  
  public void startElement(String namespaceURI, String localName,
                           String qName, Attributes atts) {
    
    //FIXME: this uses substring to pull out the contents from
    //relative URIs.  Should just resolve these
    if (qName.equals("PhysicalBioAssay")) {
      id = atts.getValue("", "identifier");
      //System.out.println("Id: "+id);
      readingRecord = true;
    }
    // images
    if (qName.equals("Image")) {
      String uri = atts.getValue("", "URI");
      if(uri.equals("")){/*do nothing if there is no image*/}
      else{images.put(uri.substring(9), uri);}
    }
    // biomaterials
    if (qName.equals("BioMaterial_ref")) {
      String bid = atts.getValue("", "identifier");
      biomaterials.put(bid.substring(12), bid);
    }
    
    if (qName.equals("Array_ref")) {
      String aid = atts.getValue("", "identifier");
      platforms.put(aid.substring(12), aid);
    }
    
  }

  public void endElement(String uri, String localName, String qName) {
    if (qName.equals("PhysicalBioAssay")) {
      version.addAssay(new Das2Assay(version, id, images, biomaterials, platforms));
      id = "";
      images = new HashMap();
      biomaterials = new HashMap();
      platforms = new HashMap();
    }
  }
  
}
