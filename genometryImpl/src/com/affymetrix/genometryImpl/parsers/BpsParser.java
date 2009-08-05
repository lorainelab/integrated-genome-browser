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

import com.affymetrix.genometryImpl.util.Timer;

import com.affymetrix.genometry.*;
import com.affymetrix.genometry.span.*;
import com.affymetrix.genometry.util.SeqUtils;
import com.affymetrix.genometryImpl.comparator.UcscPslComparator;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSymmetryConverter;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.UcscPslSym;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import java.nio.channels.FileChannel;

public final class BpsParser implements AnnotationWriter  {
	private static final boolean DEBUG = false;
	static List<String> pref_list = new ArrayList<String>();
	static {
		pref_list.add("bps");
		pref_list.add("psl");
	}

	static boolean main_batch_mode = false; // main() should run in batch mode (processing PSL files in psl_input_dir)
	static boolean write_from_text = true; // main() should read psl file from text_file and write bps to bin_file
	static boolean read_from_bps = false;  // main() should read bps file bin_file
	static boolean use_byte_buffer = true;
	static boolean REPORT_LOAD_STATS = true;

	static String user_dir = System.getProperty("user.dir");

	// .bps is for "binary PSL format"
	static String default_annot_type = "spliced_EST";

	/*
	 *  new alternative
	 *  given a psl_input_dir:
	 *     for each file in psl_input_dir,
	 *        if file ends with ".psl"
	 *             assume its a PSL file
	 *             load via PSLParser.parse()
	 *             output via BpsParser.writeBinaryFile()
	 */
	static String psl_input_dir = user_dir + "/moredata/Drosophila_Jan_2003/";
	static String bps_output_dir = user_dir + "/query_server_dro/Drosophila_Jan_2003/";

	/*  PSL format fields (from UcscPslSym)
		int matches;
		int mismatches;
		int repmatches; // should be derivable w/o residues
		int ncount;
		int qNumInsert;  // should be derivable w/o residues
		int qBaseInsert; // should be derivable w/o residues
		int tNumInsert;  // should be derivable w/o residues
		int tBaseInsert; // should be derivable w/o residues
		boolean qforward;
		boolean tforward;  // for mouse only???
		String qname;
		int qsize;
		int qmin;
		int qmax;
		String tname;
		int tsize;
		int tmin;
		int tmax;
		int blockcount; // should be redundant
		int[] blockSizes;
		int[] qmins;
		int[] qmaxs;
		*/

	static int estimated_count = 80000;

	public static void main(String[] args) throws IOException {
		//    BpsParser test = new BpsParser();
		if (write_from_text) {
			if (main_batch_mode) {
				File input_dir = new File(psl_input_dir);
				File[] fils = input_dir.listFiles();
				for (int i=0; i<fils.length; i++) {
					File fil = fils[i];
					String in_path = fil.getPath();
					String in_name = fil.getName();
					if (in_name.endsWith(".psl")) {
						SingletonGenometryModel.logInfo("processing PSL file: " + in_path);
						String barename;
						if (in_name.endsWith(".psl.psl")) {
							barename = in_name.substring(0, in_name.lastIndexOf(".psl.psl"));
						}
						else {
							barename = in_name.substring(0, in_name.lastIndexOf(".psl"));
						}
						System.out.println("bare name: " + barename);
						String out_path = bps_output_dir + barename + ".bps";
						System.out.println("output file: " + out_path);
						convertPslToBps(in_path, out_path);
					}
				}
			}
			else {
				if (args.length == 2) {
					String text_file = args[0];
					String bin_file = args[1];
					convertPslToBps(text_file, bin_file);
				}
				else {
					System.out.println("Usage:  java ... BpsParser <text infile> <binary outfile>");
					System.exit(1);
				}
			}
		}
		if (read_from_bps) {
			AnnotatedSeqGroup seq_group = SingletonGenometryModel.getGenometryModel().addSeqGroup("Test Group");

			String bin_file = args[0];
			List syms = parse(bin_file, default_annot_type, seq_group);
			int symcount = syms.size();
			SingletonGenometryModel.logInfo("total sym count: " + symcount);
			int[] blockcount = new int[100];
			for (int i=0; i<symcount; i++) {
				SeqSymmetry sym = (SeqSymmetry)syms.get(i);
				int childcount = sym.getChildCount();
				blockcount[childcount]++;
			}
			for (int i=0; i<blockcount.length; i++) {
				if (blockcount[i] != 0) {
					SingletonGenometryModel.logInfo("syms with " + i + " children: " + blockcount[i]);
				}
			}
		}
	}


