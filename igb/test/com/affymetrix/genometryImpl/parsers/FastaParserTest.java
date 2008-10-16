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
    String filename_1 = "igb/test/test_files/FASTA_chrQ.fasta";
    assertTrue(new File(filename_1).exists());
    InputStream istr_1 = new FileInputStream(filename_1);
    assertNotNull(istr_1);

    String filename_2 = "igb/test/test_files/FASTA_small_genome.fasta";
    assertTrue(new File(filename_2).exists());
    InputStream istr_2 = new FileInputStream(filename_2);
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
    String filename = "igb/test/test_files/FASTA_chrQ.fasta";
    assertTrue(new File(filename).exists());
    
    InputStream istr = new FileInputStream(filename);
    assertNotNull(istr);

    FastaParser instance = new FastaParser();
    
    MutableAnnotatedBioSeq result = instance.parse(istr);
        
    assertEquals("chrQ", result.getID());
    assertEquals(33, result.getLength());
    assertEquals("AAAAAAAAAAACCCCCCCCCGGGGGGGGGTTTT", result.getResidues());
    assertEquals("AACCC", result.getResidues(9,9+5));
    assertEquals("GGGTT", result.getResidues(9+5,9));
  }
  
  
  public void testReadFASTA() throws Exception {
      String filename = "igb/test/test_files/FASTA_obey_70.fasta";
      assertTrue(new File(filename).exists());
 
      char [] expected_fasta = null;
      byte[] fasta = null;
      expected_fasta = "LCLYTHIGRN".toCharArray();
      testFASTASegment(filename, fasta, expected_fasta, 0, 10);
      
      expected_fasta = "VITNLFSAIPYIGTNLVEWI".toCharArray();
      testFASTASegment(filename, fasta, expected_fasta, 53, 73);
      
      expected_fasta = "L".toCharArray();
      testFASTASegment(filename, fasta, expected_fasta, 0, 1);
      
      expected_fasta = "".toCharArray();
      testFASTASegment(filename, fasta, expected_fasta, 3, 3);
      
      expected_fasta = "LMPFLH".toCharArray();
      testFASTASegment(filename, fasta, expected_fasta, 211, 217);

      expected_fasta = "IENY".toCharArray();
      testFASTASegment(filename, fasta, expected_fasta, 280, 284);

      testFASTASegment(filename, fasta, expected_fasta, 280, 290);
      
      fasta = FastaParser.ReadFASTA(new File(filename), 290, 291);
      assertNull(fasta);
  }
  

  public void testReadBadFASTA_1() throws Exception {
      String filename = "igb/test/test_files/FASTA_not_obey_70.fasta";
      assertTrue(new File(filename).exists());
 
      char [] expected_fasta = null;
      byte[] fasta = null;
      expected_fasta = "LCLYTHIGRN".toCharArray();
      try {
          testFASTASegment(filename, fasta, expected_fasta, 10, 0);
      }
      catch(java.lang.IllegalArgumentException ex) {
          return;
      }
      fail("Should throw an IllegalArgumentException");     
  }
  
  public void testReadBadFASTA_2() throws Exception {
      String filename = "igb/test/test_files/FASTA_not_obey_70.fasta";
      assertTrue(new File(filename).exists());
 
      char [] expected_fasta = null;
      byte[] fasta = null;
      expected_fasta = "LCLYTHIGRN".toCharArray();
      try {
          testFASTASegment(filename, fasta, expected_fasta, 10, 100);
      }
      catch(java.io.UnsupportedEncodingException ex) {
          System.out.println(ex.toString());
            return;
      }
      fail("Should throw an UnsupportedEncodingException");     
  }
  

    private void testFASTASegment(String filename, byte[] fasta, char[] expected_fasta, int start, int end) throws IOException {
        System.out.println("Testing " + filename + "from [" + start + ":" + end + "]");
        fasta = FastaParser.ReadFASTA(new File(filename), start, end);
        assertNotNull(fasta);
        for (int i = 0; i < fasta.length; i++) {
            System.out.print((char) fasta[i] + "");
        }
        System.out.println("");

        assertTrue(end - start >= fasta.length);
        
        System.out.println("expected, actual " + expected_fasta.length + ":" + fasta.length);
        
        for (int i=0;i<fasta.length;i++)
            System.out.print((char)fasta[i]);
        System.out.println();
        
        
        assertTrue(expected_fasta.length >= fasta.length);
        
        for (int i = 0; i < fasta.length; i++) {
            assertEquals(expected_fasta[i], (char) fasta[i]);
        }
    }
}
