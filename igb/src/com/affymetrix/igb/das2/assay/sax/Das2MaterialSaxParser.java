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
public class Das2MaterialSaxParser extends DefaultHandler {
  
  Das2AssayVersionedSource version;
  
  // The current Assay being read
  String id = "";
  String name = "";
  HashMap types = new HashMap();
  HashMap contacts = new HashMap();
  bool saveMem = true;
  
  /** Creates a new instance of Das2AssaySaxParser */
  public Das2MaterialSaxParser(Das2AssayVersionedSource version) {
    this.version = version;
  }
  
  public void startElement(String namespaceURI, String localName,
                           String qName, Attributes atts) {
    
    //FIXME: this uses substring to pull out the contents from
    //relative URIs.  Should just resolve these
    if (qName.equals("BioSource")) {
      id = atts.getValue("", "identifier");
      name = atts.getValue("", "name");
    }
    // types
    if (qName.equals("DatabaseEntry")) {
      String uri = atts.getValue("", "URI");
      if(uri.equals("")){ }
      else{ types.put(uri.substring(8), uri); }
    }
    // contacts
    if (qName.equals("Organization_ref")) {
      String contact = atts.getValue("", "identifier");
      contacts.put(contact.substring(11), contact);
    }
  }

  public void endElement(String uri, String localName, String qName) {
    if (qName.equals("BioSource")) {
      // FIXME: HACK, I'm throwing away any materials that don't have types
      // to save memory.  Very large DAS/2 Assay servers will cause the client
      // to run out of memory (such as das.biopackages.net)
      if (!saveMem || !types.isEmpty()) 
        version.addMaterial(new Das2Material(version, id, name, types, contacts));
      id = "";
      name = "";
      types = new HashMap();
      contacts = new HashMap();
    }
  }

  public void setSaveMem(bool saveMem) {
    this.saveMem = saveMem;
  }
  
}
