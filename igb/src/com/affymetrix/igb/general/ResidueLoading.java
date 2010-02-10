package com.affymetrix.igb.general;

import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.parsers.FastaParser;
import com.affymetrix.genometryImpl.parsers.NibbleResiduesParser;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.das.DasLoader;
import com.affymetrix.igb.util.LocalUrlCacher;
import com.affymetrix.igb.view.SeqMapView;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ResidueLoading {

	enum FORMAT {
		RAW,
		BNIB,
		FASTA
	};
	
	private static final boolean DEBUG = true;

	/**
	 * Get residues from servers: DAS/2, Quickload, or DAS/1.
	 * Also gets partial residues.
	 * @param serversWithChrom	-- list of servers that have this chromosome.
	 * @param genomeVersionName -- name of the genome.
	 * @param seq_name -- sequence (chromosome) name
	 * @param span	-- May be null.  If not, then it's used for partial loading.
	 * @return boolean
	 */
	// Most confusing thing here -- certain parsers update the composition, and certain ones do not.
	// DAS/1 and partial loading in DAS/2 do not update the composition, so it's done separately.
	public static boolean getResidues(
			Set<GenericServer> serversWithChrom, String genomeVersionName, String seq_name, int min, int max, BioSeq aseq, SeqSpan span) {

		boolean partial_load = (min > 0 || max < aseq.getLength());	// Are we only asking for part of the sequence?

		final SeqMapView gviewer = Application.getSingleton().getMapView();
		AnnotatedSeqGroup seq_group = aseq.getSeqGroup();

		//First try to load from DAS2, then Quickload and at last DAS1
		if (partial_load) {
			// Try to load in raw format from DAS2 server.
			for (GenericServer server : serversWithChrom) {
				if (server.serverType == ServerType.DAS2) {
					String uri;
					uri = generateDas2URI(server.URL, genomeVersionName, seq_name, min, max, FORMAT.RAW);
					String residues = GetPartialFASTADas2Residues(uri);
					if (residues != null) {
						// span is non-null, here
						BioSeq.addResiduesToComposition(aseq, residues, span);
						gviewer.setAnnotatedSeq(aseq, true, true, true);
						return true;
					}
				}
			}

			// Try to load in fasta format from DAS2 server.
			for (GenericServer server : serversWithChrom) {
				if (server.serverType == ServerType.DAS2) {
					String uri;
					uri = generateDas2URI(server.URL, genomeVersionName, seq_name, min, max, FORMAT.FASTA);
					String residues = GetPartialFASTADas2Residues(uri);
					if (residues != null) {
						// span is non-null, here
						BioSeq.addResiduesToComposition(aseq, residues, span);
						gviewer.setAnnotatedSeq(aseq, true, true, true);
						return true;
					}
				}
			}

			// Try to load from Quickload server.
			for (GenericServer server : serversWithChrom) {
				if (server.serverType == ServerType.QuickLoad) {
					String residues = GetQuickLoadResidues(seq_group, seq_name, server.URL, min, max);
					if (residues != null) {
						BioSeq.addResiduesToComposition(aseq, residues, span);
						gviewer.setAnnotatedSeq(aseq, true, true, true);
						return true;
					}
				}
			}

			// Try to load via DAS/1 server.
			for (GenericServer server : serversWithChrom) {
				if (server.serverType == ServerType.DAS) {
					String residues = GetDAS1Residues(server.URL, genomeVersionName, seq_name, min, max);
					if (residues != null) {
						// Add to composition if we're doing a partial sequence
						// span is non-null, here
						BioSeq.addResiduesToComposition(aseq, residues, span);
						gviewer.setAnnotatedSeq(aseq, true, true, true);
						return true;
					}
				}
			}
		}
		// not a partial load.
		else {

			//Try to load in raw format from DAS2 server, as this format is more compactly represented internally.
			for (GenericServer server : serversWithChrom) {
				if (server.serverType == ServerType.DAS2) {
					String uri;
					
					uri = generateDas2URI(
					server.URL, genomeVersionName, seq_name, min, max, FORMAT.RAW);
					String residues = LoadResiduesFromDAS2(uri);
					if (residues != null) {
						aseq.setResidues(residues);
						BioSeq.addResiduesToComposition(aseq);
						gviewer.setAnnotatedSeq(aseq, true, true, true);
						return true;
					}
				}
			}

			// Try to load in bnib format from DAS2 server, as this format is more compactly represented internally.
			for (GenericServer server : serversWithChrom) {
				if (server.serverType == ServerType.DAS2) {
					String uri;
					uri = generateDas2URI(
							server.URL, genomeVersionName, seq_name, min, max, FORMAT.BNIB);
					if (LoadResiduesFromDAS2(seq_group, uri)) {
						BioSeq.addResiduesToComposition(aseq);
						gviewer.setAnnotatedSeq(aseq, true, true, true);
						return true;
					}
				}
			}

			// Try to load in fasta format from DAS2 server.
			for (GenericServer server : serversWithChrom) {
				if (server.serverType == ServerType.DAS2) {
					String uri;
					uri = generateDas2URI(server.URL, genomeVersionName, seq_name, min, max, FORMAT.FASTA);
					if (LoadResiduesFromDAS2(seq_group, uri)) {
						BioSeq.addResiduesToComposition(aseq);
						gviewer.setAnnotatedSeq(aseq, true, true, true);
						return true;
					}
				}
			}

			//Try to load from Quickload server.
			for (GenericServer server : serversWithChrom) {
				if (server.serverType == ServerType.QuickLoad) {
					if (GetQuickLoadResidues(seq_group, seq_name, server.URL)) {
						BioSeq.addResiduesToComposition(aseq);
						gviewer.setAnnotatedSeq(aseq, true, true, true);
						return true;
					}
				}
			}

			// Try to load via DAS/1 server.
			for (GenericServer server : serversWithChrom) {
				if (server.serverType == ServerType.DAS) {
					String residues = GetDAS1Residues(server.URL, genomeVersionName, seq_name, min, max);
					if (residues != null) {
						aseq.setResidues(residues);
						BioSeq.addResiduesToComposition(aseq);
						gviewer.setAnnotatedSeq(aseq, true, true, true);
						return true;
					}
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
	 * @return a string of residues from the DAS/1 server
	 */
	private static String GetDAS1Residues(String das_dna_server, String current_genome_name, String seqid, int min, int max) {
		String residues = null;

		if (seqid == null) {
			System.out.println("Couldn't determine das sequence residues -- seqid was null");
			return null;
		}
		try {
			String das_dna_source = DasLoader.findDasSource(das_dna_server, current_genome_name);
			if (das_dna_source == null) {
				if (DEBUG) {
					System.out.println("Couldn't find das source genome " + current_genome_name + " on DAS server:" + das_dna_server);
				}
				return null;    // if das_dna_source is null, there's no way to determine the residues
			}
			String das_seqid = DasLoader.findDasSeqID(das_dna_server, das_dna_source, seqid);
			if (das_seqid == null) {
				if (DEBUG) {
					System.out.println("Couldn't access sequence residues on DAS server  seqid: " + seqid + " genome: " + current_genome_name + " DAS server: " + das_dna_server);
				}
				return null;    // if seqid is null, there's no way to determine the residues
			}
			residues = DasLoader.getDasResidues(das_dna_server, das_dna_source, das_seqid, min, max);
			if (DEBUG) {
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
	 * @return true or false
	 */
	private static boolean GetQuickLoadResidues(AnnotatedSeqGroup seq_group, String seq_name, String root_url) {
		boolean loaded;
		InputStream istr = null;
		String genome_name = seq_group.getID();
		try {
			String url_path = root_url + genome_name + "/" + seq_name + ".bnib";
			if (DEBUG) {
				System.out.println("  Quickload location of bnib file: " + url_path);
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

	private static String GetQuickLoadResidues(AnnotatedSeqGroup seq_group, String seq_name, String root_url, int min, int max)
	{
		InputStream istr = null;
		String genome_name = seq_group.getID();
		try {
			String url_path = root_url + genome_name + "/" + seq_name + ".bnib";
			if (DEBUG) {
				System.out.println("  Quickload location of bnib file: " + url_path);
			}
			istr = LocalUrlCacher.getInputStream(url_path, true);
			if (istr == null) {
				return null;
			}
			
			ByteArrayOutputStream outstream = new ByteArrayOutputStream();
			NibbleResiduesParser.parse(istr, min, max, outstream);
			return outstream.toString();

		} catch (Exception ex) {
			System.out.println("Error -- cannot access sequence:\n" + "seq = '" + seq_name + "'\n" + "version = '" + genome_name + "'\n" + "server = " + root_url);
			ex.printStackTrace();
			return null;
		} finally {
			GeneralUtils.safeClose(istr);
		}
	}
	// Generate URI (e.g., "http://www.bioviz.org/das2/genome/A_thaliana_TAIR8/chr1?range=0:1000")
	private static String generateDas2URI(String URL, String genomeVersionName,
			String segmentName, int min, int max, FORMAT Format) {
		if (DEBUG) {
			System.out.println("trying to load residues via DAS/2");
		}
		String uri = URL + "/" + genomeVersionName + "/" + segmentName + "?format=";
//		if (!bnibFormat) {
//			uri += "fasta";
//		} else {
//			uri += "bnib";
//		}
		switch(Format)
		{
			case RAW:
				uri += "raw";
				break;

			case BNIB:
				uri += "bnib";
				break;

			case FASTA:
				uri += "fasta";
				break;
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
		Map<String, String> headers = new HashMap<String, String>();
		try {
			istr = LocalUrlCacher.getInputStream(uri, true, headers);
			// System.out.println(headers);
			String content_type = headers.get("content-type");
			if (DEBUG) {
				System.out.println("    response content-type: " + content_type);
			}
			if (istr == null || content_type == null) {
				if (DEBUG) {
					System.out.println("  Improper response from DAS/2; aborting DAS/2 residues loading.");
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

	private static String LoadResiduesFromDAS2(String uri) {
		InputStream istr = null;
		BufferedReader buff = null;
		Map<String, String> headers = new HashMap<String, String>();
		try {
			istr = LocalUrlCacher.getInputStream(uri, true, headers);
			// System.out.println(headers);
			String content_type = headers.get("content-type");
			if (DEBUG) {
				System.out.println("    response content-type: " + content_type);
			}
			if (istr == null || content_type == null) {
				if (DEBUG) {
					System.out.println("  Improper response from DAS/2; aborting DAS/2 residues loading.");
				}
				return null;
			}
			if(content_type.equals("text/raw"))
			{
				if (DEBUG) {
					System.out.println("   response is in raw format, parsing...");
				}
				buff = new BufferedReader(new InputStreamReader(istr));
				return buff.readLine();
			}

			if (DEBUG) {
				System.out.println("   response is not in accepted format, aborting DAS/2 residues loading");
			}
			return null;
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(buff);
			GeneralUtils.safeClose(istr);
		}

		return null;
	}

	// try loading via DAS/2 server
	private static String GetPartialFASTADas2Residues(String uri) {
		InputStream istr = null;
		BufferedReader buff = null;
		Map<String, String> headers = new HashMap<String, String>();
		try {
			istr = LocalUrlCacher.getInputStream(uri, true, headers);
			// System.out.println(headers);
			String content_type = headers.get("content-type");
			if (DEBUG) {
				System.out.println("    response content-type: " + content_type);
			}
			if (istr == null || content_type == null) {
				if (DEBUG) {
					System.out.println("  Didn't get a proper response from DAS/2; aborting DAS/2 residues loading.");
				}
				return null;
			}

			if(content_type.equals("text/raw"))
			{
				if (DEBUG) {
					System.out.println("   response is in raw format, parsing...");
				}
				buff = new BufferedReader(new InputStreamReader(istr));
				return buff.readLine();
			}
			
//			if (content_type.equals(NibbleResiduesParser.getMimeType())) {
//				// check for bnib format
//				if (DEBUG) {
//					System.out.println("   response is in bnib format, parsing...");
//				}
//				ByteArrayOutputStream outstream = new ByteArrayOutputStream();
//				NibbleResiduesParser.parse(istr,outstream);
//				return outstream.toString();
//			}

			if (content_type.equals(FastaParser.getMimeType())) {
				// check for fasta format
				if (DEBUG) {
					System.out.println("   response is in fasta format, parsing...");
				}
				return FastaParser.parseResidues(istr);
			}
			
			if (DEBUG) {
				System.out.println("   response is not in accepted format, aborting DAS/2 residues loading");
			}
			return null;
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(buff);
			GeneralUtils.safeClose(istr);
		}

		return null;
	}

}
