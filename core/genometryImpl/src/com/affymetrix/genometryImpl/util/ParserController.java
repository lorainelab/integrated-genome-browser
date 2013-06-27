package com.affymetrix.genometryImpl.util;

import java.io.*;
import java.util.*;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.parsers.AnnotsXmlParser.AnnotMapElt;
import com.affymetrix.genometryImpl.parsers.Bprobe1Parser;
import com.affymetrix.genometryImpl.parsers.CytobandParser;
import com.affymetrix.genometryImpl.parsers.ExonArrayDesignParser;
import com.affymetrix.genometryImpl.parsers.FileTypeHolder;
import com.affymetrix.genometryImpl.parsers.GFFParser;
import com.affymetrix.genometryImpl.parsers.IndexWriter;
import com.affymetrix.genometryImpl.parsers.PSLParser;
import com.affymetrix.genometryImpl.symmetry.GraphSym;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  Trying to make a central repository for parsers.
 */
public final class ParserController {
    
	static List<? extends SeqSymmetry> parse(
			InputStream instr, List<AnnotMapElt> annotList, String stream_name, AnnotatedSeqGroup seq_group, String type_prefix) {
		InputStream str = null;
		List<? extends SeqSymmetry> results = null;
		try {
			if (instr instanceof BufferedInputStream) {
			} else {
				str = new BufferedInputStream(instr);
			}
			if (stream_name.endsWith(".bp1") || stream_name.endsWith(".bp2")) {
				System.out.println("loading via Bprobe1Parser: " + stream_name);
				Bprobe1Parser bp1_reader = new Bprobe1Parser();
				if (type_prefix != null) {
					bp1_reader.setTypePrefix(type_prefix);
				}
				String annot_type = getAnnotType(annotList, stream_name, ".bp", type_prefix);
				// parsing probesets in bp1/bp2 format, but not add ids to group's id2sym hash
				//   (to save memory)
				results = bp1_reader.parse(str, seq_group, true, annot_type, false);
				System.out.println("done loading via Bprobe1Parser: " + stream_name);
			} else if (stream_name.endsWith(".ead")) {
				System.out.println("loading via ExonArrayDesignParser");
				String annot_type = getAnnotType(annotList, stream_name, ".ead", type_prefix);
				ExonArrayDesignParser parser = new ExonArrayDesignParser();
				parser.parse(str, seq_group, true, annot_type);
				System.out.println("done loading via ExonArrayDesignParser: " + stream_name);
			} else if (stream_name.endsWith(".gff") || stream_name.endsWith(".gtf")) {
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
				parser.setUseDefaultSource(true);
				parser.setUseTrackLines(false);
				// specifying via boolean arg that GFFParser should build container syms, one for each
				//    particular "source" on each particular seq, can override the source for setting the name
				String annot_type = type_prefix == null ? stream_name.substring(0, stream_name.length() - 4) : type_prefix;
				return parser.parse(str, annot_type, seq_group, true);
			} else if (stream_name.endsWith(".cyt")) {
				System.out.println("loading via CytobandParser: " + stream_name);
				CytobandParser parser = new CytobandParser();
				return parser.parse(str, seq_group, true);
			} else if (stream_name.endsWith(".bgr") ||
					stream_name.endsWith(".bar")) {
				// stream_name.endsWith(".gr") ||   can't use .gr yet, because doesn't
				//    specify _which_ seq to annotate (format to be upgraded soon to allow this)

				// parsing a graph
				List<GraphSym> graphs = GraphSymUtils.readGraphs(str, stream_name, seq_group, null);
				GraphSymUtils.processGraphSyms(graphs, stream_name, null);
				return graphs;
			}
			else {
				System.out.println("Can't parse, format not recognized: " + stream_name);
			}
		} catch (Exception ex) {
			System.err.println("Error loading file: " + stream_name);
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(str);
		}

		return results;
	}


	/**
	 * Parsing indexed files; don't annotate.
	 * Precondition: the stream is parseable via IndexWriter.
	 * @param str
	 * @param annotList
	 * @param stream_name
	 * @param type_prefix
	 * @return A list of parsed indexes
	 */
	static List<? extends SeqSymmetry> parseIndexed(
			InputStream str, List<AnnotMapElt> annotList, String stream_name, AnnotatedSeqGroup seq_group, String type_prefix) {
		try {
			IndexWriter iWriter = getIndexWriter(stream_name);
			DataInputStream dis = new DataInputStream(str);

			String extension = GeneralUtils.getExtension(stream_name);	// .psl, .bed, et cetera
			String annot_type = getAnnotType(annotList, stream_name, extension, type_prefix);

			System.out.println("Indexing " + stream_name);

			if (extension.equals(".link.psl")) {
				try {
					// annotate target
					return ((PSLParser) iWriter).parse(dis, annot_type, null, seq_group, null, false, true, false);
				} catch (IOException ex) {
					Logger.getLogger(ParserController.class.getName()).log(Level.SEVERE, null, ex);
				}
			}

			// bed, bps, bgn, brs, psl, psl3
			return iWriter.parse(dis, annot_type, seq_group);
		} finally {
			GeneralUtils.safeClose(str);
		}
	}

	public static IndexWriter getIndexWriter(String stream_name) {
		return FileTypeHolder.getInstance().getFileTypeHandlerForURI(stream_name).getIndexWriter(stream_name);
	}


	// This is either:
	// 1.  A type name contained in the annotList hash table.
	// 2.  (Default) The stream name with the extension stripped off.
	public static String getAnnotType(
			List<AnnotMapElt> annotsList, String stream_name, String extension, String type_name) {
		
		// Cytoband files appear to require a specific name.
		if (stream_name.endsWith(".cyt")) {
			return CytobandParser.CYTOBAND_TIER_NAME;
		}
		
		
		// Check if this was in the annots mapping.
		if (annotsList != null) {
System.out.println("\tChecking annots mapping!");
			AnnotMapElt annotMapElt = AnnotMapElt.findFileNameElt(stream_name, annotsList);
			if (annotMapElt != null) {
System.out.println("\t\tTitle "+annotMapElt.title);
				return annotMapElt.title;
			}
		}
		
		// If we didn't find an entry on annots, and the type_name was provided,
		// use it.
		if (type_name != null) {
			return type_name;
		}

		// If a type name wasn't provided, and the filename doesn't have an
		// extension, use it.
		if (extension == null) {
			return stream_name;
		}

		// Otherwise, just use the file name, first stripping off the extension.
		return stream_name.substring(0, stream_name.lastIndexOf(extension));
	}
}

