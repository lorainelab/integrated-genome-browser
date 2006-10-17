package com.affymetrix.igb.parsers;

import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;
import junit.framework.*;
import java.awt.*;
import java.io.*;

public class FastaParserTest extends TestCase {
  
  public FastaParserTest(String testName) {
    super(testName);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(FastaParserTest.class);
    
    return suite;
  }

  public void testParse() throws Exception {
    System.out.println("parse");
    
    String filename = "test_files/FASTA_chrQ.fasta";
    InputStream istr = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);

    AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("test");

    FastaParser instance = new FastaParser();
    
    MutableAnnotatedBioSeq result = instance.parse(istr);
    
    System.out.println(">>> " + result.getResidues());
    
    assertEquals("chrQ", result.getID());
    assertEquals(33, result.getLength());
    assertEquals("AAAAAAAAAAACCCCCCCCCGGGGGGGGGTTTT", result.getResidues());
    
    assertEquals("AACCC", result.getResidues(9,9+5));
    assertEquals("GGGTT", result.getResidues(9+5,9));
  }
}
