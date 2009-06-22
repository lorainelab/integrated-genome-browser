/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.genometryImpl.parsers;


import com.affymetrix.genometryImpl.util.FloatList;

import com.affymetrix.genometryImpl.util.IntList;

import java.util.Map;

import java.util.HashMap;



import com.affymetrix.genometryImpl.GraphSym;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GraphSymFloat;
import java.io.ByteArrayInputStream;


import com.affymetrix.genometryImpl.AnnotatedSeqGroup;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author auser
 */
public class BgrParserTest {

/**
    public BgrParserTest() {

    }
 * **/
    
     @Test
     //Creates a Bgr format file
    public void CreateBgrFile() throws IOException {

		String string =
						"16	948025	0.128646\n" +
						"16	948026	0.363933\n";

        InputStream istr = new ByteArrayInputStream(string.getBytes());

		AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("Test Group");
		boolean annot_seq = true;
		String stream_name = "test_file";
		boolean ensure_unique_id = true;
       

         List<GraphSym> results = SgrParser.parse(istr,stream_name,seq_group,ensure_unique_id);
         
           FileOutputStream fout;
           File file=new File("test1.bgr");
            
            fout = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(fout);

           
            DataOutputStream dos =  new DataOutputStream(bos);

            BgrParser.writeBgrFormat(results.get(0), dos);
            dos.close();
         }



    @Before
    public void setUp() {
     
    }

    @After
    public void tearDown() {
    }
    
    @Test
    public void  testParseFromFile() throws Exception {

        String filename = "test/data/bgr/test1.bgr";
		assertTrue(new File(filename).exists());

		InputStream istr = new FileInputStream(filename);
		assertNotNull(istr);
       
        String stream_name = "test_file";
		AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("Test Group");
		boolean annot_seq = true;
		
		boolean ensure_unique_id = true;

		GraphSymFloat gr0 =BgrParser.parse(istr,stream_name,seq_group,ensure_unique_id);
        istr.close();
        //List<GraphSym> results = SgrParser.parse(istr, stream_name, group, annot_seq, ensure_unique_id);

		//assertEquals(1, results.size());
		//GraphSym gr0 = results.get(0);

		assertEquals("16", gr0.getGraphSeq().getID());
		assertEquals(2, gr0.getPointCount());
		assertEquals(0.128646, gr0.getGraphYCoord(0), 0.01);
		assertEquals(0.363933, gr0.getGraphYCoord(1), 0.01);
		assertEquals(948026, gr0.getGraphXCoords()[1]);

        
		
       
    }


 }