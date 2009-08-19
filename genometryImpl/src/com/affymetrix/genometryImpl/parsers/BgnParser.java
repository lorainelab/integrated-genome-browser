/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */

package com.affymetrix.genometryImpl.parsers;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.*;

import com.affymetrix.genometryImpl.MutableAnnotatedBioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.UcscGeneSym;
import com.affymetrix.genometryImpl.SupportsCdsSpan;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.comparator.SeqSymMinComparator;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.Timer;

/**
 *  Just like refFlat table format, except no geneName field (just name field).
 */
public final class BgnParser implements AnnotationWriter, IndexWriter  {
	boolean use_byte_buffer = true;
	boolean write_from_text = true;
	static final boolean DEBUG = false;

	static List<String> pref_list = new ArrayList<String>();
	static {
		pref_list.add("bgn");
	}

	//static String default_annot_type = "genepred";
	//  static String default_annot_type = "refflat-test";
	//static String user_dir = System.getProperty("user.dir");

	// mod_chromInfo.txt is same as chromInfo.txt, except entries have been arranged so
	//   that all random, etc. bits are at bottom


	// .bin1:
	//         name UTF8
	//        chrom UTF8
	//       strand UTF8
	//      txStart int
	//        txEnd int
	//     cdsStart int
	//       cdsEnd int
	//    exoncount int
	//   exonStarts int[exoncount]
	//     exonEnds int[exoncount]
	//
	static final Pattern line_regex = Pattern.compile("\t");
	static final Pattern emin_regex = Pattern.compile(",");
	static final Pattern emax_regex = Pattern.compile(",");

	ArrayList<String> chromosomes = new ArrayList<String>();