	public static void convertPslToBps(String psl_in, String bps_out)  {
		System.out.println("reading text psl file");
		List<SeqSymmetry> psl_syms = readPslFile(psl_in);
		System.out.println("done reading text psl file, annot count = " + psl_syms.size());
		System.out.println("writing binary psl file");
		//    writeBpsFile(psl_syms, bps_out);
		writeBinary(bps_out, psl_syms);
		System.out.println("done writing binary psl file");
	}


	public static List parse(String file_name, String annot_type, AnnotatedSeqGroup seq_group)
		throws IOException {
		SingletonGenometryModel.logInfo("loading file: " + file_name);
		List results = null;
		FileInputStream fis = null;
		DataInputStream dis = null;
		try {
			File fil = new File(file_name);
			long flength = fil.length();
			fis = new FileInputStream(fil);
			BufferedInputStream bis = new BufferedInputStream(fis);

			if (use_byte_buffer) {
				byte[] bytebuf = new byte[(int)flength];
				bis.read(bytebuf);
				//        fis.read(bytebuf);
				ByteArrayInputStream bytestream = new ByteArrayInputStream(bytebuf);
				dis = new DataInputStream(bytestream);
			}
			else {
				dis = new DataInputStream(bis);
			}
			results = parse(dis, annot_type, (AnnotatedSeqGroup) null, seq_group, false, true);
		}
		finally {
			GeneralUtils.safeClose(dis);
			GeneralUtils.safeClose(fis);
		}
		return results;
	}

