package com.affymetrix.genometryImpl;

import com.affymetrix.genometry.seq.*;
import com.affymetrix.genometryImpl.util.NibbleIterator;
import com.affymetrix.genometryImpl.util.RevCompNibbleIterator;
import com.affymetrix.genometryImpl.util.SearchableCharIterator;
import junit.framework.*;
import java.io.*;
import java.util.*;

public class NibbleBioSeqTest extends TestCase {
  
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  
  public NibbleBioSeqTest(String testName) {
    super(testName);
  }

  @Override
  protected void setUp() throws Exception {
  }

  @Override
  protected void tearDown() throws Exception {
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(NibbleBioSeqTest.class);
    
    return suite;
  }

  // Verify that, if you read in a nibble string, and then write it out, it's the same.
  public void testStringToNibblesAndBack() {
    String test_string = "ACTGAAACCCTTTGGGNNNATATGCGC";
    
    byte[] test_array = NibbleIterator.stringToNibbles(test_string, 0, test_string.length());
    GeneralBioSeq nibseq = new GeneralBioSeq(null, null, test_string.length());
    NibbleIterator nibber = new NibbleIterator(test_array, test_string.length());
    assertEquals(test_string,nibber.substring(0, test_string.length()));
    
    nibseq.setResiduesProvider(nibber);
    String result_string = NibbleIterator.nibblesToString(test_array, 0, test_string.length());
    assertEquals(test_string,result_string);
  }
  
  // Verify that Reverse Complement works with NibbleIterators
  public void testRevComp() {
    String test_string = "ACTGAACC";
    String reverse_string = "GGTTCAGT";
    byte[] test_array = NibbleIterator.stringToNibbles(test_string, 0, test_string.length());
    NibbleIterator nibs = new NibbleIterator(test_array, test_string.length());
    RevCompNibbleIterator rcnibs = new RevCompNibbleIterator(test_array, test_string.length());

    String rcstr = rcnibs.substring(0, rcnibs.getLength());
    assertEquals(reverse_string,rcstr);
    System.out.println("length: " + test_string.length());
    System.out.println("in:   " + test_string);
    System.out.println("nib:  " + nibs.substring(0, test_string.length()));

    /*
    StringBuffer compbuf = new StringBuffer();
    for (int i=0; i<test_string.length(); i++) {
      compbuf.append(rcnibs.compCharAt(i));
    }
    System.out.println("comp: " + compbuf.toString());

    StringBuffer revbuf = new StringBuffer();
    for (int i=0; i<test_string.length(); i++) {
      revbuf.append(rcnibs.revCharAt(i));
    }
    System.out.println("rev:  " + revbuf.toString());
    */

    /*
    StringBuffer rcbuf = new StringBuffer();
    for (int i=0; i<test_string.length(); i++) {
      rcbuf.append(rcnibs.charAt(i));
    }
    */
    System.out.println("rc:   " + rcstr);
    System.out.println("index of TTT in forward: " + nibs.indexOf("TTT", 0));
    System.out.println("index of TTT in revcomp: " + rcnibs.indexOf("TTT", 0));
  }
}