	public List parse(DataInputStream dis, String annot_type, AnnotatedSeqGroup group) {
		try {
			return this.parse(dis, annot_type, group, 0, false);
		} catch (IOException ex) {
			Logger.getLogger(BgnParser.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}


	/*public List parse(String file_name, String annot_type, AnnotatedSeqGroup seq_group) throws IOException {
		if (DEBUG) {
			System.out.println("loading file: " + file_name);
		}
		File fil = new File(file_name);
		long blength = fil.length();
		FileInputStream fis = null;
		List result;
		try {
			fis = new FileInputStream(fil);
			result = parse(fis, annot_type, seq_group, blength, true);
		} finally {
			GeneralUtils.safeClose(fis);
		}
		return result;
	}*/

	/**
	 *  The main parsing routine.
	 *  @param seq_group  must not be null.
	 *  @param blength  Byte Buffer Length.
	 *     If length is unknown, force to skip using byte buffer by passing in blength = -1;
	 */
	public List<SeqSymmetry> parse(InputStream istr, String annot_type,
			AnnotatedSeqGroup seq_group, long blength, boolean annotate_seq) throws IOException {

		if (seq_group == null) {
			throw new IllegalArgumentException("BgnParser called with seq_group null.");
		}
		Timer tim = new Timer();
		tim.start();

		// annots is list of top-level parent syms (max 1 per seq in seq_group) that get
		//    added as annotations to the annotated BioSeqs -- their children
		//    are then actual transcript annotations
		ArrayList<SeqSymmetry> annots = new ArrayList<SeqSymmetry>();
		// results is list actual transcript annotations
		ArrayList<SeqSymmetry> results = new ArrayList<SeqSymmetry>(15000);
		// chrom2sym is temporary hash to put top-level parent syms in to map
		//     seq id to top-level symmetry, prior to adding these parent syms
		//     to the actual annotated seqs
		HashMap<String,SeqSymmetry> chrom2sym = new HashMap<String,SeqSymmetry>(); // maps chrom name to top-level symmetry

		int total_exon_count = 0;
		int count = 0;
		//int same_count = 0;
		BufferedInputStream bis = new BufferedInputStream(istr);
		DataInputStream dis = null;
		boolean reached_EOF = false;

		try {
			//      BufferedInputStream bis = new BufferedInputStream(fis, 16384);
			if (use_byte_buffer && blength > 0) {
				byte[] bytebuf = new byte[(int)blength];
				bis.read(bytebuf);
				//        fis.read(bytebuf);
				ByteArrayInputStream bytestream = new ByteArrayInputStream(bytebuf);
				dis = new DataInputStream(bytestream);
			}
			else {
				dis = new DataInputStream(bis);
			}
			//if (true) {
				/*
				 *  "while (dis.available() > 0)" loop is not a good alternative
				 *     when retrieving the data from a slow InputStream -- for example, over a
				 *     wireless network connection.
				 *  There may still be bytes to read from the stream even if they're not yet
				 *     available -- basically if the processing here outpaces the speed of
				 *     streaming the data from the network, then dis.available() will be <= 0
				 *     and we need to _block_ on availability here rather than ending.
				 *  Therefore switching to using EOFException throwing to catch when end of
				 *     stream is reached
				 *
				 *  This seems to fix the problem, except I'm not sure what to do about cleanup --
				 *  can't call close() on the inputstream(s)...
				 *
				 */
				// Loop will usually be ended by EOFException, but
				// can also be interrupted by Thread.interrupt()
				Thread thread = Thread.currentThread();
				while (! thread.isInterrupted()) {
					//
					String name = dis.readUTF();
					String chrom_name = dis.readUTF();
					String strand = dis.readUTF();
					boolean forward = (strand.equals("+") || (strand.equals("++")));
					int tmin = dis.readInt();
					int tmax = dis.readInt();
					//int tlength = tmax - tmin;
					int cmin = dis.readInt();
					int cmax = dis.readInt();
					//int clength = cmax - cmin;
					int ecount = dis.readInt();
					int[] emins = new int[ecount];
					int[] emaxs = new int[ecount];
					for (int i=0; i<ecount; i++) {
						emins[i] = dis.readInt();
					}
					for (int i=0; i<ecount; i++) {
						emaxs[i] = dis.readInt();
					}

					MutableAnnotatedBioSeq chromseq = seq_group.getSeq(chrom_name);

					if (chromseq == null) {
						chromseq = seq_group.addSeq(chrom_name, 0);
					}

					UcscGeneSym sym = new UcscGeneSym(annot_type, name, name, chromseq, forward,
							tmin, tmax, cmin, cmax, emins, emaxs);

					if (seq_group != null) {
						seq_group.addToIndex(name, sym);
					}
					results.add(sym);

					if (tmax > chromseq.getLength()) {
						chromseq.setLength(tmax);
					}

					if (annotate_seq)  {
						SimpleSymWithProps parent_sym = (SimpleSymWithProps)chrom2sym.get(chrom_name);
						if (parent_sym == null) {
							parent_sym = new SimpleSymWithProps();
							parent_sym.addSpan(new SimpleSeqSpan(0, chromseq.getLength(), chromseq));
							parent_sym.setProperty("method", annot_type);
							//              System.out.println("method: " + annot_type);
							parent_sym.setProperty("preferred_formats", pref_list);
							parent_sym.setProperty(SimpleSymWithProps.CONTAINER_PROP, Boolean.TRUE);
							annots.add(parent_sym);
							chrom2sym.put(chrom_name, parent_sym);
						}
						//TODO: Make sure parent_sym is long enough to encompas all its children
						parent_sym.addChild(sym);
					}
					total_exon_count += ecount;
					count++;
				}
		//	}
		}
		catch (EOFException ex) {
			// System.out.println("end of file reached, file successfully loaded");
			reached_EOF = true;
		}
		catch (IOException ioe) {
			throw ioe;
		}
		catch (Exception ex) {
			String message = "Problem processing BGN file";
			String m1 = ex.getMessage();
			if (m1 != null && m1.length() > 0) {
				message += ": "+m1;
			}
			IOException ioe = new IOException(message);
			ioe.initCause(ex);
			throw ioe;
		}

		if (annotate_seq) {
			for (SeqSymmetry annot : annots) {
				MutableAnnotatedBioSeq chromseq = annot.getSpan(0).getBioSeq();
				chromseq.addAnnotation(annot);
			}
		}
		if (DEBUG) {
			System.out.println("bgn file load time: " + tim.read() / 1000f);
			System.out.println("transcript count = " + count);
			System.out.println("exon count = " + total_exon_count);
			if (count > 0) {
				System.out.println("average exons / transcript = " +
						((double) total_exon_count / (double) count));
			}
		}
		if (! reached_EOF) {
			System.out.println("File loading was terminated early.");
		}
		return results;
	}

	/**
	 *  Writes a single SeqSymmetry to the output stream in BGN format.
	 *  If the SeqSymmetry implements SupportsCdsSpan, then the CDS
	 *  span information will be written.  If not, then the BGN format is
	 *  probably not the best format to use, but since that can still be useful,
	 *  this routine will treat the entire span as the CDS.
	 */
	public void writeSymmetry(SeqSymmetry gsym, MutableAnnotatedBioSeq targetSeq, OutputStream os) throws IOException {
		SeqSpan tspan = gsym.getSpan(0);
		SeqSpan cspan;
		String name;
		if (gsym instanceof UcscGeneSym) {
			UcscGeneSym ugs = (UcscGeneSym) gsym;
			cspan = ugs.getCdsSpan();
			name = ugs.getName();
		}
		else if (gsym instanceof SupportsCdsSpan) {
			cspan = ((SupportsCdsSpan) gsym).getCdsSpan();
			name = gsym.getID();
		}
		else {
			cspan = tspan;
			name = gsym.getID();
		}
		MutableAnnotatedBioSeq seq = tspan.getBioSeq();
		DataOutputStream dos = null;
		if (os instanceof DataOutputStream) {
			dos = (DataOutputStream)os;
		} else {
			dos = new DataOutputStream(os);
		}
		dos.writeUTF(name);
		dos.writeUTF(seq.getID());
		if (tspan.isForward()) { dos.writeUTF("+"); }
		else { dos.writeUTF("-"); }
		dos.writeInt(tspan.getMin());
		dos.writeInt(tspan.getMax());
		dos.writeInt(cspan.getMin());
		dos.writeInt(cspan.getMax());
		dos.writeInt(gsym.getChildCount());
		int childcount = gsym.getChildCount();
		for (int k=0; k<childcount; k++) {
			SeqSpan child = gsym.getChild(k).getSpan(seq);
			dos.writeInt(child.getMin());
		}
		for (int k=0; k<childcount; k++) {
			SeqSpan child = gsym.getChild(k).getSpan(seq);
			dos.writeInt(child.getMax());
		}
	}

	/**
	 *  Writes a list of annotations to a file in BGN format.
	 *  @param annots  a List of SeqSymmetry objects, preferably implementing SupportsCdsSpan
	 */
	public void writeBinary(String file_name, List<SeqSymmetry> annots) throws IOException {
		DataOutputStream dos = null;
		try {
			dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(file_name))));
			for (SeqSymmetry gsym : annots) {
				writeSymmetry(gsym, null, dos);
			}
		}
		finally {
			GeneralUtils.safeClose(dos);
		}
	}

	public void convertTextToBinary(String text_file, String bin_file, AnnotatedSeqGroup seq_group) {
		System.out.println("loading file: " + text_file);
		int count = 0;
		long flength = 0;
		//    int bread = 0;
		int max_tlength = Integer.MIN_VALUE;
		int max_exons = Integer.MIN_VALUE;
		int max_spliced_length = Integer.MIN_VALUE;
		int total_exon_count = 0;
		int biguns = 0;
		int big_spliced = 0;

		Timer tim = new Timer();
		tim.start();
		DataOutputStream dos = null;
		DataInputStream dis = null;
		try {
			File fil = new File(text_file);
			flength = fil.length();
			FileInputStream fis = new FileInputStream(fil);
			BufferedInputStream bis = new BufferedInputStream(fis);
			if (use_byte_buffer) {
				byte[] bytebuf = new byte[(int)flength];
				bis.read(bytebuf);
				ByteArrayInputStream bytestream = new ByteArrayInputStream(bytebuf);
				dis = new DataInputStream(bytestream);
			}
			else {
				dis = new DataInputStream(bis);
			}

			if (write_from_text) {
				File outfile = new File(bin_file);
				FileOutputStream fos = new FileOutputStream(outfile);
				BufferedOutputStream bos = new BufferedOutputStream(fos);
				dos = new DataOutputStream(bos);
			}

			writeLines(dis, count, seq_group, dos, biguns, total_exon_count, max_exons, max_tlength, tim, flength, max_spliced_length, big_spliced);

			if (write_from_text) {
				dos.flush();
				dos.close();
			}

			
		}
		catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(dis);
			GeneralUtils.safeClose(dos);
		}
		
	}

	private void writeLines(DataInputStream dis, int count, AnnotatedSeqGroup seq_group, DataOutputStream dos, int biguns, int total_exon_count, int max_exons, int max_tlength, Timer tim, long flength, int max_spliced_length, int big_spliced) throws NumberFormatException, IOException {
		String line = null;
		while ((line = dis.readLine()) != null) {
			count++;
			String[] fields = line_regex.split(line);
			String name = fields[0];
			//        name_hash.put(name, null);
			String chrom = fields[1];
			if (seq_group != null && seq_group.getSeq(chrom) == null) {
				System.out.println("sequence not recognized, ignoring: " + chrom);
				continue;
			}
			String strand = fields[2];
			String txStart = fields[3]; // min base of transcript on genome
			String txEnd = fields[4]; // max base of transcript on genome
			String cdsStart = fields[5]; // min base of CDS on genome
			String cdsEnd = fields[6]; // max base of CDS on genome
			String exonCount = fields[7]; // number of exons
			String exonStarts = fields[8];
			String exonEnds = fields[9];
			int tmin = Integer.parseInt(txStart);
			int tmax = Integer.parseInt(txEnd);
			int tlength = tmax - tmin;
			int cmin = Integer.parseInt(cdsStart);
			int cmax = Integer.parseInt(cdsEnd);
			//int clength = cmax - cmin;
			int ecount = Integer.parseInt(exonCount);
			String[] emins = emin_regex.split(exonStarts);
			String[] emaxs = emax_regex.split(exonEnds);
			if (write_from_text) {
				dos.writeUTF(name);
				dos.writeUTF(chrom);
				dos.writeUTF(strand);
				dos.writeInt(tmin);
				dos.writeInt(tmax);
				dos.writeInt(cmin);
				dos.writeInt(cmax);
				dos.writeInt(ecount);
			}
			if (ecount != emins.length || ecount != emaxs.length) {
				System.out.println("EXON COUNTS DON'T MATCH UP FOR " + name + " !!!");
			} else {
				//int spliced_length = 0;
				for (int i = 0; i < ecount; i++) {
					int emin = Integer.parseInt(emins[i]);
					if (write_from_text) {
						dos.writeInt(emin);
					}
				}
				for (int i = 0; i < ecount; i++) {
					int emax = Integer.parseInt(emaxs[i]);
					if (write_from_text) {
						dos.writeInt(emax);
					}
				}
			}
			if (tlength >= 500000) {
				biguns++;
			}
			total_exon_count += ecount;
			max_exons = Math.max(max_exons, ecount);
			max_tlength = Math.max(max_tlength, tlength);
		}
		if (DEBUG) {
			System.out.println("load time: " + tim.read() / 1000f);
			System.out.println("line count = " + count);
			System.out.println("file length = " + flength);
			System.out.println("max genomic transcript length: " + max_tlength);
			System.out.println("max exons in single transcript: " + max_exons);
			System.out.println("total exons: " + total_exon_count);
			System.out.println("max spliced transcript length: " + max_spliced_length);
			System.out.println("spliced transcripts > 65000: " + big_spliced);
		}
	}



	/** For testing. */
	/*
	   public static void main(String[] args) {
	   String text_file = null;
	   String bin_file = null;
	   if (args.length == 2) {
	   text_file = args[0];
	   bin_file = args[1];
	   } else {
	   System.out.println("Usage:  java ... BgnParser <text infile> <binary outfile>");
	   System.exit(1);
	   }
	   BgnParser test = new BgnParser();
//    test.readTextTest(text_file, null);
test.convertTextToBinary(text_file, bin_file, null);
}*/


