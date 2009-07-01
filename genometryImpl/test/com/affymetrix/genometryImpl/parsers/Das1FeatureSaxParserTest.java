/*
 * Das1FeatureSaxParserTest.java
 * JUnit based test
 *
 * Created on October 6, 2006, 3:19 PM
 */

package com.affymetrix.genometryImpl.parsers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.*;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SymWithProps;

/**
 *
 * @author Ed Erwin
 */
public class Das1FeatureSaxParserTest {
	public static final String test_file_name_1 = "test/data/das1/das1-sample-hg18.dasxml";
	public static final String test_file_name_2 = "test/data/das1/das1-sample-hg10.dasxml";

	public Das1FeatureSaxParserTest() {
	}

	@Before
		public void setUp() {
		}

	@After
		public void tearDown() {
		}

	/**
	 * Tests the parsing of the <LINK> elements
	 */
	@Test
		public void testLinks() {
			InputStream istr = null;
			assertTrue(new File(test_file_name_1).exists());

			AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
			Das1FeatureSaxParser parser = new Das1FeatureSaxParser();

			List results = null;
			try {
				istr = new FileInputStream(test_file_name_1);
				assertNotNull(istr);

				results = parser.parse(istr, group);
			} catch (IOException ioe) {
				ioe.printStackTrace();
				fail("Failed due to Exception: " + ioe.toString());
			} finally {
				if (istr != null) {
					try {
						istr.close();
					} catch (IOException e) {
						e.printStackTrace();
						fail("Failed due to Exception: " + e.toString());
					}
				}
			}

			assertEquals(32, results.size());

			SymWithProps sym_7 = (SymWithProps) results.get(7);
			String link_7 = (String) sym_7.getProperty("link");
			String link_name_7 = (String) sym_7.getProperty("link_name");
			assertEquals("http://genome.ucsc.edu/cgi-bin/hgTracks?position=chr3:73089142-73107313&db=hg18", link_7);
			assertEquals("Link to UCSC Browser", link_name_7);
			assertEquals("affyU133Plus2", sym_7.getProperty("method"));
			assertEquals("235371_at.chr3.73089142", sym_7.getProperty("id"));
			assertEquals(4, sym_7.getChildCount());
		}

	/**
	 * Tests the parsing of a simple example file
	 */
	@Test
		public void testParse() {
			InputStream istr = null;
			assertTrue(new File(test_file_name_2).exists());

			AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
			Das1FeatureSaxParser parser = new Das1FeatureSaxParser();

			List results = null;
			try {
				istr = new FileInputStream(test_file_name_2);
				assertNotNull(istr);

				results = parser.parse(istr, group);
			} catch (IOException ioe) {
				ioe.printStackTrace();
				fail("Failed due to Exception: " + ioe.toString());
			} finally {
				if (istr != null) {
					try {
						istr.close();
					} catch (IOException e) {
						e.printStackTrace();
						fail("Failed due to Exception: " + e.toString());
					}
				}
			}

			assertEquals(1, results.size());

			SymWithProps sym = (SymWithProps) results.get(0);

			//SeqUtils.printSymmetry(sym, "  ", true);

			assertEquals(1, sym.getSpanCount());
			assertEquals(46, sym.getSpan(0).getLength());
			assertEquals("Em:D87024.C22.12.chr22.20012405", sym.getID());
			assertEquals("sanger22", sym.getProperty("method"));

			// The label is actually ignored by our parser, but why?
			//assertEquals("Em:D87024.C22.12", sym.getProperty("label"));

			// There is more than one link, so it gets put in a Map

			Map<String,String> links = (Map<String,String>) sym.getProperty("link");
			assertEquals(2, links.size());

			String key1 = "Link to UCSC Browser";
			String key2 = "Link to UCSC Browser for the feature";
			assert(links.containsKey(key1));
			assert(links.containsKey(key2));

			// Currently it is possible for multiple keys to map to the same URL.  I'd like to change that.
			assertEquals("http://genome.ucsc.edu/cgi-bin/hgTracks?position=chr22:20012405-20012900&db=hg10", links.get(key1));
			assertEquals("http://genome.ucsc.edu/cgi-bin/hgTracks?position=chr22:20012405-20012900&db=hg10", links.get(key2));    
		}
}
