/*
 * WiggleParserTest.java
 * JUnit based test
 *
 * Created on October 18, 2006, 2:36 PM
 */
package com.affymetrix.genometryImpl.parsers.graph;

import org.junit.Test;
import static org.junit.Assert.*;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GraphIntervalSym;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.Scored;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.style.GraphState;
import java.io.*;
import java.util.*;

/**
 *
 * @author Ed Erwin
 */
public class WiggleParserTest {

	public WiggleParserTest() {
	}

	@Test
	public void testParse() throws Exception {
		String filename = "test/data/wiggle/wiggleExample.wig";
		assertTrue(new File(filename).exists());

		InputStream istr = new FileInputStream(filename);
		assertNotNull(istr);

		AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("test");

		WiggleParser parser = new WiggleParser();

		List<GraphSym> results = parser.parse(istr, seq_group, true, filename);

		assertEquals(3, results.size());

		GraphSym gr0 = results.get(0);

		BioSeq seq = gr0.getGraphSeq();

		// BED format
		assertTrue(gr0 instanceof GraphIntervalSym);
		assertEquals("chr19", gr0.getGraphSeq().getID());
		assertEquals(9, gr0.getPointCount());
		assertEquals(59302000, gr0.getSpan(seq).getMin());
		assertEquals(59304700, gr0.getSpan(seq).getMax());

		// variableStep format
		GraphSym gr1 = results.get(1);
		assertTrue(gr1 instanceof GraphIntervalSym);
		assertEquals(9, gr1.getChildCount());
		assertTrue(gr1.getChild(0) instanceof Scored);
		assertEquals(59304701 - 1, gr1.getSpan(seq).getMin());	// variableStep: 1-relative format
		assertEquals(59308021 - 1, gr1.getSpan(seq).getMax());	// variableStep: 1-relative foramt

		// fixedStep format
		GraphSym gr2 = results.get(2);
		assertTrue(gr2 instanceof GraphIntervalSym);
		assertEquals(10, gr2.getChildCount());
		assertEquals(59307401 - 1, gr2.getSpan(seq).getMin());			// fixedStep: 1-relative format
		assertEquals(59310301 - 1, gr2.getSpan(seq).getMax());			// fixedStep: 1-relative format
		assertEquals(300.0f, ((Scored) gr2.getChild(7)).getScore(), 0.00000001);

		assertEquals("Bed Format", gr0.getID());
		assertEquals("variableStep", gr1.getID());
		assertEquals("fixedStep", gr2.getID());

		GraphState state = gr1.getGraphState();
		assertEquals(0.0, state.getVisibleMinY(), 0.00001);
		assertEquals(25.0, state.getVisibleMaxY(), 0.00001);

		assertEquals(59310301 - 1, seq.getLength());	// fixedStep was 1-relative format.

	}

	/*
	@Test
	public void testWriteGraphs() {
	fail("test not implemented");
	}
	 */
}
