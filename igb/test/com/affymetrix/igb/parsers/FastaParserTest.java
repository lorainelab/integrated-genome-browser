package com.affymetrix.igb.parsers;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.MutableAnnotatedBioSeq;
import com.affymetrix.igb.genometry.AnnotatedSeqGroup;
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
    
    String filename = "test_files/FASTA_small_genome.fasta";
    InputStream istr = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);

    AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("test");

    FastaParser instance = new FastaParser();
    
    java.util.List seqs = instance.parseAll(istr, seq_group);
    
    assertEquals(4, seqs.size());
    assertEquals(4, seq_group.getSeqCount());
    
    BioSeq seq = (BioSeq) seqs.get(0);
    assertEquals("chrQ", seq.getID());
    
    seq = (BioSeq) seqs.get(1);
    assertEquals("gi|5524211|gb|AAD44166.1| cytochrome b [Elephas maximus maximus]", seq.getID());
    
    seq = (BioSeq) seqs.get(2);
    assertEquals("SEQUENCE_1", seq.getID());
    
    seq = (BioSeq) seqs.get(3);
    assertEquals("SEQUENCE_2", seq.getID());
    assertEquals("SATV", seq.getResidues(0,4));
  }

  public void testParse() throws Exception {
    System.out.println("parse");
    
    String filename = "test_files/FASTA_chrQ.fasta";
    InputStream istr = Thread.currentThread().getContextClassLoader().getResourceAsStream(filename);

    FastaParser instance = new FastaParser();
    
    MutableAnnotatedBioSeq result = instance.parse(istr);
        
    assertEquals("chrQ", result.getID());
    assertEquals(33, result.getLength());
    assertEquals("AAAAAAAAAAACCCCCCCCCGGGGGGGGGTTTT", result.getResidues());
    assertEquals("AACCC", result.getResidues(9,9+5));
    assertEquals("GGGTT", result.getResidues(9+5,9));
  }
}
