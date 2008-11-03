package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symmetry.SimpleSeqSymmetry;
import com.affymetrix.genometryImpl.*;
import junit.framework.*;
import java.io.*;
import java.util.*;

public class BedParserTest extends TestCase {
  
  static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
  
  public BedParserTest(String testName) {
    super(testName);
  }

  @Override
  protected void setUp() throws Exception {
  }

  @Override
  protected void tearDown() throws Exception {
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(BedParserTest.class);
    
    return suite;
  }

  public void testParseFromFile() throws IOException {
    
    String filename = "test/test_files/bed_01.bed";
    assertTrue(new File(filename).exists());
    
    InputStream istr = new FileInputStream(filename); 
    assertNotNull(istr);
    
    AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
    boolean annot_seq = true;
    String stream_name = "test_file";
    boolean create_container = true;
    
    BedParser parser = new BedParser();
    List<SeqSymmetry> result = parser.parse(istr, gmodel, group, annot_seq, stream_name, create_container);

    assertEquals(6, result.size());    
        
    UcscBedSym sym = (UcscBedSym) result.get(2);
    assertEquals(1, sym.getSpanCount());
    SeqSpan span = sym.getSpan(0);
    assertEquals(1790361, span.getMax());
    assertEquals(1789140, span.getMin());
    assertEquals(false, span.isForward());
    assertEquals(false, sym.hasCdsSpan());
    assertEquals(null, sym.getCdsSpan());
    assertEquals(2, sym.getChildCount());
    
    sym = (UcscBedSym) result.get(5);
    assertEquals(sym.hasCdsSpan(), true);
    SeqSpan cds = sym.getCdsSpan();
    assertEquals(1965425, cds.getMin());
    assertEquals(1965460, cds.getMax());
    assertEquals(new Float(0), ((Float) sym.getProperty("score")));
  }
  
  /**
   * Test of parse method, of class com.affymetrix.igb.parsers.BedParser.
   */
  public void testParseFromString() throws Exception {
    System.out.println("parse");
    
    String string = 
        "591	chr2L	901490	901662	CR31656-RA	0	-	901490	901662	0	1	172,	0,\n"+
        "595	chr2L	1432710	1432921	CR31927-RA	0	+	1432710	1432920	0	1	211,	0,\n"+
      
        // Next line is line "2": we'll specifically test that it was read correctly
        "598	chr2L	1789140	1790361	CR31930-RA	0	-	1789140	1789140	0	2	153,1010,	0,211,\n"+
        "598	chr2L	1792056	1793268	CR31931-RA	0	-	1792056	1792056	0	2	153,1014,	0,198,\n"+
        "599	chr2L	1938088	1938159	CR31667-RA	0	-	1938088	1938088	0	1	71,	0,\n"+
      
        // This last line has a CDS: we'll test that it was read correctly as well
        "599	chr2L	1965425	1965498	CR31942-RA	0	+	1965425	1965460	0	1	73,	0,\n"
        ;
    
    InputStream istr = new ByteArrayInputStream(string.getBytes());
    AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
    boolean annot_seq = true;
    String stream_name = "test_file";
    boolean create_container = true;
    BedParser instance = new BedParser();
    
    List<SeqSymmetry> result = instance.parse(istr, gmodel, group, annot_seq, stream_name, create_container);

    assertEquals(6, result.size());    
        
    UcscBedSym sym = (UcscBedSym) result.get(2);
    assertEquals(1, sym.getSpanCount());
    SeqSpan span = sym.getSpan(0);
    assertEquals(1790361, span.getMax());
    assertEquals(1789140, span.getMin());
    assertEquals(false, span.isForward());
    assertEquals(false, sym.hasCdsSpan());
    assertEquals(null, sym.getCdsSpan());
    assertEquals(2, sym.getChildCount());
    
    sym = (UcscBedSym) result.get(5);
    assertEquals(sym.hasCdsSpan(), true);
    SeqSpan cds = sym.getCdsSpan();
    assertEquals(1965425, cds.getMin());
    assertEquals(1965460, cds.getMax());
    assertEquals(new Float(0), ((Float) sym.getProperty("score")));
  }

  /**
   * Test of parseIntArray method, of class com.affymetrix.igb.parsers.BedParser.
   */
  public void testParseIntArray() {
    System.out.println("parseIntArray");
    
    String int_array = "1,7,8,9,10";
    BedParser instance = new BedParser();
    
    int[] expResult = new int[] {1,7,8,9,10};
    int[] result = instance.parseIntArray(int_array);
    for (int i=0; i<expResult.length; i++) {
      assertEquals(expResult[i], result[i]);
    }    
  }
  
