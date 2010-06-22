package com.affymetrix.genometryImpl.parsers.graph;

import com.affymetrix.genometryImpl.parsers.useq.*;
import com.affymetrix.genometryImpl.parsers.useq.data.RegionScoreTextData;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.*;
import com.affymetrix.genometryImpl.symloader.BED;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.UcscBedSym;

import java.io.*;
import java.util.*;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Nix
 */
public class UseqParserTest {


	@Test
	public void TestParseFromFile() throws IOException {
		
		//System.out.println("Graph file parsing tests.............");
		//testGraphFileParsing();
		
		//System.out.println("\nRegion file parsing tests.............");
		//testRegionFileParsing();
		
	}
	
	public void testRegionFileParsing() throws IOException{
		//test region parsing
		String stream_name = "chr17";
		String filename = "test/data/useq/chr17_H_sapiens_Mar_2006_Region.useq";
		assertTrue(new File(filename).exists());
		InputStream istr = new FileInputStream(filename);
		assertNotNull(istr);
		AnnotatedSeqGroup group = new AnnotatedSeqGroup("H_sapiens_Mar_2006");
		
		USeqRegionParser up = new USeqRegionParser();
		List<SeqSymmetry> results = up.parse(istr, group, stream_name, false, null);
		
		//size of results
		System.out.println("NumberRegions "+results.size());
		assertEquals(677, results.size());
		
		UcscBedSym sym = (UcscBedSym) results.get(5);
		SeqSpan span = sym.getSpan(0);
		
		//span count
		System.out.println("SpanCount "+sym.getSpanCount());
		assertEquals(1, sym.getSpanCount());
		
		//start
		System.out.println("SpanStart "+span.getMin());
		assertEquals(860852, span.getMin());
		
		//stop
		System.out.println("SpanStop "+span.getMax());
		assertEquals(861836, span.getMax());
		
		//strand
		System.out.println("SpanStrand "+span.isForward());
		assertEquals(true, span.isForward());
		
		//score
		System.out.println("SpanScore "+sym.getScore());
		assertEquals(new Float(401.0), new Float(sym.getScore()));
	}
	
	public void testGraphFileParsing() throws IOException{
		//test graph parsing
		String filename = "test/data/useq/chr17_H_sapiens_Mar_2006_Graph.useq";
		String stream_name = "chr17";
		
		assertTrue(new File(filename).exists());
		InputStream istr = new FileInputStream(filename);
		assertNotNull(istr);
		GenometryModel gmodel = GenometryModel.getGenometryModel();
		AnnotatedSeqGroup group = new AnnotatedSeqGroup("H_sapiens_Mar_2006");
		gmodel.addSeqGroup(group);
		gmodel.setSelectedSeqGroup(group);

		USeqGraphParser up = new USeqGraphParser();
		List<GraphSym> results = up.parseGraphSyms(istr, gmodel, "test", null);

		//size
		System.out.println("ResultsSize "+results.size());
		assertEquals(2, results.size());
		
		//fetch first graph
		GraphSym gr0 = results.get(0);
		
		//check stream name
		System.out.println("StreamName "+gr0.getGraphSeq().getID());
		assertEquals(stream_name, gr0.getGraphSeq().getID());
		
		//check point count
		System.out.println("PointCount "+gr0.getPointCount());
		assertEquals(20000, gr0.getPointCount());
		
		//check X coor
		System.out.println("XCoor "+gr0.getGraphXCoord(3));
		assertEquals(12168484, gr0.getGraphXCoord(3));
		
		//check Y coor
		System.out.println("YCoor "+gr0.getGraphYCoord(1));
		assertEquals(new Float(1), new Float(gr0.getGraphYCoord(1)));
	}
	
	@Test
	public void TestWriteAnnotations() throws Exception {
		//System.out.println("\nTesting writing Graph data to file..........");
		//testWritingGraphToFile();
		
		//System.out.println("\nTesting writing Regions to file..........");
		//testWritingRegionsToFile();
		
	}
	
	public void testWritingRegionsToFile() throws Exception {
		//read in data
		String filename = "test/data/useq/chr17_H_sapiens_Mar_2006_Region.useq";
		File regionFile = new File(filename);
		assertTrue(regionFile.exists());
		USeqArchive archive = new USeqArchive(regionFile);
		assertTrue(archive != null);
		
		//write some slices
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(baos);
		boolean wrote = archive.writeSlicesToStream(out, "chr18", 0, 100000, false);
		assertFalse(wrote);
		
		wrote = archive.writeSlicesToStream(out, "chr17", 860852, 861836, true);
		assertTrue(wrote);
		
	}
	
	public void testWritingGraphToFile() throws Exception {
		//read in data
		String filename = "test/data/useq/chr17_H_sapiens_Mar_2006_Graph.useq";
		File file = new File(filename);
		assertTrue(file.exists());
		USeqArchive archive = new USeqArchive(file);
		assertTrue(archive != null);
		
		//write some slices
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(baos);
		boolean wrote = archive.writeSlicesToStream(out, "chr17", 23000000, 24000000, false);
		assertFalse(wrote);
		
		wrote = archive.writeSlicesToStream(out, "chr17", 13000000, 14000000, true);
		assertTrue(wrote);
		
	}

}