	/** Reads binary PSL data from the given stream.  Note that this method <b>can</b>
	 *  be interrupted early by Thread.interrupt().  The input stream will always be closed
	 *  before exiting this method.
	 */
	public static List<UcscPslSym> parse(DataInputStream dis, String annot_type,
			AnnotatedSeqGroup query_group, AnnotatedSeqGroup target_group,
			boolean annot_query, boolean annot_target)
		throws IOException {

		// make temporary seq groups to avoid null pointers later
		if (query_group == null) {
			query_group = new AnnotatedSeqGroup("Query");
			query_group.setUseSynonyms(false);
		}
		if (target_group == null) {
			target_group = new AnnotatedSeqGroup("Target");
			target_group.setUseSynonyms(false);
		}

		int total_block_count = 0;
		HashMap<String,SeqSymmetry> target2sym = new HashMap<String,SeqSymmetry>(); // maps target chrom name to top-level symmetry
		HashMap<String,SeqSymmetry> query2sym = new HashMap<String,SeqSymmetry>(); // maps query chrom name to top-level symmetry
		ArrayList<UcscPslSym> results = new ArrayList<UcscPslSym>(estimated_count);
		int count = 0;
		int same_count = 0;
		Timer tim = new Timer();
		tim.start();
		boolean reached_EOF = false;
		try {
			Thread thread = Thread.currentThread();
			// Loop will usually be ended by EOFException, but
			// can also be interrupted by Thread.interrupt()
			while (! thread.isInterrupted()) {
				int matches = dis.readInt();
				int mismatches = dis.readInt();
				int repmatches = dis.readInt();
				int ncount = dis.readInt();
				int qNumInsert = dis.readInt();
				int qBaseInsert = dis.readInt();
				int tNumInsert = dis.readInt();
				int tBaseInsert = dis.readInt();
				boolean qforward = dis.readBoolean();
				String qname = dis.readUTF();
				int qsize = dis.readInt();
				int qmin = dis.readInt();
				int qmax = dis.readInt();

				MutableAnnotatedBioSeq queryseq = query_group.getSeq(qname);
				if (queryseq == null)  {
					queryseq = query_group.addSeq(qname, qsize);
				}
				if (queryseq.getLength() < qsize) { queryseq.setLength(qsize); }

				String tname = dis.readUTF();
				int tsize = dis.readInt();
				int tmin = dis.readInt();
				int tmax = dis.readInt();


				MutableAnnotatedBioSeq targetseq = target_group.getSeq(tname);
				if (targetseq == null) {
					targetseq = target_group.addSeq(tname, tsize);
				}
				if (targetseq.getLength() < tsize) { targetseq.setLength(tsize); }

				int blockcount = dis.readInt();
				int[] blockSizes = new int[blockcount];
				int[] qmins = new int[blockcount];
				int[] tmins = new int[blockcount];
				for (int i=0; i<blockcount; i++) {
					blockSizes[i] = dis.readInt();
				}
				for (int i=0; i<blockcount; i++) {
					qmins[i] = dis.readInt();
				}
				for (int i=0; i<blockcount; i++) {
					tmins[i] = dis.readInt();
				}
				total_block_count += blockcount;
				count++;

				UcscPslSym sym =
					new UcscPslSym(annot_type, matches, mismatches, repmatches, ncount,
							qNumInsert, qBaseInsert, tNumInsert, tBaseInsert, qforward,
							queryseq, qmin, qmax, targetseq, tmin, tmax,
							blockcount, blockSizes, qmins, tmins);
				results.add(sym);


				if (annot_query) {
					SimpleSymWithProps query_parent_sym = (SimpleSymWithProps)query2sym.get(qname);
					if (query_parent_sym == null) {
						query_parent_sym = new SimpleSymWithProps();
						query_parent_sym.addSpan(new SimpleSeqSpan(0, queryseq.getLength(), queryseq));
						query_parent_sym.setProperty("method", annot_type);
						query_parent_sym.setProperty("preferred_formats", pref_list);
						query_parent_sym.setProperty(SimpleSymWithProps.CONTAINER_PROP, Boolean.TRUE);
						queryseq.addAnnotation(query_parent_sym);
						query2sym.put(qname, query_parent_sym);
					}
					query_group.addToIndex(sym.getID(), sym);
					query_parent_sym.addChild(sym);
				}

				if (annot_target) {
					SimpleSymWithProps target_parent_sym = (SimpleSymWithProps)target2sym.get(tname);
					if (target_parent_sym == null) {
						target_parent_sym = new SimpleSymWithProps();
						target_parent_sym.addSpan(new SimpleSeqSpan(0, targetseq.getLength(), targetseq));
						target_parent_sym.setProperty("method", annot_type);
						target_parent_sym.setProperty("preferred_formats", pref_list);
						target_parent_sym.setProperty(SimpleSymWithProps.CONTAINER_PROP, Boolean.TRUE);
						targetseq.addAnnotation(target_parent_sym);
						target2sym.put(tname, target_parent_sym);
					}
					target_parent_sym.addChild(sym);
					target_group.addToIndex(sym.getID(), sym);
				}
			}
		}
		catch (EOFException ex) {
			reached_EOF = true;
		}
		finally {try { dis.close(); } catch (Exception ex) {}}

		long timecount = tim.read();
		if (REPORT_LOAD_STATS) {
			SingletonGenometryModel.logInfo("PSL binary file load time: " + timecount/1000f);
			if (! reached_EOF) {
				SingletonGenometryModel.logInfo("File loading was terminated early.");
			}
		}
		if (count <= 0) {
			SingletonGenometryModel.logInfo("PSL total counts <= 0 ???");
		}
		else {
			tim.start();
			UcscPslComparator comp = new UcscPslComparator();
			Collections.sort(results, comp);
			if (REPORT_LOAD_STATS) {
				SingletonGenometryModel.logInfo("PSL sort time: " + tim.read()/1000f);
				SingletonGenometryModel.logInfo("PSL alignment count = " + count);
				SingletonGenometryModel.logInfo("PSL total block count = " + total_block_count);
				SingletonGenometryModel.logInfo("PSL average blocks / alignment = " +
						((double)total_block_count/(double)count));
			}
		}
		return results;
	}


	private static List<SeqSymmetry> readPslFile(String file_name) {
		Timer tim = new Timer();
		tim.start();

		List<SeqSymmetry> results = null;
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		try  {
			File fil = new File(file_name);
			long flength = fil.length();
			fis = new FileInputStream(fil);
			InputStream istr = null;
			if (use_byte_buffer) {
				byte[] bytebuf = new byte[(int)flength];
				bis = new BufferedInputStream(fis);
				bis.read(bytebuf);
				bis.close();
				ByteArrayInputStream bytestream = new ByteArrayInputStream(bytebuf);
				istr = bytestream;
			}
			else {
				istr = fis;
			}
			PSLParser parser = new PSLParser();
			// don't bother annotating the sequences, just get the list of syms
			results = parser.parse(istr, file_name, null, null, false, false);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(bis);
			GeneralUtils.safeClose(fis);
		}
		//long timecount = tim.read();
		SingletonGenometryModel.logInfo("finished reading PSL file, time to read = " + (tim.read()/1000f));
		return results;
	}

