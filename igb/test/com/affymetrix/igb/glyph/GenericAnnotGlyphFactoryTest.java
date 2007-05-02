/*
 * GenericAnnotGlyphFactoryTest.java
 * JUnit based test
 *
 * Created on June 19, 2006, 2:19 PM
 */

package com.affymetrix.igb.glyph;

import junit.framework.*;
import com.affymetrix.genometry.*;
import com.affymetrix.genometry.seq.SimpleAnnotatedBioSeq;
import com.affymetrix.genometry.util.*;
import com.affymetrix.genometry.span.SimpleMutableSeqSpan;
import com.affymetrix.genometry.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.igb.view.SeqMapView;

public class GenericAnnotGlyphFactoryTest extends TestCase {
  
  BioSeq annot_seq = null;
  BioSeq view_seq = null;
    
  public GenericAnnotGlyphFactoryTest(String testName) {
    super(testName);
  }

  protected void setUp() throws Exception {
    annot_seq = new SimpleAnnotatedBioSeq("annot", 1000000);
    view_seq = new SimpleAnnotatedBioSeq("view_seq", 1000000);
  }

  protected void tearDown() throws Exception {
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(GenericAnnotGlyphFactoryTest.class);
    
    return suite;
  }
  
  /**
   * Test of transformSymmetry method, of class com.affymetrix.genometry.util.SeqUtils.
   */
  public void testTransformSymmetry() {
    System.out.println("transformSymmetry");
    
    int startsA[] = new int[] {500,  900, 1100, 1300, 1500, 1900};
    int stopsA[] = new int[]  {600, 1000, 1200, 1400, 1600, 2100};
    
    int startsV[] = new int[] {0,    100,  200,  300,  400,  500};
    int stopsV[] = new int[]  {100,  200,  300,  400,  500,  700};
    
    MutableSeqSymmetry transformer = new SimpleMutableSeqSymmetry();
    // These next two lines are necessary.  But should they be?
    transformer.addSpan(new SimpleMutableSeqSpan(500, 1900, annot_seq));
    transformer.addSpan(new SimpleMutableSeqSpan(0, 700, view_seq));

    for (int i=0; i<startsA.length; i++) {
      MutableSeqSymmetry child = new SimpleMutableSeqSymmetry();
      child.addSpan(new SimpleMutableSeqSpan(startsA[i], stopsA[i], annot_seq));
      child.addSpan(new SimpleMutableSeqSpan(startsV[i], stopsV[i], view_seq));
      transformer.addChild(child);
    }
    
    int starts[] = new int[] {125, 325, 525, 725, 925, 1125, 1225, 1525, 1725, 1925, 2125};
    
    MutableSeqSymmetry initialSym = new SimpleMutableSeqSymmetry();
    for (int i=0; i<starts.length; i++) {
      MutableSeqSymmetry child = new SimpleMutableSeqSymmetry();
      child.addSpan(new SimpleMutableSeqSpan(starts[i], starts[i] + 50, annot_seq));
      initialSym.addChild(child);
    }    

    System.out.println("Transformer");
    SeqUtils.printSymmetry(transformer);
        
    System.out.println("");
    System.out.println("---------------------------");
    System.out.println("");
    
    boolean result = SeqUtils.transformSymmetry(initialSym, transformer);
    
    System.out.println("Result");
    SeqUtils.printSymmetry(initialSym);        
    
    int[][] bounds = GenericAnnotGlyphFactory.determineBoundaries(transformer, annot_seq, view_seq);

    assertEquals(bounds[0][0], Integer.MIN_VALUE);
    assertEquals(bounds[0][1], 0);

    assertEquals(bounds[1][0], 600);
    assertEquals(bounds[1][1], 100);
    
    assertEquals(bounds[2][0], 1000);
    assertEquals(bounds[2][1], 200);
    
    assertEquals(bounds[3][0], 1200);
    assertEquals(bounds[3][1], 300);
    
    assertEquals(bounds[4][0], 1400);
    assertEquals(bounds[4][1], 400);
    
    assertEquals(bounds[5][0], 1600);
    assertEquals(bounds[5][1], 500);
    
    assertEquals(bounds[6][0], 2100);
    assertEquals(bounds[6][1], 700);
    
    describeSym(bounds, initialSym);
    
    assertEquals(true, result);
  }

  void describeSym(int[][] bounds, SeqSymmetry parent) {
    
//    transformerTester(annot_seq, view_seq, transformer);
    
    int j=0;
    for (int i=0; i< parent.getChildCount(); i++) {
      SeqSymmetry child = parent.getChild(i);
      SeqSpan span = child.getSpan(view_seq);
      
      System.out.println("Child " + i + ": ");
      if (span != null) {
        SeqUtils.printSpan(span);
      } else {
         int annot_span_min = child.getSpan(annot_seq).getMin();
         while (j+1 < bounds.length && annot_span_min >= bounds[j+1][0]) {
           j++;
         }
        
        System.out.println("gap at "+ j + " : " + bounds[j][1]);
      }
      
    }    
  }
  
  // $Author$
  // $Date$
  // $Header$
  // $ID$
  // $Locker$
  // $Name$
  // $RCSfile$
  // $Revision$
  // $Source$
  // $State$

  // $Log$
  // Revision 1.2  2007/05/02 16:44:57  chyekk
  // Just testing CVS tags.  Ignore.
  //
}
