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
public class Das2ResultSaxParser extends DefaultHandler {
  
  Das2AssayVersionedSource version;
  
  /** Creates a new instance of Das2AssaySaxParser */
  public Das2ResultSaxParser(Das2AssayVersionedSource version) {
    this.version = version;
  }
  
  public void startElement(String namespaceURI, String localName,
                           String qName, Attributes atts) {
    
    //FIXME: this uses substring to pull out the contents from
    //relative URIs.  Should just resolve these
    if (qName.equals("RESULT")) {
      String id = atts.getValue("", "id");
      String assay = atts.getValue("", "assay").substring(9);
      String image = atts.getValue("", "image").substring(9);
      String protocol = atts.getValue("", "protocol").substring(12);
      version.addResult(new Das2Result(version, id, assay, image, protocol));
    }
    
  }
  
}
