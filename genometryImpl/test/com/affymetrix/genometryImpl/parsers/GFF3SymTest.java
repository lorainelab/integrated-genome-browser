/*
 * GFF3SymTest.java
 * JUnit based test
 *
 * Created on August 24, 2006, 10:28 AM
 */
package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometryImpl.GFF3Sym;
import junit.framework.*;

public class GFF3SymTest extends TestCase {
  
  public GFF3SymTest(String testName) {
    super(testName);
  }

  protected void setUp() throws Exception {
  }

  protected void tearDown() throws Exception {
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(GFF3SymTest.class);
    
    return suite;
  }

  public void testGetSource() {
  }

  public void testGetFeatureType() {
  }

  public void testGetScore() {
  }

  public void testGetFrame() {
  }

  public void testGetAttributes() {
  }

  public void testGetProperty() {
  }

  public void testSetProperty() {
  }

  public void testGetProperties() {
  }

  public void testCloneProperties() {
  }

  public void testGetIdFromGFF3Attributes() {
    assertEquals("test1", GFF3Sym.getIdFromGFF3Attributes("test1;test2=foo;ID=test1"));
    assertEquals("test2", GFF3Sym.getIdFromGFF3Attributes("ID=test2"));
    assertEquals("test3", GFF3Sym.getIdFromGFF3Attributes("test1;test2=foo;ID=test3;"));
    assertEquals("test4", GFF3Sym.getIdFromGFF3Attributes("test1;;test2=foo;ID=test4;;;"));
    assertEquals("test5", GFF3Sym.getIdFromGFF3Attributes("test1;test2=foo;ID=test5;animals=cow,dog,rat"));
    assertEquals(null, GFF3Sym.getIdFromGFF3Attributes("test1;test2=foo;NotTheID=this is not the id;animals=cow,dog,rat"));
    assertEquals(null, GFF3Sym.getIdFromGFF3Attributes("test1;test2=foo;animals=cow,dog,rat"));
    assertEquals("This has&some special$characters", GFF3Sym.getIdFromGFF3Attributes("test1;ID=This%20has%26some+special%24characters;foo=bar"));
    String example = "ID=NC_000964.2:yaaA:unknown_transcript_1;Parent=NC_000964.2:yaaA;locus_tag=BSU00030;function=unknown;transl_table=11;product=hypothetical%20protein;protein_id=NP_387884.1;db_xref=GOA:P05650;db_xref=UniProtKB%2FSwiss-Prot:P05650;db_xref=GI:16077071;db_xref=GeneID:939444;exon_number=1";
    assertEquals("NC_000964.2:yaaA:unknown_transcript_1", GFF3Sym.getIdFromGFF3Attributes(example)); 
  }

  public void testAddAllAttributesFromGFF3() {
  }

  public void testGetGFF3PropertyFromAttributes() {
  }

  public void testMain() {
  }
  
}
