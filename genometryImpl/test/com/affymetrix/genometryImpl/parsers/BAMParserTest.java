package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SymWithProps;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jnicol
 */
public class BAMParserTest {

	@Test
		public void testParseFromFile() throws IOException {

			String filename = "test/data/bam/combined_mapping_q.sorted.bam";
			assertTrue(new File(filename).exists());

			InputStream istr = new FileInputStream(filename);
			DataInputStream dis = new DataInputStream(istr);
			assertNotNull(dis);

			AnnotatedSeqGroup group = new AnnotatedSeqGroup("M_musculus_Mar_2006");
			BioSeq seq = group.addSeq("chr1", 197069962);
			
			BAMParser parser = new BAMParser(new File(filename).toURI(), "featureName", group);
			assertNotNull(parser);

			List<SeqSymmetry> result = parser.parse(seq, seq.getMin(), seq.getMax(), false, true);
			assertNotNull(result);
			assertEquals(189, result.size());	// 189 alignments in sample file

			SeqSymmetry sym = result.get(0);	// first (positive) strand
			assertEquals("0", sym.getID());
			assertEquals(51119999, sym.getSpan(seq).getMin());
			assertEquals(51120035, sym.getSpan(seq).getMax());
			assertTrue(sym.getSpan(seq).isForward());
			Object residues = ((SymWithProps)sym).getProperty("residues");
			assertNotNull(residues);
			assertEquals("CACTGCTTAAAAATCCTCTTTAAAGAGCAAGCAAAT",residues.toString());

			sym = result.get(58);	// first negative strand
			assertEquals("0", sym.getID());
			assertEquals(51120691, sym.getSpan(seq).getMin());
			assertEquals(51120727, sym.getSpan(seq).getMax());
			assertFalse(sym.getSpan(seq).isForward());
			residues = ((SymWithProps)sym).getProperty("residues");
			assertNotNull(residues);
			assertEquals("ACATTCATCTAATTCATGAATGGCAAAGACACGTCC",residues.toString());

			// Strange, considering the span length is only 36
			result = parser.parse(seq, 51120000, 51120038, false, true);
			assertEquals(0, result.size());

			result = parser.parse(seq, 51120000, 51120039, false, true);
			assertEquals(1, result.size());
			sym = result.get(0);	// first (positive) strand
			assertEquals("0", sym.getID());
			assertEquals(51119999, sym.getSpan(seq).getMin());
			assertEquals(51120035, sym.getSpan(seq).getMax());


			for(SeqSymmetry symOut : result) {
				System.out.println(symOut.getID() + " " + symOut.getSpan(seq).getStart() + " " + symOut.getSpan(seq).getEnd());
			}
		}

}
