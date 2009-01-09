package com.affymetrix.genometryImpl;

import com.affymetrix.genometry.seq.*;
import com.affymetrix.genometryImpl.util.NibbleIterator;
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
  
}