  public void testParseIntArrayWithWhitespace() {
    System.out.println("parseIntArray");
    
    // the parser doesn't accept whitespace in the integer lists
    // (Maybe it should, but it insn't expected to need to do so.)
    String int_array = "1,7, 8,9,10";
    BedParser instance = new BedParser();
    
    boolean passed = false;
    try {
      int[] result = instance.parseIntArray(int_array);
    } catch (NumberFormatException nfe) {
      passed = true;
    }
    if (!passed) fail("Expected exception was not thrown");
  }
  
  
  /**
   * Test of makeBlockMins method, of class com.affymetrix.igb.parsers.BedParser.
   */
  public void testMakeBlockMins() {
    System.out.println("makeBlockMins");
    
    int min = 100;
    int[] blockStarts = new int[] {1,3,4,5,9};
    BedParser instance = new BedParser();
    
    int[] expResult = new int[] {101,103,104,105,109};
    int[] result = instance.makeBlockMins(min, blockStarts);
    for (int i=0; i<expResult.length; i++) {
      assertEquals(expResult[i], result[i]);
    }    
  }

  /**
   * Test of makeBlockMaxs method, of class com.affymetrix.igb.parsers.BedParser.
   */
  public void testMakeBlockMaxs() {
    System.out.println("makeBlockMaxs");
    
    int[] blockMins =  new int[] {1,3,4,5,9};
    int[] blockSizes =  new int[] {1,3,4,5,9};
    BedParser instance = new BedParser();
    
    int[] expResult =  new int[] {2,6,8,10,18};
    int[] result = instance.makeBlockMaxs(blockMins, blockSizes);
    for (int i=0; i<expResult.length; i++) {
      assertEquals(expResult[i], result[i]);
    }    
  }

  /**
   * Test of writeBedFormat method, of class com.affymetrix.igb.parsers.BedParser.
   */
  public void testWriteBedFormat() throws Exception {
    System.out.println("writeBedFormat");
    
    Writer out = new StringWriter();
    
    AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
    BioSeq seq = group.addSeq("chr12", 500000);
    SeqSpan span = new SimpleSeqSpan(500,800,seq);
    SeqSpan[] span_array = new SeqSpan[] {span};
    SeqSymmetry sym = new SimpleSeqSymmetry(span_array);
    
    BedParser.writeBedFormat(out, sym, seq);
    assertEquals("chr12\t500\t800\n", out.toString());    
  }

  /**
   * Test of writeAnnotations method, of class com.affymetrix.igb.parsers.BedParser.
   */
  public void testWriteAnnotations() {
    System.out.println("writeAnnotations");

    String string = 
        "chr2L	901490	901662	CR31656-RA	0	-	901490	901662	0	1	172,	0,\n"+
        "chr2L	1432710	1432921	CR31927-RA	0	+	1432710	1432920	0	1	211,	0,\n"+
        "chr2L	1789140	1790361	CR31930-RA	0	-	1789140	1789140	0	2	153,1010,	0,211,\n"+
        "chr2L	1792056	1793268	CR31931-RA	0	-	1792056	1792056	0	2	153,1014,	0,198,\n"+
        "chr2L	1938088	1938159	CR31667-RA	0	-	1938088	1938088	0	1	71,	0,\n"+
        "chr2L	1965425	1965498	CR31942-RA	0	+	1965425	1965460	0	1	73,	0,\n"
        ;
    
    InputStream istr = new ByteArrayInputStream(string.getBytes());
    AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
    boolean annot_seq = true;
    String stream_name = "test_file";
    boolean create_container = true;
    BedParser instance = new BedParser();
    
    Collection<SeqSymmetry> syms = null;
    try {
      syms = instance.parse(istr,gmodel,group,annot_seq,stream_name,create_container);
    } catch (IOException ioe) {
      fail("Exception: " + ioe);
    }

    // Now we have read the data into "syms", so let's try writing it.
    
    BioSeq seq = group.getSeq("chr2L");
    String type = "test_type";
    ByteArrayOutputStream outstream = new ByteArrayOutputStream();
    
    boolean result = instance.writeAnnotations(syms, seq, type, outstream);
    assertEquals(true, result);
    assertEquals(string, outstream.toString());
  }

  /**
   * Test of getMimeType method, of class com.affymetrix.igb.parsers.BedParser.
   */
  public void testGetMimeType() {
    System.out.println("getMimeType");
    
    BedParser instance = new BedParser();
    
    String expResult = "text/plain";
    String result = instance.getMimeType();
    assertEquals(expResult, result);    
  }
  
}
