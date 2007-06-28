/*
 * Das2OntologyServerInfoTest.java
 * JUnit based test
 *
 * Created on April 27, 2006, 3:07 PM
 */

package com.affymetrix.igb.das2.ontology;

import junit.framework.*;
import java.util.*;

/**
 *
 * @author boconnor
 */
public class Das2OntologyServerInfoTest extends TestCase {
  
  ArrayList serverInfos;
  
  public Das2OntologyServerInfoTest(String testName) {
    super(testName);
  }

  protected void setUp() throws Exception {
      String[] test_urls = {"http://das.biopackages.net/das/ontology"};
      serverInfos = new ArrayList();
      for (int i=0; i<test_urls.length; i++) {
        serverInfos.add(new Das2OntologyServerInfo(test_urls[i], "DAS2 Server", true));
      }
  }

  protected void tearDown() throws Exception {
  }

  public static Test suite() {
    TestSuite suite = new TestSuite(Das2OntologyServerInfoTest.class);
    
    return suite;
  }
  
    /**
     * Test of getSources method, of class com.affymetrix.igb.das2.Das2ServerInfo.
     */
    public void testGetSources() {
      Iterator it = serverInfos.iterator();
      while (it.hasNext()) {
        Das2OntologyServerInfo serverInfo = (Das2OntologyServerInfo)it.next();
        System.out.println("***** DAS Server Info *****");
        System.out.println("  Root URL: " + serverInfo.getID());
        System.out.println("  DAS version: " + serverInfo.getDasVersion());
        assertNotNull(serverInfo.getID());
        assertNotNull(serverInfo.getDasVersion());
        Iterator sources = serverInfo.getSources().values().iterator();
        assertTrue(sources.hasNext());
        System.out.println("  Data sources: ");
        while (sources.hasNext()) {
          Das2OntologySource source = (Das2OntologySource)sources.next();
          System.out.println("     id = " + source.getID() +
                             ", description = " + source.getDescription() +
                             ", info_url = " + source.getInfoUrl() +
                             ", taxon = " + source.getTaxon());
          assertNotNull(source.getID());
          Iterator versions = source.getVersions().values().iterator();
          assertTrue(versions.hasNext());
          while (versions.hasNext()) {
            System.out.println("     Versions");
            Das2OntologyVersionedSource version = (Das2OntologyVersionedSource)versions.next();
            System.out.println("          Version id = " + version.getID());
            assertNotNull(version.getID());
            Map types = version.getTypes("MA");
          }
       }
    }
  }
  
}
