package com.affymetrix.genometryImpl.parsers.graph;

import org.junit.Test;
import static org.junit.Assert.*;

import com.affymetrix.genometryImpl.*;
import com.affymetrix.genometryImpl.symmetry.GraphSym;

import java.io.*;
import java.util.*;

/**
 *
 * @author jnicol
 */
public class CntParserTest {

	static GenometryModel gmodel = GenometryModel.getGenometryModel();

	@Test
	public void testParseFromFile() throws IOException {

		String filename = "test1.cnt";

		InputStream istr = CntParserTest.class.getClassLoader().getResourceAsStream(filename);
		DataInputStream dis = new DataInputStream(istr);
		assertNotNull(dis);

		CntParser cnt = new CntParser();

		AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");

		List<GraphSym> result = cnt.parse(dis, group, true);
		assertNotNull(result);
		assertEquals(2,result.size());

		assertEquals(5, result.get(0).getGraphXCoords().length);
		assertEquals(2224111, result.get(0).getGraphXCoord(0));
		assertEquals(1, result.get(1).getGraphXCoords().length);
		assertEquals(53452, result.get(1).getGraphXCoord(0));
		assertEquals(0.054294, result.get(0).getGraphYCoord(0),0.0001);
		assertEquals(10.051188, result.get(0).getGraphYCoord(4),0.0001);
	}

}
