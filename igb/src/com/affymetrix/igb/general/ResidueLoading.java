package com.affymetrix.igb.general;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.MutableSeqSymmetry;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.seq.SimpleBioSeq;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometry.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SmartAnnotBioSeq;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.parsers.FastaParser;
import com.affymetrix.genometryImpl.parsers.NibbleResiduesParser;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.das.DasLoader;
import com.affymetrix.igb.util.LocalUrlCacher;
import com.affymetrix.igb.view.SeqMapView;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class ResidueLoading {

	private static final boolean DEBUG = true;
	
	/**
	 * Get residues from servers: DAS/2, Quickload, or DAS/1.
	 * Also gets partial residues.
	 * @param serversWithChrom	-- list of servers that have this chromosome.
	 * @param genomeVersionName -- name of the genome.
	 * @param seq_name -- sequence (chromosome) name
	 * @param span	-- May be null.  If not, then it's used for partial loading.
	 * @return
	 */
	// Most confusing thing here -- certain parsers update the composition, and certain ones do not.
	// DAS/1 and partial loading in DAS/2 do not update the composition, so it's done separately.
	public static boolean getResidues(
					Set<GenericServer> serversWithChrom, String genomeVersionName, String seq_name, int min, int max, SmartAnnotBioSeq aseq, SeqSpan span) {

		boolean partial_load = (min > 0 || max < aseq.getLength());	// Are we only asking for part of the sequence?
		
		final SeqMapView gviewer = Application.getSingleton().getMapView();
		AnnotatedSeqGroup seq_group = aseq.getSeqGroup();

		// Attempt to load via DAS/2
		for (GenericServer server : serversWithChrom) {
			if (server.serverType == ServerType.DAS2) {
				String uri;

				if (partial_load) {
					// Try to load in fasta format
					uri = generateDas2URI(server.URL, genomeVersionName, seq_name, min, max, false);
					String residues = GetPartialFASTADas2Residues(uri);
					if (residues != null) {
						// span is non-null, here
						AddResiduesToComposition(aseq, residues, span);
						gviewer.setAnnotatedSeq(aseq, true, true, true);
						return true;
					}
				} else {
					// not a partial load.  Try bnib format first, as this format is more compactly represented internally.
					uri = generateDas2URI(
									server.URL, genomeVersionName, seq_name, min, max, true);
					if (LoadResiduesFromDAS2(seq_group, uri)) {
						AddResiduesToComposition(aseq);
						gviewer.setAnnotatedSeq(aseq, true, true, true);
						return true;
					}

					// Try fasta format.
					uri = generateDas2URI(server.URL, genomeVersionName, seq_name, min, max, false);
					if (LoadResiduesFromDAS2(seq_group, uri)) {
						AddResiduesToComposition(aseq);
						gviewer.setAnnotatedSeq(aseq, true, true, true);
						return true;
					}
				}
			}
		}

		
		if (!partial_load) {
			// Attempt to load via Quickload -- not supported except for full loading.
			for (GenericServer server : serversWithChrom) {
				if (server.serverType == ServerType.QuickLoad) {
					if (GetQuickLoadResidues(seq_group, seq_name, server.URL)) {
						AddResiduesToComposition(aseq);
						gviewer.setAnnotatedSeq(aseq, true, true, true);
						return true;
					}
				}
			}
		}

		for ( // Attempt to load via DAS/1
						GenericServer server : serversWithChrom) {
			if (server.serverType == ServerType.DAS) {
				String residues = GetDAS1Residues(server.URL, genomeVersionName, seq_name, min, max);
				if (residues != null) {
					// Add to composition if we're doing a partial sequence
					if (partial_load) {
						// span is non-null, here
						AddResiduesToComposition(aseq, residues, span);
					} else {
						aseq.setResidues(residues);
						AddResiduesToComposition(aseq);
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
	 * @return
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
		Map<String,String> headers = new HashMap<String,String>();
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

	// try loading via DAS/2 server
    private static String GetPartialFASTADas2Residues(String uri) {
        InputStream istr = null;
        Map<String,String> headers = new HashMap<String,String>();
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
            GeneralUtils.safeClose(istr);
        }

        return null;
    }

		/**
		 * Add residues to composition (full sequence loaded).
		 * @param aseq
		 */
		private static void AddResiduesToComposition(SmartAnnotBioSeq aseq) {
			String residues = aseq.getResidues();
			SeqSpan span = new SimpleSeqSpan(0, residues.length(), aseq);
			AddResiduesToComposition(aseq, residues, span);
		}
		
    /**
		 * Adds the residues to the composite sequence.  This allows merging of subsequences.
		 * @param aseq
		 * @param residues
		 * @param span
		 */
    private static void AddResiduesToComposition(SmartAnnotBioSeq aseq, String residues, SeqSpan span) {
        BioSeq subseq = new SimpleBioSeq(aseq.getID() + ":" + span.getMin() + "-" + span.getMax(), residues);

        SeqSpan span1 = new SimpleSeqSpan(0, span.getLength(), subseq);
        SeqSpan span2 = span;
        MutableSeqSymmetry subsym = new SimpleMutableSeqSymmetry();
        subsym.addSpan(span1);
        subsym.addSpan(span2);

        MutableSeqSymmetry compsym = (MutableSeqSymmetry) aseq.getComposition();
        if (compsym == null) {
            //No children.  Add one.
            compsym = new SimpleMutableSeqSymmetry();
            compsym.addChild(subsym);
            compsym.addSpan(new SimpleSeqSpan(span2.getMin(), span2.getMax(), aseq));
            aseq.setComposition(compsym);
        } else {
					// Merge children that already exist.
            compsym.addChild(subsym);
            SeqSpan compspan = compsym.getSpan(aseq);
            int compmin = Math.min(compspan.getMin(), span.getMin());
            int compmax = Math.max(compspan.getMax(), span.getMax());
            SeqSpan new_compspan = new SimpleSeqSpan(compmin, compmax, aseq);
            compsym.removeSpan(compspan);
            compsym.addSpan(new_compspan);
        }
    }

}
