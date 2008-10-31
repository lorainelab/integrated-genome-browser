/*
 * Das2AssayServerInfoTest.java
 * JUnit based test
 *
 * Created on April 27, 2006, 9:18 PM
 */

package com.affymetrix.igb.das2.assay;

import junit.framework.*;
import com.affymetrix.igb.das2.*;
import java.util.*;

/**
 *
 * @author boconnor
 */
public class Das2AssayServerInfoTest extends TestCase {
  
  ArrayList serverInfos;
  
  public Das2AssayServerInfoTest(String testName) {
    super(testName);
  }

  protected void setUp() throws Exception {
    //String[] test_urls = {"http://das.biopackages.net/das/assay"};
    String[] test_urls = {"http://humerus.ctrl.ucla.edu:8529/das/assay"};
    serverInfos = new ArrayList();
    for (int i=0; i<test_urls.length; i++) {
      // FIXME: the following uses a hard-coded ontology URL
      serverInfos.add(new Das2AssayServerInfo(test_urls[i], "DAS2 Server", true, test_urls[i]+"/../ontology/obo/1/ontology"));
    }
  }

  protected void tearDown() throws Exception {
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(Das2AssayServerInfoTest.class);
    
    return suite;
  }

  /**
   * Test of getRootOntologyUrl method, of class com.affymetrix.igb.das2.assay.Das2AssayServerInfo.
   */
  public void testGetRootOntologyUrl() {
    System.out.println("testGetRootOntologyUrl");
    
    // TODO add your test code below by replacing the default call to fail.
    fail("The test case is empty.");
  }
  
  /**
   * Test of getSources method, of class com.affymetrix.igb.das2.Das2ServerInfo.
   */
  public void testGetSources() {
    Iterator it = serverInfos.iterator();
    while (it.hasNext()) {
      Das2AssayServerInfo serverInfo = (Das2AssayServerInfo)it.next();
      System.out.println("***** DAS2 Server Info *****");
      System.out.println("  Root URL: " + serverInfo.getID());
      System.out.println("  DAS2 version: " + serverInfo.getDasVersion());
      assertNotNull(serverInfo.getID());
      assertNotNull(serverInfo.getDasVersion());
      Iterator sources = serverInfo.getSources().values().iterator();
      assertTrue(sources.hasNext());
      System.out.println("  Data sources: ");
      while (sources.hasNext()) {
        Das2AssaySource source = (Das2AssaySource)sources.next();
        System.out.println("     id = " + source.getID() +
                           ", description = " + source.getDescription() +
                           ", info_url = " + source.getInfoUrl() +
                           ", taxon = " + source.getTaxon());
        assertNotNull(source.getID());
        Iterator versions = source.getVersions().values().iterator();
        assertTrue(versions.hasNext());
        while (versions.hasNext()) {
          System.out.println("     Versions");
          Das2AssayVersionedSource version = (Das2AssayVersionedSource)versions.next();
          System.out.println("          Version id = " + version.getID());
          assertNotNull(version.getID());
          
          System.out.println("            Materials:");
          Map materials = version.getMaterials();
          Iterator matIt = materials.values().iterator();
          assertTrue(matIt.hasNext());
          while(matIt.hasNext()) {
            Das2Material material = (Das2Material)matIt.next();
            System.out.println("            material id: "+material.id+", "+material.name);
            assertNotNull(material.id);
          }
           
          System.out.println("            Results:");
          Map results = version.getResults();
          Iterator rIt = results.values().iterator();
          assertTrue(rIt.hasNext());
          while(rIt.hasNext()) {
            Das2Result result = (Das2Result)rIt.next();
            System.out.println("            result id: "+result.id+", "+result.getProtocolId());
            assertNotNull(result.id);
          }
           
          System.out.println("            Assays:");
          Map assays = version.getAssays();
          Iterator aIt = assays.values().iterator();
          assertTrue(aIt.hasNext());
          while(aIt.hasNext()) {
            Das2Assay assay = (Das2Assay)aIt.next();
            System.out.println("              assay id: "+assay.getID());
            assertNotNull(assay.getID());
          }
          
          System.out.println("            Platforms:");
          LinkedList platforms = version.getPlatforms();
          Iterator pIt = platforms.listIterator();
          assertTrue(pIt.hasNext());
          while(pIt.hasNext()) {
            Das2Platform platform = (Das2Platform)pIt.next();
            System.out.println("            platform id: "+platform.getPlatform());
            assertNotNull(platform.getPlatform());
          }
          
          // Now for types!
          System.out.println("            Types:");
          Map types = version.getTypes("MA");
          Iterator tIt = types.values().iterator();
          while(tIt.hasNext()) {
            Das2Type type = (Das2Type)tIt.next();
            System.out.println("            type id: "+type.getID());
            assertNotNull(type.getID());
          }
          
        }
      }
    }
  }
}
