package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.parsers.ChromInfoParser;
import com.affymetrix.genometryImpl.parsers.PSLParser;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jnicol
 */
public class ServerUtilsTest {
	private static SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
	private static String baseDir = "test/data/server/A_thaliana";
	private static String versionString = "A_thaliana_TAIR8";
	AnnotatedSeqGroup genome = null;

	@Before
	public void setUp() {
		InputStream chromstream = null;
		InputStream istr = null;
		try {
			// Load chromosomes
			File chrom_info_file = new File(baseDir + "/" + versionString + "/mod_chromInfo.txt");
			chromstream = new FileInputStream(chrom_info_file);
			ChromInfoParser.parse(chromstream, gmodel, versionString);
			
			// Load genome
			genome = gmodel.getSeqGroup(versionString);
			String stream_name = baseDir + "/" + versionString + "/mRNA1.mm.psl";
			File current_file = new File(stream_name);
			istr = new BufferedInputStream(new FileInputStream(current_file));

			PSLParser parser = new PSLParser();
			parser.setTrackNamePrefix("blah");
			parser.setCreateContainerAnnot(true);
			parser.parse(istr, "mRNA1.sm", null, genome, null, false, true, false);  // annotate target

			// optimize genome by replacing second-level syms with IntervalSearchSyms
			Optimize.Genome(genome);

		} catch (Exception ex) {
			Logger.getLogger(ServerUtilsTest.class.getName()).log(Level.SEVERE, null, ex);
			fail();
		} finally {
			GeneralUtils.safeClose(istr);
			GeneralUtils.safeClose(chromstream);
		}
	}

	@Test
	public void testGenome() {
		assertNotNull(genome);
		assertEquals("A_thaliana_TAIR8", genome.getID());
		assertEquals(7, genome.getSeqCount());
		BioSeq seq = genome.getSeq("chr1");
		assertNotNull(seq);
	}
	
	/*@Test
	public void testOutputSpan() {
		String seqid="";
		String query_type="";
		String overlap = null;
		List<SeqSymmetry> result = null;
		AnnotatedSeqGroup genome = null;

		SeqSpan overlap_span = ServerUtils.getLocationSpan(seqid, overlap, genome);
		assertNotNull(overlap_span);

		result = ServerUtils.getIntersectedSymmetries(overlap_span, query_type);
	}*/

	/*@Test
	public void testOutputSpanWithInsideSpan() {
		String seqid="";
		String query_type="";
		String overlap = null;
		List<SeqSymmetry> result = null;
		AnnotatedSeqGroup genome = null;

		SeqSpan overlap_span = ServerUtils.getLocationSpan(seqid, overlap, genome);
		assertNotNull(overlap_span);

		result = ServerUtils.getIntersectedSymmetries(overlap_span, query_type);

		String inside="";
		SeqSpan inside_span = ServerUtils.getLocationSpan(seqid, inside, genome);

		assertNotNull(inside_span);

		BioSeq oseq = (BioSeq) overlap_span.getBioSeq();
		result = ServerUtils.SpecifiedInsideSpan(inside_span, oseq, result, query_type);
	}
*/
}
