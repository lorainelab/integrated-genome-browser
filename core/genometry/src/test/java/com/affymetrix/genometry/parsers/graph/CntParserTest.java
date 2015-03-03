package com.affymetrix.genometry.parsers.graph;

import com.affymetrix.genometry.AnnotatedSeqGroup;
import com.affymetrix.genometry.GenometryModel;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 *
 * @author jnicol
 */
public class CntParserTest {

    static GenometryModel gmodel = GenometryModel.getInstance();

    @Test
    public void testParseFromFile() throws IOException {

        String filename = "data/cnt/test1.cnt";

        InputStream istr = CntParserTest.class.getClassLoader().getResourceAsStream(filename);
        DataInputStream dis = new DataInputStream(istr);
        assertNotNull(dis);

        CntParser cnt = new CntParser();

        AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");

        List<GraphSym> result = cnt.parse(dis, group, true);
        assertNotNull(result);
        assertEquals(2, result.size());

        assertEquals(5, result.get(0).getGraphXCoords().length);
        assertEquals(2224111, result.get(0).getGraphXCoord(0));
        assertEquals(1, result.get(1).getGraphXCoords().length);
        assertEquals(53452, result.get(1).getGraphXCoord(0));
        assertEquals(0.054294, result.get(0).getGraphYCoord(0), 0.0001);
        assertEquals(10.051188, result.get(0).getGraphYCoord(4), 0.0001);
    }

}
