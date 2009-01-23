/*
 * Das2ServerInfoTest.java
 * JUnit based test
 *
 * Created on April 26, 2006, 3:08 PM
 */

package com.affymetrix.igb.das2;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.net.URISyntaxException;
import java.util.*;

/**
 *
 * @author boconnor
 */
public class Das2ServerInfoTest {
    ArrayList serverInfos;
    
    public Das2ServerInfoTest() {
    }

    @Before
    public void setUp() throws URISyntaxException {
			String[] test_urls = { "http://netaffxdas.affymetrix.com/das2/genome" };
      serverInfos = new ArrayList();
      for (String url : test_urls) {
		// Commented out because add accesses the network
        /* serverInfos.add(new Das2ServerInfo(url, "DAS2 Server", true)); */
      }
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of getSources method, of class com.affymetrix.igb.das2.Das2ServerInfo.
     */
    @Test
    public void testGetSources() {
        fail("test needs to be re-implemented so it does not use the network");
      Iterator it = serverInfos.iterator();
      while (it.hasNext()) {
        Das2ServerInfo serverInfo = (Das2ServerInfo)it.next();
        System.out.println("***** DAS2 Server Info *****");
        System.out.println("  Root URL: " + serverInfo.getID());
        System.out.println("  DAS2 version: " + serverInfo.getDasVersion());
        assertNotNull(serverInfo.getID());
				/* DAS/2 Does not require a version string */
        /* assertNotNull(serverInfo.getDasVersion()); */
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
  }
}
