/*
 * GFF3ParserTest.java
 * JUnit based test
 *
 * Created on August 18, 2006, 4:49 PM
 */
package com.affymetrix.igb.parsers;

import junit.framework.*;
import java.io.*;
import java.util.*;
import com.affymetrix.genometry.*;
import com.affymetrix.genometry.util.*;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;
import com.affymetrix.igb.genometry.GFF3Sym;

public class GFF3ParserTest extends TestCase {
  
  public GFF3ParserTest(String testName) {
    super(testName);
  }

  protected void setUp() throws Exception {
  }

  protected void tearDown() throws Exception {
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(GFF3ParserTest.class);
    
    return suite;
  }

  /**
   * Test of parse method, of class com.affymetrix.igb.parsers.GFF3Parser.
   */
  public void testParse() throws Exception {
    System.out.println("parse");
    
    String filename = "test_files/GFF3_canonical_example.gff3";
    InputStream istr = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);

    AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("test");
    GFF3Parser instance = new GFF3Parser();
    
    List expResult = null;
    List result = instance.parse(istr, seq_group);
    
    for (int i=0; i<result.size(); i++) {
      SeqUtils.printSymmetry((SeqSymmetry) result.get(i), "|  ", true);
    }
    assertEquals(1, result.size());
    
    
    SeqSymmetry gene = (SeqSymmetry) result.get(0);
    assertEquals(999, gene.getSpan(0).getStart());
    assertEquals(9000, gene.getSpan(0).getEnd());
    
    assertEquals(4, gene.getChildCount());
    GFF3Sym  binding_site = (GFF3Sym) gene.getChild(0);
    GFF3Sym mRNA1 = (GFF3Sym) gene.getChild(1);
    GFF3Sym mRNA2 = (GFF3Sym) gene.getChild(2);
    GFF3Sym mRNA3 = (GFF3Sym) gene.getChild(3);
    
    assertEquals("EDEN.1", mRNA1.getProperty(GFF3Parser.GFF3_NAME));
    
    assertEquals(4+1, mRNA1.getChildCount()); // 4 exons, 1 CDS
    assertEquals(3+1, mRNA2.getChildCount()); // 3 exons, 1 CDS
    assertEquals(4+2, mRNA3.getChildCount()); // 4 exons, 2 CDS
  }

  /**
   * Test of processDirective method, of class com.affymetrix.igb.parsers.GFF3Parser.
   */
  public void testProcessDirective() throws Exception {
    System.out.println("processDirective");
    
    GFF3Parser instance = new GFF3Parser();
    
    instance.processDirective("##gff-version 3");
    
    // Setting to gff-version 2 should throw an exception
    Exception e = null;
    try {
      instance.processDirective("##gff-version 2");
    } catch (IOException ioe) {
      e = ioe;
    }
    assertNotNull(e);
    
  }
}