	public static void writeBinary(String file_name, List syms)  {
		try  {
			File outfile = new File(file_name);
			FileOutputStream fos = new FileOutputStream(outfile);
			BufferedOutputStream bos = new BufferedOutputStream(fos);
			DataOutputStream dos = new DataOutputStream(bos);
			int symcount = syms.size();
			for (int i=0; i<symcount; i++) {
				UcscPslSym psl = (UcscPslSym)syms.get(i);
				psl.outputBpsFormat(dos);
			}
			dos.close();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}


	/**
	 *  Implementing AnnotationWriter interface to write out annotations
	 *    to an output stream as "binary PSL".
	 **/
	public boolean writeAnnotations(Collection<SeqSymmetry> syms, MutableAnnotatedBioSeq seq,
			String type, OutputStream outstream) {
		//    SingletonGenometryModel.logInfo("in BpsParser.writeAnnotations()");
		boolean success = true;
		DataOutputStream dos = null;
		try {
			dos = new DataOutputStream(new BufferedOutputStream(outstream));
			Iterator iterator = syms.iterator();
			while (iterator.hasNext()) {
				SeqSymmetry sym = (SeqSymmetry)iterator.next();
				if (! (sym instanceof UcscPslSym)) {
					int spancount = sym.getSpanCount();
					if (sym.getSpanCount() == 1) {
						sym = SeqSymmetryConverter.convertToPslSym(sym, type, seq);
					}
					else {
						MutableAnnotatedBioSeq seq2 = SeqUtils.getOtherSeq(sym, seq);
						sym = SeqSymmetryConverter.convertToPslSym(sym, type, seq2, seq);
					}
				}
				((UcscPslSym)sym).outputBpsFormat(dos);
			}
			dos.flush();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			success = false;
		}
		finally {
			GeneralUtils.safeClose(dos);
		}
		return success;
	}


	/**
	 * Create a file of annotations, and index its entries.
	 * @param syms -- a sorted list of annotations (on one chromosome)
	 * @param fos -- stream to write file to.
	 * @param min -- int array of TargetMins in annotation list.
	 * @param max -- int array of TargetMaxes in annotation list.
	 * @param fileIndices -- long array of file pointers in annotation list.
	 * Note there is an extra file index, to allow us to record both beginning and ends of lines.
	 * @return -- success or failures
	 */
	public static boolean writeIndexedAnnotations(List<UcscPslSym> syms, FileOutputStream fos,
			int min[], int max[], long[] fileIndices) {
		if (DEBUG){
			System.out.println("in BpsParser.writeIndexedAnnotations()");
		}
		DataOutputStream dos = null;
		try {
			dos = new DataOutputStream(fos);
			FileChannel fChannel = fos.getChannel();
			int index = 0;
			fileIndices[index] = 0;
			
			for (UcscPslSym sym : syms) {
				min[index] = sym.getTargetMin();
				max[index] = sym.getTargetMax();
				index++;
				sym.outputBpsFormat(dos);
				fileIndices[index] = fChannel.position();
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}


	/**
	 * Write out PSL annotations as binary PSL.
	 * This file is for a specific chromosome, and is sorted by the given comparator.
	 * @param syms - original list of annotations
	 * @param seq - specific chromosome
	 * @param outstream - stream to write to
	 * @return - success or failure
	 */
	public static boolean writeSortedAnnotationsForChrom(List<UcscPslSym> syms, BioSeq seq, OutputStream outstream, Comparator<UcscPslSym> UCSCcomp) {
		DataOutputStream dos = null;
		try {
			dos = new DataOutputStream(new BufferedOutputStream(outstream));
			List<UcscPslSym> symList = getSortedAnnotationsForChrom(syms, seq, UCSCcomp);
			for (UcscPslSym sym : symList) {
				sym.outputBpsFormat(dos);
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		finally {
			GeneralUtils.safeClose(dos);
		}
		return true;
	}

	/**
	 * Returns annotations for specific chromosome, sorted by comparator.
	 * @param syms - original list of annotations
	 * @param seq - specific chromosome
	 * @param UCSCcomp - comparator
	 * @return - sorted list of annotations
	 */
	public static List<UcscPslSym> getSortedAnnotationsForChrom(List<UcscPslSym> syms, BioSeq seq, Comparator<UcscPslSym> UCSCcomp) {
		Collections.sort(syms, UCSCcomp);

		List<UcscPslSym> results = new ArrayList<UcscPslSym>();
		for (UcscPslSym sym : syms) {
			if (sym.getTargetSeq() != seq) {
				continue;
			}
			// add the lines specifically with Target seq == seq.
			results.add(sym);
		}
		return results;
	}

	/**
	 *  Implementing AnnotationWriter interface to write out annotations
	 *    to an output stream as "binary PSL".
	 **/
	public String getMimeType() { return "binary/bps"; }
}

