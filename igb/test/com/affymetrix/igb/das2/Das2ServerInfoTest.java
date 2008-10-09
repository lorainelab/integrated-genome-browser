/*
 * Das2ServerInfoTest.java
 * JUnit based test
 *
 * Created on April 26, 2006, 3:08 PM
 */

package com.affymetrix.igb.das2;

import junit.framework.*;
import java.util.*;

/**
 *
 * @author boconnor
 */
public class Das2ServerInfoTest extends TestCase {
    
    ArrayList serverInfos;
    
    public Das2ServerInfoTest(String testName) {
 //       super(testName);
    }

    protected void setUp() throws Exception {
/*
      String[] test_urls = {"http://das.biopackages.net/das/genome", "http://205.217.46.81:9091/das2/genome/sequence"};
      serverInfos = new ArrayList();
      for (int i=0; i<test_urls.length; i++) {
        serverInfos.add(new Das2ServerInfo(test_urls[i], "DAS2 Server", true));
      }
*/
    }

    protected void tearDown() throws Exception {
    }

    public static Test suite() {
        TestSuite suite = new TestSuite(Das2ServerInfoTest.class);
        
        return suite;
    }

    /**
     * Test of getSources method, of class com.affymetrix.igb.das2.Das2ServerInfo.
     */
    public void testGetSources() {
    /*  Iterator it = serverInfos.iterator();
      while (it.hasNext()) {
        Das2ServerInfo serverInfo = (Das2ServerInfo)it.next();
        System.out.println("***** DAS Server Info *****");
        System.out.println("  Root URL: " + serverInfo.getID());
        System.out.println("  DAS version: " + serverInfo.getDasVersion());
        assertNotNull(serverInfo.getID());
        assertNotNull(serverInfo.getDasVersion());
        Iterator sources = serverInfo.getSources().values().iterator();
        assertTrue(sources.hasNext());
        System.out.println("  Data sources: ");
        while (sources.hasNext()) {
          Das2Source source = (Das2Source)sources.next();
          System.out.println("     id = " + source.getID() +
                             ", description = " + source.getDescription() +
                             ", info_url = " + source.getInfoUrl() +
                             ", taxon = " + source.getTaxon());
          assertNotNull(source.getID());
          Iterator versions = source.getVersions().values().iterator();
          assertTrue(versions.hasNext());
          while (versions.hasNext()) {
            System.out.println("     Versions");
            Das2VersionedSource version = (Das2VersionedSource)versions.next();
            System.out.println("          Version id = " + version.getID());
            System.out.println("          AnnotatedSeqGroup: " + version.getGenome().getID());
            assertNotNull(version.getID());
            
            //regions
            Map regions = version.regions;
            Iterator riter = regions.values().iterator();
            while (riter.hasNext()) {
              Das2Region region = (Das2Region)riter.next();
              //MutableAnnotatedBioSeq aseq = region.getAnnotatedSeq();
              System.out.println("          Region name: "+region.getName());
            }
            
            //types
            Map types = version.getTypes();
            Iterator typesIt = types.values().iterator();
            while(typesIt.hasNext()) {
              Das2Type type = (Das2Type)typesIt.next();
              System.out.println("          Type name: "+type.getName());
            }
          }
       }
    }
 */
  }
}
