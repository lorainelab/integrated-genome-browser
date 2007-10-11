package com.affymetrix.genometry.servlets;

import java.io.*;
import java.util.*;
import com.affymetrix.igb.util.GraphSymUtils;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.parsers.*;


/**
 *  Trying to make a central repository for parsers.
 */
public class ParserController {
  //  private static SearchAllSeqSymmetry globalSeqSymmetryIndex = new SearchAllSeqSymmetry();

  //  setCreateContainerAnnotation()
  //

  public static List parse(InputStream instr, String stream_name, GenometryModel gmodel, AnnotatedSeqGroup seq_group) {
    InputStream str = null;
    List results = null;
    try {
      if (instr instanceof BufferedInputStream)  {
	str = (BufferedInputStream)instr;
      }
      else {

	str = new BufferedInputStream(instr);
      }
      if (stream_name.endsWith(".das") || stream_name.endsWith(".dasxml")) {
	System.out.println("loading via Das1FeatureSaxParser: " + stream_name);
	Das1FeatureSaxParser parser = new Das1FeatureSaxParser();
	// need to modify Das1FeatureSaxParser to return a list of "transcript-level" annotation syms
        parser = null;
      }
      else if (stream_name.endsWith(".psl") || stream_name.endsWith( ".psl3")) {
	System.out.println("loading via PslParser: " + stream_name);
	String annot_type = stream_name.substring(0, stream_name.lastIndexOf(".psl"));
	// assume it's PSL format
	// assume that want to annotate target seqs, and that these are the seqs
	//    represented in seq_group
	PSLParser parser = new PSLParser();
        if (stream_name.endsWith(".link.psl")) {
          annot_type = stream_name.substring(0, stream_name.lastIndexOf(".link.psl"));
	  parser.setIsLinkPsl(true);
          parser.enableSharedQueryTarget(true);
        }
	parser.setCreateContainerAnnot(true); // is this needed?
	//	parser.setCreateContainerAnnot(false); // is this needed?
	results = parser.parse(str, annot_type, null, seq_group, null, false, true, false);  // annotate target
      }
      else if (stream_name.endsWith(".bed")) {
	System.out.println("loading via BedParser: " + stream_name);
	String annot_type = stream_name.substring(0, stream_name.lastIndexOf(".bed"));
	BedParser parser = new BedParser();
	// specifying via boolean arg that BedParser should build container syms
	results = parser.parse(str, gmodel, seq_group, true, annot_type, true);
      }
      else if (stream_name.endsWith(".bps")) {
	System.out.println("loading via BpsParser: " + stream_name);
	String annot_type = stream_name.substring(0, stream_name.lastIndexOf(".bps"));
	// assume binary psl format
	DataInputStream dis = new DataInputStream(str);
	BpsParser psl_reader = new BpsParser();
        results = psl_reader.parse(dis, annot_type, null, seq_group, false, true);
      }
      else if (stream_name.endsWith(".bgn")) {
	System.out.println("loading via BgnParser: " + stream_name);
	BgnParser gene_reader = new BgnParser();
	String annot_type =  stream_name.substring(0, stream_name.lastIndexOf(".bgn"));
	//	String annot_type =  "das_" + stream_name.substring(0, stream_name.lastIndexOf(".bgn"));
	results = gene_reader.parse(str, annot_type, seq_group, -1, true);
      }
      else if (stream_name.endsWith(".brs")) {
	System.out.println("loading via BrsParser: " + stream_name);
	BrsParser refseq_reader = new BrsParser();
	String annot_type = stream_name.substring(0, stream_name.lastIndexOf(".brs"));
	//	String annot_type = "das_" + stream_name.substring(0, stream_name.lastIndexOf(".brs"));
	results = refseq_reader.parse(str, annot_type, seq_group);
      }
      else if (stream_name.endsWith(".bp1") || stream_name.endsWith(".bp2")) {
	System.out.println("loading via Bprobe1Parser: " + stream_name);
	Bprobe1Parser bp1_reader = new Bprobe1Parser();
	String annot_type = stream_name.substring(0, stream_name.lastIndexOf(".bp"));
        // parsing probesets in bp1/bp2 format, but not add ids to group's id2sym hash
        //   (to save memory)
	results = bp1_reader.parse(str, seq_group, true, annot_type, false);
	System.out.println("done loading via Bprobe1Parser: " + stream_name);
      }
      else if (stream_name.endsWith(".ead")) {
	System.out.println("loading via ExonArrayDesignParser");
	String annot_type = stream_name.substring(0, stream_name.lastIndexOf(".ead"));
	ExonArrayDesignParser parser = new ExonArrayDesignParser();
	parser.parse(str, seq_group, true, annot_type);
	System.out.println("done loading via ExonArrayDesignParser: " + stream_name);
      }
      else if (stream_name.endsWith(".gff") || stream_name.endsWith(".gtf")) {
	// assume it's GFF1, GFF2, or GTF format
	System.out.println("loading via GFFParser: " + stream_name);
	GFFParser parser = new GFFParser();
	// this feature filtering and group tags are all specific to the way Affy uses GTF files!
	parser.addFeatureFilter("intron");
	parser.addFeatureFilter("splice3");
	parser.addFeatureFilter("splice5");
	parser.addFeatureFilter("prim_trans");
	parser.addFeatureFilter("gene");

	parser.addFeatureFilter("transcript");

	parser.setGroupTag("transcript_id");
	// specifying via boolean arg that GFFParser should build container syms, one for each
	//    particular "source" on each particular seq
        results = parser.parse(str, seq_group, true);
      }
      else if (stream_name.endsWith(".cyt")) {
	System.out.println("loading via CytobandParser: " + stream_name);
	CytobandParser parser = new CytobandParser();
	results = parser.parse(str, seq_group, true);
      }
      else if (stream_name.endsWith(".bgr") ||
	       stream_name.endsWith(".bar") ) {
	// stream_name.endsWith(".gr") ||   can't use .gr yet, because doesn't
	//    specify _which_ seq to annotate (format to be upgraded soon to allow this)

	// parsing a graph
        results = GraphSymUtils.readGraphs(str, stream_name, gmodel, seq_group);
      }
      else {
	System.out.println("Can't parse, format not recognized: " + stream_name);
      }
      System.gc();
    }
    catch (Exception ex) {
      System.err.println("Error loading file: " + stream_name);
      ex.printStackTrace();
    } finally {
      if (str != null) try {str.close();} catch (Exception e) {}
    }

    // Now, enter the new SeqSymmetrys into the global SeqSymmetry
    // index, to make it easier to search for them later on:
    /*
    if (results != null) {
      Iterator iterator = results.iterator();
      SeqSymmetry nextSeqSym = null;
      while (iterator.hasNext()) {
        nextSeqSym = (SeqSymmetry) iterator.next();
        ParserController.globalSeqSymmetryIndex.registerSeqSymmetry(nextSeqSym);
      }
    }
    */

    return results;
  }

  /**
   * finds all SeqSymmetry objects having a particular Type and ID.
   */
  //  public static SeqSymmetry[] findMatchingSeqSyms(String type, String ID) {
  //    return ParserController.globalSeqSymmetryIndex.getAllSeqSymsByTypeAndID(type, ID);
  //  }
}
