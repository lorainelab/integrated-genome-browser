package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import junit.framework.*;
import java.io.*;

public class FastaParserTest extends TestCase {
  
  public FastaParserTest(String testName) {
    super(testName);
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(FastaParserTest.class);
    
    return suite;
  }
  
  public void testParseAll() throws Exception {
    System.out.println("parse");
    
    String filename_1 = "igb/test/test_files/FASTA_chrQ.fasta";
    assertTrue(new File(filename_1).exists());
    InputStream istr_1 = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename_1);
    assertNotNull(istr_1);

    String filename_2 = "igb/test/test_files/FASTA_small_genome.fasta";
    assertTrue(new File(filename_2).exists());
    InputStream istr_2 = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename_2);
    assertNotNull(istr_2);

    AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("test");

    FastaParser instance = new FastaParser();
    
    java.util.List seqs = instance.parseAll(istr_1, seq_group);

    assertEquals(1, seqs.size());
    assertEquals(1, seq_group.getSeqCount());
    
    BioSeq seq = (BioSeq) seqs.get(0);
    assertEquals("chrQ", seq.getID());
    
    seqs = instance.parseAll(istr_2, seq_group);
    
    assertEquals(3, seqs.size());
    assertEquals(4, seq_group.getSeqCount());
        
    seq = (BioSeq) seqs.get(0);
    assertEquals("gi|5524211|gb|AAD44166.1| cytochrome b [Elephas maximus maximus]", seq.getID());
    
    seq = (BioSeq) seqs.get(1);
    assertEquals("SEQUENCE_1", seq.getID());
    
    seq = (BioSeq) seqs.get(2);
    assertEquals("SEQUENCE_2", seq.getID());
    assertEquals("SATV", seq.getResidues(0,4));
  }

  public void testParse() throws Exception {
    System.out.println("parse");
    
    String filename = "igb/test/test_files/FASTA_chrQ.fasta";
    assertTrue(new File(filename).exists());
    
    InputStream istr = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);
    assertNotNull(istr);

    FastaParser instance = new FastaParser();
    
    MutableAnnotatedBioSeq result = instance.parse(istr);
        
    assertEquals("chrQ", result.getID());
    assertEquals(33, result.getLength());
    assertEquals("AAAAAAAAAAACCCCCCCCCGGGGGGGGGTTTT", result.getResidues());
    assertEquals("AACCC", result.getResidues(9,9+5));
    assertEquals("GGGTT", result.getResidues(9+5,9));
  }
}
