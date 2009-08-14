package com.affymetrix.genometryImpl.util;

import java.io.*;
import java.util.*;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.parsers.*;

/**
 *  Trying to make a central repository for parsers.
 */
public final class ParserController {

	public static List parse(
			InputStream instr, Map<String, String> annots_map, String stream_name, GenometryModel gmodel, AnnotatedSeqGroup seq_group) {
		InputStream str = null;
		List results = null;
		String type_prefix = null;
		int sindex = stream_name.lastIndexOf("/");
		if (sindex >= 0) {
			type_prefix = stream_name.substring(0, sindex + 1);  // include ending "/" in prefix
		}
		try {
			if (instr instanceof BufferedInputStream) {
				str = (BufferedInputStream) instr;
			} else {
				str = new BufferedInputStream(instr);
			}
			if (stream_name.endsWith(".das") || stream_name.endsWith(".dasxml")) {
				System.out.println("loading via Das1FeatureSaxParser: " + stream_name);
				Das1FeatureSaxParser parser = new Das1FeatureSaxParser();
				// need to modify Das1FeatureSaxParser to return a list of "transcript-level" annotation syms
				parser = null;
			} else if (stream_name.endsWith(".link.psl"))  {
				System.out.println("loading link.psl via PslParser: " + stream_name);
				String annot_type = GetAnnotType(annots_map, stream_name, ".link.psl");
				// assume that want to annotate target seqs, and that these are the seqs
				//    represented in seq_group
				PSLParser parser = new PSLParser();
				parser.setIsLinkPsl(true);
				parser.enableSharedQueryTarget(true);
				parser.setCreateContainerAnnot(true); // is this needed?
				if (type_prefix != null) {
					parser.setTrackNamePrefix(type_prefix);
				}

				 // annotate target
				results = parser.parse(str, annot_type, null, seq_group, null, false, true, false);
			}
			else if (stream_name.endsWith(".bed")) {
				System.out.println("loading via BedParser: " + stream_name);
				String annot_type = GetAnnotType(annots_map, stream_name, ".bed");
				BedParser parser = new BedParser();
				// specifying via boolean arg that BedParser should build container syms
				results = parser.parse(str, gmodel, seq_group, true, annot_type, true);
			} else if (stream_name.endsWith(".bp1") || stream_name.endsWith(".bp2")) {
				System.out.println("loading via Bprobe1Parser: " + stream_name);
				Bprobe1Parser bp1_reader = new Bprobe1Parser();
				if (type_prefix != null) {
					bp1_reader.setTypePrefix(type_prefix);
				}
				String annot_type = GetAnnotType(annots_map, stream_name, ".bp");
				// parsing probesets in bp1/bp2 format, but not add ids to group's id2sym hash
				//   (to save memory)
				results = bp1_reader.parse(str, seq_group, true, annot_type, false);
				System.out.println("done loading via Bprobe1Parser: " + stream_name);
			} else if (stream_name.endsWith(".ead")) {
				System.out.println("loading via ExonArrayDesignParser");
				String annot_type = GetAnnotType(annots_map, stream_name, ".ead");
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
				// specifying via boolean arg that GFFParser should build container syms, one for each
				//    particular "source" on each particular seq, can override the source for setting the name
				results = parser.parse(str, stream_name.substring(0, stream_name.length() - 4), seq_group, true);
			} else if (stream_name.endsWith(".cyt")) {
				System.out.println("loading via CytobandParser: " + stream_name);
				CytobandParser parser = new CytobandParser();
				results = parser.parse(str, seq_group, true);
			} else if (stream_name.endsWith(".bgr") ||
					stream_name.endsWith(".bar")) {
				// stream_name.endsWith(".gr") ||   can't use .gr yet, because doesn't
				//    specify _which_ seq to annotate (format to be upgraded soon to allow this)

				// parsing a graph
				results = GraphSymUtils.readGraphs(str, stream_name, gmodel, seq_group);
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
	 * @param stream_name
	 * @param annots_map
	 * @param str
	 * @param type_prefix
	 * @param seq_group
	 * @return
	 */
	public static List parseIndexed(InputStream str, Map<String, String> annots_map, String stream_name, AnnotatedSeqGroup seq_group) {

		DataInputStream dis = new DataInputStream(str);
		if (stream_name.endsWith(".psl") || stream_name.endsWith(".psl3")) {
			String type_prefix = null;
		int sindex = stream_name.lastIndexOf("/");
		if (sindex >= 0) {
			type_prefix = stream_name.substring(0, sindex + 1);  // include ending "/" in prefix
		}
			System.out.println("loading via PslParser: " + stream_name);
			String annot_type = GetAnnotType(annots_map, stream_name, ".psl");
			IndexWriter iWriter = new PSLParser();
			if (type_prefix != null) {
				((PSLParser) iWriter).setTrackNamePrefix(type_prefix);
			}
			return iWriter.parse(dis, annot_type, seq_group);
		}
		if (stream_name.endsWith(".bps")) {
			System.out.println("loading via BpsParser: " + stream_name);
			String annot_type = GetAnnotType(annots_map, stream_name, ".bps");
			IndexWriter iWriter = new BpsParser();
			return iWriter.parse(dis, annot_type, seq_group);
		}
		if (stream_name.endsWith(".bgn")) {
			System.out.println("loading via BgnParser: " + stream_name);
			IndexWriter iWriter = new BgnParser();
			String annot_type = GetAnnotType(annots_map, stream_name, ".bgn");
			return iWriter.parse(dis, annot_type, seq_group);
		}
		if (stream_name.endsWith(".brs")) {
			System.out.println("loading via BrsParser: " + stream_name);
			IndexWriter iWriter = new BrsParser();
			String annot_type = GetAnnotType(annots_map, stream_name, ".brs");
			return iWriter.parse(dis, annot_type, seq_group);
		}
		return null;
	}



	public static IndexWriter getIndexWriter(String stream_name) {
		if (stream_name.endsWith((".bps"))) {
			return new BpsParser();
		}
		if (stream_name.endsWith("psl") && !stream_name.endsWith("link.psl")) {
			return new PSLParser();
		}
		if (stream_name.endsWith(".bgn")) {
			return new BgnParser();
		}
		if (stream_name.endsWith(".brs")) {
			return new BrsParser();
		}
		return null;
		
	}

	// return an annotation type.
	// This is either:
	// 1.  A type name contained in the annots_map hash table.
	// 2.  (Default) The stream name with the extension stripped off.
	public static String GetAnnotType(
			Map<String, String> annots_map, String stream_name, String extension) {
		// Check if this was in the annots mapping.
		if (annots_map != null && !annots_map.isEmpty()) {
			if (annots_map.containsKey(stream_name)) {
				return annots_map.get(stream_name);
			}
			if (annots_map.containsKey(stream_name.toLowerCase())) {
				return annots_map.get(stream_name.toLowerCase());
			}
		}

		if (extension == null) {
			return stream_name;
		}

		// Strip off the extension.
		return stream_name.substring(0, stream_name.lastIndexOf(extension));
	}
}
