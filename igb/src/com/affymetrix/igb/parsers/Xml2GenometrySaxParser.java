/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
*    
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*   Distributions from Affymetrix, Inc., place this in the
*   IGB_LICENSE.html file.  
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/

package com.affymetrix.igb.parsers;

import java.io.*;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.*;
import org.xml.sax.helpers.DefaultHandler;

import com.affymetrix.genoviz.util.Timer;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.seq.*;
import com.affymetrix.genometry.symmetry.*;
import com.affymetrix.genometry.span.*;

public class Xml2GenometrySaxParser extends DefaultHandler {

  public Xml2GenometrySaxParser() {
    super();
  }

  public AnnotatedBioSeq parseSaxAxml(File fil) {
    AnnotatedBioSeq aseq = null;
    try {
      FileReader fr = new FileReader(fil);
      InputSource src = new InputSource(fr);
      aseq = this.parseAxml(src);
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    return aseq;
  }

  public AnnotatedBioSeq parseAxml(InputSource src) {
    try {
      XMLReader reader = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
      reader.setContentHandler(this);
      System.out.println("trying to do load in AXML via a SAX-driven parser");
      Timer tim = new Timer();
      tim.start();
      reader.parse(src);
      System.out.println("Time to process XML as SAX events: " + tim.read()/1000f);
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    return new SimpleAnnotatedBioSeq("test", 5000);
  }

  public void startDocument() {
    System.out.println("document start");
  }

  public void endDocument() {
    System.out.println("document end");
    System.out.println("element count = " + count);
  }

  int count = 0;
  public void startElement(String uri, String name, String qname, Attributes atts) {
    count++;
  }

  public void endElement(String uri, String name, String qname)  {

  }

}
