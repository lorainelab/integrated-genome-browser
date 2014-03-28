package com.affymetrix.genometry.util;

import com.affymetrix.genometry.Das2AnnotatedSeqGroup;
import java.util.Set;
import java.util.regex.Pattern;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.util.logging.Level;
import java.util.List;
import java.util.logging.Logger;
import com.affymetrix.genometryImpl.parsers.IndexWriter;
import com.affymetrix.genometryImpl.parsers.PSLParser;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.UcscPslSym;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

/**
 *
 * @author jnicol
 *
 * Verify that searches (locally and on server) return correct results.
 */
@Ignore
public class SearchUtilsTest {
	File f = null;
	Das2AnnotatedSeqGroup group = new Das2AnnotatedSeqGroup("searchGroup");
	List<UcscPslSym> syms = null;
	Pattern regex = Pattern.compile(".*EG510482.*", Pattern.CASE_INSENSITIVE);
	IndexWriter iWriter = null;

	@Before
	public void setUp() {
		GenometryModel.getGenometryModel().addSeqGroup(group);
		assertNotNull(group);
		
		DataInputStream dis = null;
		try {
			String filename = SearchUtilsTest.class.getClassLoader().getResource("search.psl").getFile();
			// load in test file.
			f = new File(filename);
			assertTrue(f.exists());
			iWriter = new PSLParser();
			dis = new DataInputStream(new FileInputStream(f));
			syms = ((PSLParser)iWriter).parse(dis, "SearchTest", null, group, false, true);
			assertEquals(46, syms.size());
			assertEquals(5,group.getSeqCount());
			assertEquals("5", group.getSeq(0).getID());
			for(SeqSymmetry sym : syms){
				group.addToIndex(sym.getID(), sym);
			}
		} catch (Exception ex) {
			Logger.getLogger(SearchUtilsTest.class.getName()).log(Level.SEVERE, null, ex);
			fail();
		} finally {
			GeneralUtils.safeClose(dis);
		}
	}

	@Test
	public void testLocalSearch() {
		Set<SeqSymmetry> foundSyms = group.findSyms(regex);
		assertEquals(46, foundSyms.size());
	}

	@Test
	public void testNonIndexedServerSearch() {
		Set<SeqSymmetry> foundSyms = IndexingUtils.findNameInGenome(".*EG510482.*", group);
		assertEquals(0, foundSyms.size());
		
	}

	@Test
	public void testIndexedServerSearch() {
		try {
			Set<SeqSymmetry> foundSyms = null;
			foundSyms = IndexingUtils.findSymsByName(group, regex);
			assertEquals(0, foundSyms.size());

			// Need to index information
			AnnotatedSeqGroup tempGroup = Das2AnnotatedSeqGroup.tempGenome(group);
			assertEquals(group.getSeqCount(), tempGroup.getSeqCount());
			
			List loadedSyms = Das2ServerUtils.loadAnnotFile(f, "indexPSL", null, tempGroup, true);
			assertEquals(46, loadedSyms.size());

			/*IndexingUtils.determineIndexes(group, tempGroup, System.getProperty("user.dir"), f, loadedSyms, iWriter, "indexPSL", "indexPSL");
			
			foundSyms = IndexingUtils.findSymsByName(tempGroup, regex);
			assertEquals(0, foundSyms.size());*/

		} catch (Exception ex) {
			Logger.getLogger(SearchUtilsTest.class.getName()).log(Level.SEVERE, null, ex);
			fail();
		}

	}
}
