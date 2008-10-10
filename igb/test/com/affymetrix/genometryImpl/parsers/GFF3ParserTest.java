/*
 * GFF3ParserTest.java
 * JUnit based test
 *
 * Created on August 18, 2006, 4:49 PM
 */
package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GFF3Sym;
import junit.framework.*;
import java.io.*;
import java.util.*;

/**
 * Tests of class {@link com.affymetrix.igb.parsers.GFF3Parser}.
 */
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
   * Test of parse method using a canonical example.
   */
  public void testParseCanonical() throws Exception {
    System.out.println("parse");
    
    String filename = "igb/test/test_files/GFF3_canonical_example.gff3";
    assertTrue(new File(filename).exists());
    
    InputStream istr = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
    assertNotNull(istr);
    
    AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("test");

    GFFParser instance = new GFFParser(); // the parser should be able to recognized
    // that this is GFF3 and create an instance of GFF3Parser to do the actual parsing.
    
    
    List expResult = null;
    List result = instance.parse(istr, seq_group, true);
    
//    for (int i=0; i<result.size(); i++) {
//      SeqUtils.printSymmetry((SeqSymmetry) result.get(i), "|  ", true);
//    }
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

    GFF3Sym exon1 = (GFF3Sym) mRNA1.getChild(0);
    assertEquals(GFF3Sym.FEATURE_TYPE_EXON, exon1.getFeatureType());

    GFF3Sym cds_group1 = (GFF3Sym) mRNA1.getChild(4);
    assertEquals(GFF3Sym.FEATURE_TYPE_CDS, cds_group1.getFeatureType());
    assertEquals(cds_group1.getChildCount(), 4);

    GFF3Sym cds1 = (GFF3Sym) cds_group1.getChild(0);
    assertEquals(GFF3Sym.FEATURE_TYPE_CDS + "-part", cds1.getFeatureType());
    
    istr.close();
  }

  public void testParseErrors() throws IOException {
    System.out.println("parse");
    
    String filename = "igb/test/test_files/GFF3_with_errors.gff3";
    assertTrue(new File(filename).exists());
    
    InputStream istr = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
    assertNotNull(istr);

    AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("test");

    GFFParser instance = new GFFParser(); // the parser should be able to recognized
    // that this is GFF3 and create an instance of GFF3Parser to do the actual parsing.
    
    
    List expResult = null;
    List result = instance.parse(istr, seq_group, true);
    
    for (int i=0; i<result.size(); i++) {
      SeqUtils.printSymmetry((SeqSymmetry) result.get(i), "|  ", true);
    }
    assertEquals(1, result.size());
  }
  
  /**
   * Test of processDirective method, of class com.affymetrix.igb.parsers.GFF3Parser.
   */
  public void testProcessDirective() throws Exception {
    System.out.println("processDirective");
    
    GFF3Parser instance = new GFF3Parser();
    
    instance.processDirective("##gff-version 3");
    assertEquals(3, instance.gff_version);
    
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