/**
 *  Implementing AnnotationWriter interface to write out annotations
 *    to an output stream as "binary UCSC gene" (.bgn)
 **/
public boolean writeAnnotations(java.util.Collection<SeqSymmetry> syms, MutableAnnotatedBioSeq seq,
		String type, OutputStream outstream) {
	System.out.println("in BgnParser.writeAnnotations()");
	boolean success = true;
	try {
		DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(outstream));
		Iterator iterator = syms.iterator();
		while (iterator.hasNext()) {
			SeqSymmetry sym = (SeqSymmetry)iterator.next();
			if (! (sym instanceof UcscGeneSym)) {
				System.err.println("trying to output non-UcscGeneSym as UcscGeneSym!");
			}
			writeSymmetry((UcscGeneSym)sym, null, dos);
		}
		dos.flush();
	}
	catch (Exception ex) {
		ex.printStackTrace();
		success = false;
	}
	return success;
}

	public Comparator getComparator(MutableAnnotatedBioSeq seq) {
		return new SeqSymMinComparator((BioSeq)seq);
	}

	public int getMin(SeqSymmetry sym, MutableAnnotatedBioSeq seq) {
		SeqSpan span = sym.getSpan(seq);
		return span.getMin();
	}

	public int getMax(SeqSymmetry sym, MutableAnnotatedBioSeq seq) {
		SeqSpan span = sym.getSpan(seq);
		return span.getMax();
	}

	public List<String> getFormatPrefList() {
		return BgnParser.pref_list;
	}

	/**
	 *  Implementing AnnotationWriter interface to write out annotations
	 *    to an output stream as "binary UCSC gene".
	 **/
	public String getMimeType() {
		return "binary/bgn";
	}


}
