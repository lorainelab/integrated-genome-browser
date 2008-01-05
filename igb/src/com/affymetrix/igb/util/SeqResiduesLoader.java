package com.affymetrix.igb.util;

import java.io.*;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.seq.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.genometry.symmetry.*;
import com.affymetrix.genometry.util.*;
import com.affymetrix.genometryImpl.*;
import com.affymetrix.igb.*;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.QuickLoadView2;
import com.affymetrix.igb.view.QuickLoadServerModel;
import com.affymetrix.genometryImpl.parsers.NibbleResiduesParser;

public class SeqResiduesLoader {

	public static final String PREF_DAS_DNA_SERVER_URL = "DAS DNA Server URL";
	public static final String DEFAULT_DAS_DNA_SERVER = "http://genome.cse.ucsc.edu/cgi-bin/das";

	/**
	 *  Load all DNA residues for current AnnotatedBioSeq
	 */
	public static boolean loadAllResidues(SmartAnnotBioSeq aseq)  {
		//    if (current_group==null) { ErrorHandler.errorPanel("Error", "No sequence group selected.", gviewer); return; }
		//    if (current_seq==null) { ErrorHandler.errorPanel("Error", "No sequence selected.", gviewer); return; }
		SeqMapView gviewer = Application.getSingleton().getMapView();
		String seq_name = aseq.getID();
		AnnotatedSeqGroup seq_group = aseq.getSeqGroup();
		boolean loaded = true;
		System.out.println("processing request to load residues for sequence: " + seq_name);
		if (aseq.isComplete()) {
			System.out.println("already have residues for " + seq_name);
			loaded = false;
		}
		else {
			InputStream istr = null;
			String root_url = QuickLoadView2.getQuickLoadUrl();
			String genome_name = seq_group.getID();
			try {
				String url_path = root_url + genome_name + "/" + seq_name + ".bnib";
				System.out.println("location of bnib file: " + url_path);
				istr = LocalUrlCacher.getInputStream(url_path, QuickLoadServerModel.getCacheResidues());
				// NibbleResiduesParser handles creating a BufferedInputStream from the input stream
				NibbleResiduesParser.parse(istr, seq_group);
			}
			catch(Exception ex) {
				loaded = false;
				ErrorHandler.errorPanel("Error", "cannot access sequence:\n" +
						"seq = '" + seq_name + "'\n" +
						"version = '" + genome_name +"'\n" + "server = " + root_url,
						gviewer, ex);
			}
			finally {
				try { istr.close(); } catch (Exception e) {}
			}

			gviewer.setAnnotatedSeq(aseq, true, true, true);
		}
		return loaded;
	}


	/**
	 *  Load sequence residues for a span along a sequence.
	 *  Access residues via DAS reference server
	 *
	 *  DAS reference server can be specified by setting PREF_DAS_DNA_SERVER_URL preference value.
	 *  Currently defaults to UCSC DAS reference server (this will cause problems if genome is not
	 *     available at UCSC)
	 */
	public static boolean loadPartialResidues(SeqSpan span, AnnotatedSeqGroup seq_group)  {
		SeqMapView gviewer = Application.getSingleton().getMapView();
		String das_dna_server = UnibrowPrefsUtil.getLocation(PREF_DAS_DNA_SERVER_URL, DEFAULT_DAS_DNA_SERVER);
		AnnotatedBioSeq aseq = (AnnotatedBioSeq)span.getBioSeq();
		String seqid = aseq.getID();
		System.out.println("trying to load residues for span: " + SeqUtils.spanToString(span));
		String current_genome_name = seq_group.getID();
		System.out.println("current genome name: " + current_genome_name);

		//    System.out.println("seq_id: " + seqid);
		int min = span.getMin();
		int max = span.getMax();
		int length = span.getLength();

		if ((min <= 0) && (max >= aseq.getLength())) {
			System.out.println("loading all residues");
			// loadAllResidues(aseq.getID());
		}
		else if (aseq instanceof NibbleBioSeq)  {
			String residues = null;
			try {
				String das_dna_source = DasUtils.findDasSource(das_dna_server, current_genome_name);
				if (das_dna_source == null)  {
					System.out.println("Couldn't find das source genome '"+current_genome_name
							+ "'\n on DAS server:\n"+ das_dna_server);
					return false;
				}
				String das_seqid = DasUtils.findDasSeqID(das_dna_server, das_dna_source, seqid);
				if (das_seqid == null)  {
					System.out.println(
							"Couldn't access sequence residues on DAS server\n" +
							" seqid: '" + seqid +"'\n"+
							" genome: '"+current_genome_name + "'\n" +
							" DAS server: " + das_dna_server);
					return false;
				}
				residues = DasUtils.getDasResidues(das_dna_server, das_dna_source, das_seqid,
						min, max);
				System.out.println("DAS DNA request length: " + length);
				System.out.println("DAS DNA response length: " + residues.length());
			}
			catch (Exception ex) {
				System.out.println(
						"Couldn't access sequence residues on DAS server\n" +
						" seqid: '" + seqid +"'\n"+
						" genome: '"+current_genome_name + "'\n" +
						" DAS server: " + das_dna_server);
				return false;
			}

			if (residues != null) {
				BioSeq subseq = new SimpleBioSeq(aseq.getID() + ":" + min + "-" + max, residues);

				SeqSpan span1 = new SimpleSeqSpan(0, length, subseq);
				SeqSpan span2 = span;
				MutableSeqSymmetry subsym = new SimpleMutableSeqSymmetry();
				subsym.addSpan(span1);
				subsym.addSpan(span2);

				NibbleBioSeq compseq = (NibbleBioSeq)aseq;
				MutableSeqSymmetry compsym = (MutableSeqSymmetry)compseq.getComposition();
				if (compsym == null) {
					//System.err.println("composite symmetry is null!");
					compsym = new SimpleMutableSeqSymmetry();
					compsym.addChild(subsym);
					compsym.addSpan(new SimpleSeqSpan(span2.getMin(), span2.getMax(), aseq));
					compseq.setComposition(compsym);
				}
				else {
					compsym.addChild(subsym);
					SeqSpan compspan = compsym.getSpan(aseq);
					int compmin = Math.min(compspan.getMin(), min);
					int compmax = Math.max(compspan.getMax(), max);
					SeqSpan new_compspan = new SimpleSeqSpan(compmin, compmax, aseq);
					compsym.removeSpan(compspan);
					compsym.addSpan(new_compspan);
					//        System.out.println("adding to composition: " );
					//        SeqUtils.printSymmetry(compsym);
					gviewer.setAnnotatedSeq(aseq, true, true, true);
				}
			}
			else {
				return false;
			}
		}
		else {
			System.err.println("quickloaded seq is _not_ a NibbleBioSeq: " + aseq);
			return false;
		}
		return true;
	}

}
