package com.affymetrix.igb.general;

import com.affymetrix.genometry.AnnotatedBioSeq;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.MutableSeqSymmetry;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.seq.SimpleBioSeq;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GeneralBioSeq;
import com.affymetrix.genometryImpl.SmartAnnotBioSeq;
import com.affymetrix.genometryImpl.parsers.FastaParser;
import com.affymetrix.genometryImpl.parsers.NibbleResiduesParser;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.das.DasServerInfo;
import com.affymetrix.igb.das2.Das2Region;
import com.affymetrix.igb.das2.Das2SeqGroup;
import com.affymetrix.igb.das2.Das2ServerInfo;
import com.affymetrix.igb.das2.Das2VersionedSource;
import com.affymetrix.igb.util.DasUtils;
import com.affymetrix.igb.util.LocalUrlCacher;
import com.affymetrix.igb.view.QuickLoadServerModel;
import com.affymetrix.igb.view.SeqMapView;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ResidueLoading {

	private static final boolean DEBUG = true;
	
	/**
	 * Get residues from servers: DAS/2, Quickload, or DAS/1.
	 * @param serversWithChrom	-- list of servers that have this chromosome.
	 * @param genomeVersionName -- name of the genome.
	 * @param seq_name -- sequence (chromosome) name
	 * @param span	-- the residues requested.
	 * @return
	 */
	public static boolean getResidues(
					Set<genericServer> serversWithChrom, String genomeVersionName, String seq_name, SeqSpan span, SmartAnnotBioSeq aseq) {

		if (DEBUG) {
			System.out.println("trying to load residues for span: " + SeqUtils.spanToString(span));
			System.out.println("genome name: " + genomeVersionName);
		}

		final SeqMapView gviewer = Application.getSingleton().getMapView();
		AnnotatedSeqGroup seq_group = aseq.getSeqGroup();

		// Attempt to load via DAS/2
		for (genericServer server : serversWithChrom) {
			if (server.serverClass == Das2ServerInfo.class) {
				// Try to load in fasta format
				String uri;
				uri = generateDas2URI(
								server.URL, genomeVersionName,seq_name, span.getMin(), span.getMax(), false);
				if (LoadResiduesFromDAS2(seq_group, uri)) {
					gviewer.setAnnotatedSeq(aseq, true, true, true);
					return true;
				}

				// Try to load in bnib format
				uri = generateDas2URI(
								server.URL, genomeVersionName, seq_name, span.getMin(), span.getMax(), true);
				if (LoadResiduesFromDAS2(seq_group, uri)) {
					gviewer.setAnnotatedSeq(aseq, true, true, true);
					return true;
				}
			}
		}

		// Attempt to load via Quickload
		for (genericServer server : serversWithChrom) {
			if (server.serverClass == QuickLoadServerModel.class) {
				if (GetQuickLoadResidues(seq_group, seq_name, server.URL)) {
					gviewer.setAnnotatedSeq(aseq, true, true, true);
					return true;
				}
			}
		}

		for ( // Attempt to load via DAS/1
						genericServer server : serversWithChrom) {
			if (server.serverClass == DasServerInfo.class) {
				String residues = GetDAS1Residues(server.serverName, genomeVersionName, seq_name, span.getMin(), span.getMax(), span.getLength());
				if (residues != null) {
					// Add to composition if we're doing a partial sequence
					if (span.getLength() < aseq.getLength()) {
						AddResiduesToComposition(aseq, residues, span);
					}
					gviewer.setAnnotatedSeq(aseq, true, true, true);
					return true;
				}
			}
		}
						
		return false;
	}


	/**
	 * Get the residues from the specified DAS/1 server.
	 * @param das_dna_server
	 * @param current_genome_name
	 * @param seqid
	 * @param min
	 * @param max
	 * @param length
	 * @return
	 */
	private static String GetDAS1Residues(String das_dna_server, String current_genome_name, String seqid, int min, int max, int length) {
		String residues = null;

		if (seqid == null) {
			System.out.println("Couldn't determine das sequence residues -- seqid was null");
			return null;
		}
		try {
			String das_dna_source = DasUtils.findDasSource(das_dna_server, current_genome_name);
			if (das_dna_source == null) {
				if (DEBUG) {
					System.out.println("Couldn't find das source genome '" + current_genome_name + "'\n on DAS server:\n" + das_dna_server);
				}
				return null;    // if das_dna_source is null, there's no way to determine the residues
			}
			String das_seqid = DasUtils.findDasSeqID(das_dna_server, das_dna_source, seqid);
			if (das_seqid == null) {
				if (DEBUG) {
					System.out.println("Couldn't access sequence residues on DAS server\n" + " seqid: '" + seqid + "'\n" + " genome: '" + current_genome_name + "'\n" + " DAS server: " + das_dna_server);
				}
				return null;    // if seqid is null, there's no way to determine the residues
			}
			residues = DasUtils.getDasResidues(das_dna_server, das_dna_source, das_seqid, min, max);
			if (DEBUG) {
				System.out.println("DAS DNA request length: " + length);
				System.out.println("DAS DNA response length: " + residues.length());
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return residues;
	}

	/**
	 * Get the residues from the specified QuickLoad server.
	 * @param seq_group
	 * @param seq_name
	 * @param root_url
	 * @return
	 */
	private static boolean GetQuickLoadResidues(AnnotatedSeqGroup seq_group, String seq_name, String root_url) {
		boolean loaded;
		InputStream istr = null;
		String genome_name = seq_group.getID();
		try {
			String url_path = root_url + genome_name + "/" + seq_name + ".bnib";
			if (DEBUG) {
				System.out.println("trying to load residues via default QuickLoad location");
				System.out.println("  location of bnib file: " + url_path);
			}
			istr = LocalUrlCacher.getInputStream(url_path, true);
			if (istr == null) {
				return false;
			}
			// NibbleResiduesParser handles creating a BufferedInputStream from the input stream
			NibbleResiduesParser.parse(istr, seq_group);
			loaded = true;
		} catch (Exception ex) {
			loaded = false;
			System.out.println("Error -- cannot access sequence:\n" + "seq = '" + seq_name + "'\n" + "version = '" + genome_name + "'\n" + "server = " + root_url);
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(istr);
		}
		return loaded;
	}

	// Generate URI (e.g., "http://www.bioviz.org/das2/genome/A_thaliana_TAIR8/chr1?range=0:1000")
	private static String generateDas2URI(String URL, String genomeVersionName, 
					String segmentName, int min, int max, boolean bnibFormat) {
		if (DEBUG) {
			System.out.println("trying to load residues via DAS/2");
		}
		String uri = URL + "/" + genomeVersionName + "/" + segmentName + "?format=";
		if (!bnibFormat) {
			uri += "fasta";
		} else {
			uri += "bnib";
		}
		if (max > -1) {
			// ranged
			uri = uri + "&range=" + min + ":" + max;
		}

		if (DEBUG) {
			System.out.println("   request URI: " + uri);
		}
		return uri;
	}

	 // try loading via DAS/2 server that genome was originally modeled from
	private static boolean LoadResiduesFromDAS2(AnnotatedSeqGroup seq_group, String uri) {
		InputStream istr = null;
		Map headers = new HashMap();
		try {
			istr = LocalUrlCacher.getInputStream(uri, true, headers);
			// System.out.println(headers);
			String content_type = (String) headers.get("content-type");
			if (DEBUG) {
				System.out.println("    response content-type: " + content_type);
			}
			if (istr == null || content_type == null) {
				if (DEBUG) {
					System.out.println("  Didn't get a proper response from DAS/2; aborting DAS/2 residues loading.");
				}
				return false;
			}
			if (content_type.equals(NibbleResiduesParser.getMimeType())) {
				// check for bnib format
				// NibbleResiduesParser handles creating a BufferedInputStream from the input stream
				if (DEBUG) {
					System.out.println("   response is in bnib format, parsing...");
				}
				NibbleResiduesParser.parse(istr, seq_group);
				return true;
			}

			if (content_type.equals(FastaParser.getMimeType())) {
				// check for fasta format
				if (DEBUG) {
					System.out.println("   response is in fasta format, parsing...");
				}
				FastaParser.parseSingle(istr, seq_group);
				return true;
			}
			if (DEBUG) {
				System.out.println("   response is not in accepted format, aborting DAS/2 residues loading");
			}
			return false;
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(istr);
		}

		return false;
	}


	 // try loading via DAS/2 server
	/*private static String GetFASTADas2Residues(AnnotatedSeqGroup seq_group, SmartAnnotBioSeq aseq, int min, int max) {
		String uri = generateDas2URI(seq_group, aseq, min, max, false);
		InputStream istr = null;
		Map headers = new HashMap();
		try {
			istr = LocalUrlCacher.getInputStream(uri, true, headers);
			// System.out.println(headers);
			String content_type = (String) headers.get("content-type");
			System.out.println("    response content-type: " + content_type);
			if (istr == null || content_type == null) {
				System.out.println("  Didn't get a proper response from DAS/2; aborting DAS/2 residues loading.");
				return null;
			}

			if (content_type.equals(FastaParser.getMimeType())) {
				// check for fasta format
				System.out.println("   response is in fasta format, parsing...");
				return FastaParser.parseResidues(istr);
			}

			System.out.println("   response is not in accepted format, aborting DAS/2 residues loading");
			return null;
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(istr);
		}

		return null;
	}*/


    // Adds the residues to the composite sequence.  This allows merging of subsequences.
    private static void AddResiduesToComposition(AnnotatedBioSeq aseq, String residues, SeqSpan span) {
        BioSeq subseq = new SimpleBioSeq(aseq.getID() + ":" + span.getMin() + "-" + span.getMax(), residues);

        SeqSpan span1 = new SimpleSeqSpan(0, span.getLength(), subseq);
        SeqSpan span2 = span;
        MutableSeqSymmetry subsym = new SimpleMutableSeqSymmetry();
        subsym.addSpan(span1);
        subsym.addSpan(span2);

        GeneralBioSeq compseq = (GeneralBioSeq) aseq;
        MutableSeqSymmetry compsym = (MutableSeqSymmetry) compseq.getComposition();
        if (compsym == null) {
            //System.err.println("composite symmetry is null!");
            compsym = new SimpleMutableSeqSymmetry();
            compsym.addChild(subsym);
            compsym.addSpan(new SimpleSeqSpan(span2.getMin(), span2.getMax(), aseq));
            compseq.setComposition(compsym);
        } else {
            compsym.addChild(subsym);
            SeqSpan compspan = compsym.getSpan(aseq);
            int compmin = Math.min(compspan.getMin(), span.getMin());
            int compmax = Math.max(compspan.getMax(), span.getMax());
            SeqSpan new_compspan = new SimpleSeqSpan(compmin, compmax, aseq);
            compsym.removeSpan(compspan);
            compsym.addSpan(new_compspan);
        //        System.out.println("adding to composition: " );
        //        SeqUtils.printSymmetry(compsym);
        }
    }

}
