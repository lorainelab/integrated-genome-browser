package com.affymetrix.genometryImpl.util;

import java.util.Set;
import com.affymetrix.genometryImpl.SeqSymmetry;
import java.util.regex.Pattern;
import com.affymetrix.genometryImpl.GenometryModel;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.File;
import com.affymetrix.genometryImpl.parsers.PSLParser;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.UcscPslSym;
import java.util.logging.Level;
import java.util.List;
import java.util.logging.Logger;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jnicol
 *
 * Verify that searches (locally and on server) return correct results.
 */
public class SearchUtilsTest {
	File f = null;
	AnnotatedSeqGroup group = GenometryModel.getGenometryModel().addSeqGroup("searchGroup");
	List<UcscPslSym> syms = null;
	Pattern regex = Pattern.compile(".*EG510482.*", Pattern.CASE_INSENSITIVE);

	@Before
	public void setUp() {
		assertNotNull(group);
		
		DataInputStream dis = null;
		try {
			String filename = "test/data/psl/search.psl";
			// load in test file.
			f = new File(filename);
			assertTrue(f.exists());
			PSLParser instance = new PSLParser();
			dis = new DataInputStream(new FileInputStream(f));
			syms = instance.parse(dis, "SearchTest", null, group, false, true);
			assertEquals(46, syms.size());
			assertEquals(5,group.getSeqCount());
			assertEquals("5", group.getSeq(0).getID());

		} catch (Exception ex) {
			Logger.getLogger(SearchUtilsTest.class.getName()).log(Level.SEVERE, null, ex);
			fail();
		} finally {
			GeneralUtils.safeClose(dis);
		}
	}

	@Test
	public void testLocalSearch() {
		List<SeqSymmetry> foundSyms = SearchUtils.findLocalSyms(group, null, regex);
		assertEquals(46, foundSyms.size());
	}

	@Test
	public void testNonIndexedServerSearch() {
		Set<SeqSymmetry> foundSyms = SearchUtils.findNameInGenome(".*EG510482.*", group);
		assertEquals(0, foundSyms.size());
		
	}

	/*
	@Test
	public void testIndexedServerSearch() {
		Set<SeqSymmetry> foundSyms = SearchUtils.findNameInGenome(".*EG510482.*", group);
		assertEquals(0, foundSyms.size());

	}*/
}
