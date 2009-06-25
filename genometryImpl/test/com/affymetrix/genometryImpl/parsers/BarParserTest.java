/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSymmetry;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.SmartAnnotBioSeq;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author auser
 */
public class BarParserTest {

/**
	@Test
	public void CreateBarFile() throws IOException {

		
		String filename = "test/data/bar/kim.bar";
		assertTrue(new File(filename).exists());

		InputStream istr = new FileInputStream(filename);
		assertNotNull(istr);
		//GenometryModel gmodel=new GenometryModel(){};
		SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
		AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
		boolean annot_seq = true;
		String stream_name = "test_file";
		//boolean ensure_unique_id = true;
		boolean ensure_unique_id = true;

		List<GraphSym> results = BarParser.parse(istr,gmodel,group,stream_name,ensure_unique_id);
		for (int i=0; i<results.size(); i++){
		GraphSym gr0 = (GraphSym) results.get(i);
		BioSeq seq = (BioSeq) gr0.getGraphSeq();
		//Collection<6> syms = (Collection<SeqSymmetry>) results.get(0);
		Collection<SeqSymmetry> syms = new ArrayList();
		syms.add(gr0);
		String type = "test_type";
		System.out.println("Success");
		/**
		for (int i=0; i<results.size(); i++) {
					SeqUtils.printSymmetry((SeqSymmetry) results.get(0), "|  ", true);
		}
		 * **/
	/**
	
		FileOutputStream fout;
    File file=new File("test"+i+".bar");

    fout = new FileOutputStream(file);
    BufferedOutputStream bos = new BufferedOutputStream(fout);


		DataOutputStream ostr =  new DataOutputStream(bos);

    BarParser instance=new BarParser();
		instance.writeAnnotations(syms,seq,type,ostr);
    ostr.close();
		}
		}
	**/
	 
  @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }
		@Test
	public void TestParseFromFile() throws IOException {


		String filename = "test/data/bar/test1.bar";

		assertTrue(new File(filename).exists());

		InputStream istr = new FileInputStream(filename);
		assertNotNull(istr);

		SingletonGenometryModel gmodel = SingletonGenometryModel.getGenometryModel();
		AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
		boolean annot_seq = true;
		String stream_name = "chr15_random";

		boolean ensure_unique_id = true;
		//SmartAnnotBioSeq seq = group.getSeq("chr15_random");
   // assertEquals("",seq);
		List<GraphSym> results = BarParser.parse(istr,gmodel,group,stream_name,ensure_unique_id);
		assertEquals(1, results.size());
		GraphSym gr0 = results.get(0);
		assertEquals(stream_name, gr0.getGraphSeq().getID());
		assertEquals(98, gr0.getPointCount());
		assertEquals(0, gr0.getGraphYCoord(2),0);
		assertEquals(0, gr0.getGraphYCoord(3),0.01);
		assertEquals(1879565, gr0.getGraphXCoords()[3]);

	}
		public void TestWriteAnnotations() throws IOException {
			String string =
						"chr15_random	1880113	0.23001233\n" +
						"chr15_random	1880219	0.21503295\n";

        InputStream istr = new ByteArrayInputStream(string.getBytes());

		AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("Test Group");
		boolean annot_seq = true;
		String stream_name = "test_file";
		boolean ensure_unique_id = true;

    List<GraphSym> results = SgrParser.parse(istr,stream_name,seq_group,ensure_unique_id);

    GraphSym gr0 = (GraphSym) results.get(0);
		BioSeq seq = (BioSeq) gr0.getGraphSeq();
		Collection<SeqSymmetry> syms = new ArrayList();
		syms.add(gr0);
		String type = "test_type";
		ByteArrayOutputStream outstream = new ByteArrayOutputStream();
		BarParser instance=new BarParser();
		boolean result1 =instance.writeAnnotations(syms,seq,type,outstream);
		assertEquals(true, result1);
		assertEquals(string, outstream.toString());



		}


}
