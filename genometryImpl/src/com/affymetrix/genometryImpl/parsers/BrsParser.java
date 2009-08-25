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

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.MutableAnnotatedBioSeq;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.SingletonGenometryModel;
import com.affymetrix.genometryImpl.UcscGeneSym;
import com.affymetrix.genometryImpl.comparator.SeqSymMinComparator;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.Timer;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * BrsParser can convert UCSC-style RefFlat database table dumps into
 * binary refseq format (".brs").
 *
 * Also used to read in binary refseq format.
 *
 * Typical Command-line Usage to convert from RefFlat text files to brs files
 * java -classpath genometry.jar:genometryImpl.jar com.affymetrix.genometryImpl.parsers.BrsParser
 * reflat_input_file brs_output_file.brs
 *
 * This class can handle both "refFlat.txt" and "refGene.txt",
 * but "refFlat.txt" is preferred.
 * (refFlat contains gene names, while refGene does not.)
 */
public final class BrsParser implements AnnotationWriter, IndexWriter  {

	static List<String> pref_list = new ArrayList<String>();
	static {
		pref_list.add("brs");
	}

	final private static boolean DEBUG = false;

	boolean use_byte_buffer = true;
	boolean write_from_text = true;
	boolean write_objects = false;

	// .bin1:
	//     geneName UTF8
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
	// .bin4:
	//     same as bin1 but doesn't write chrom and strand strings to file
	// .bin5:
	//     same as .bin4 but writes geneName and name as byte for length, and then writeBytes(),
	//     rather than using UTF-8


	static final Pattern line_regex = Pattern.compile("\t");
	static final Pattern emin_regex = Pattern.compile(",");
	static final Pattern emax_regex = Pattern.compile(",");

	int max_genes = 50000;  // guesstimate...
	ArrayList chromosomes = new ArrayList();

	public List<SeqSymmetry> parse(String file_name, String annot_type, AnnotatedSeqGroup seq_group, Integer annot_id)
		throws IOException {
		System.out.println("loading file: " + file_name);
		List<SeqSymmetry> result = null;
		FileInputStream fis = null;
		try {
			File fil = new File(file_name);
			long blength = fil.length();
			fis = new FileInputStream(fil);
			result = parse(fis, annot_type, seq_group, true, blength, annot_id);
		} finally {
			GeneralUtils.safeClose(fis);
		}
		return result;
	}

	public List<SeqSymmetry> parse(InputStream istr, String annot_type, AnnotatedSeqGroup seq_group, Integer annot_id)
		throws IOException {
		return parse(istr, annot_type, seq_group, true, annot_id);
	}

	public List<SeqSymmetry> parse(InputStream istr, String annot_type, AnnotatedSeqGroup seq_group, boolean annotate_seq, Integer annot_id)
		throws IOException {
		return parse(istr, annot_type, seq_group, annotate_seq, -1, annot_id);
	}

	/**
	 *  @param blength  buffer length, if unknown use -1;
	 */
	public List<SeqSymmetry> parse(InputStream istr, String annot_type,
			AnnotatedSeqGroup seq_group, boolean annotate_seq, long blength, Integer annot_id)
		throws IOException {
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
		int same_count = 0;
		BufferedInputStream bis = new BufferedInputStream(istr);
		DataInputStream dis = null;


		try {
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
			if (true) {
				// just keep looping till hitting end-of-file throws an EOFException
				Thread thread = Thread.currentThread();
				while (! thread.isInterrupted()) {
					String geneName = dis.readUTF();
					String name = dis.readUTF();
					String chrom_name = dis.readUTF();

					String strand = dis.readUTF();
					boolean forward = (strand.equals("+") || (strand.equals("++")));
					//          System.out.println("forward: " + forward);
					//          dis.skip(16);  // skip tmin, tmax, cmin, cmax (4 ints * 4bytes/int = 16 bytes);
					int tmin = dis.readInt();
					int tmax = dis.readInt();
					//          tstarts[count] = tmin;
					int tlength = tmax - tmin;
					int cmin = dis.readInt();
					int cmax = dis.readInt();
					int clength = cmax - cmin;
					int ecount = dis.readInt();
					//          pos += (20 + (ecount * 8));
					//          dis.skip(ecount * 8);
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
						chromseq = seq_group.addSeq(chrom_name, tmax);
					}
					UcscGeneSym sym = new UcscGeneSym(annot_type, geneName, name, chromseq, forward,
							tmin, tmax, cmin, cmax, emins, emaxs);

					if (geneName.length() != 0) {
						seq_group.addToIndex(geneName, sym);
					}
					if (name.length() != 0) {
						seq_group.addToIndex(name, sym);
					}


					results.add(sym);
					if (chromseq.getLength() < tmax) { chromseq.setLength(tmax); }

					if (annotate_seq) {
						SimpleSymWithProps parent_sym = (SimpleSymWithProps)chrom2sym.get(chrom_name);
						if (parent_sym == null) {
							parent_sym = new SimpleSymWithProps();
							parent_sym.addSpan(new SimpleSeqSpan(0, chromseq.getLength(), chromseq));
							parent_sym.setProperty("method", annot_type);
							parent_sym.setProperty("preferred_formats", pref_list);
							parent_sym.setProperty(SimpleSymWithProps.CONTAINER_PROP, Boolean.TRUE);
              parent_sym.setProperty(SimpleSymWithProps.ANNOT_ID, annot_id);
							annots.add(parent_sym);
							chrom2sym.put(chrom_name, parent_sym);
						}
						parent_sym.addChild(sym);
					}


					total_exon_count += ecount;
					count++;
				}
			}
		}
		catch (EOFException ex) {
			// System.out.println("end of file reached, file successfully loaded");
		}

		if (annotate_seq) {
			for (SeqSymmetry annot : annots) {
				MutableAnnotatedBioSeq chromseq = annot.getSpan(0).getBioSeq();
				chromseq.addAnnotation(annot);
			}
		}
		if (DEBUG) {
			SingletonGenometryModel.logInfo("load time: " + tim.read() / 1000f);
			SingletonGenometryModel.logInfo("transcript count = " + count);
			SingletonGenometryModel.logInfo("exon count = " + total_exon_count);
			if (count > 0) {
				SingletonGenometryModel.logInfo("average exons / transcript = " +
						((double) total_exon_count / (double) count));
			}
		}
		return results;
	}


	/*public void writeBinary(String file_name, List annots) {
		DataOutputStream dos = null;
		try {
			dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(file_name))));
			int acount = annots.size();
			for (int i=0; i<acount; i++) {
				UcscGeneSym gsym = (UcscGeneSym)annots.get(i);
				outputBrsFormat(gsym, dos);
			}
			dos.close();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(dos);
		}
	}*/

	public void outputBrsFormat(UcscGeneSym gsym, DataOutputStream dos) throws IOException {
		SeqSpan tspan = gsym.getSpan(0);
		SeqSpan cspan = gsym.getCdsSpan();
		MutableAnnotatedBioSeq seq = tspan.getBioSeq();
		dos.writeUTF(gsym.getGeneName());
		dos.writeUTF(gsym.getName());
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

	public void convertTextToBinary(String file_name, String bin_file) {
		SingletonGenometryModel.logInfo("loading file: " + file_name);
		int count = 0;
		long flength = 0;
		//    int bread = 0;
		int max_tlength = Integer.MIN_VALUE;
		int max_exons = Integer.MIN_VALUE;
		int total_exon_count = 0;
		int biguns = 0;
		int big_spliced = 0;

		Timer tim = new Timer();
		tim.start();

		DataOutputStream dos = null;
		DataInputStream dis = null;
		try {
			File fil = new File(file_name);
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
			String line;


			if (write_from_text) {
				File outfile = new File(bin_file);
				FileOutputStream fos = new FileOutputStream(outfile);
				BufferedOutputStream bos = new BufferedOutputStream(fos);
				dos = new DataOutputStream(bos);
			}
			// trying to handle both refFlat and refGene files
			//   if refGene, then file doesn't include geneName field (and can be detected because
			//   it has one fewer tab-delimited field)
			boolean text_includes_genename;
			while ((line = dis.readLine()) != null) {
				count++;
				int field_index = 0;
				String[] fields = line_regex.split(line);
				text_includes_genename = (fields.length > 10);
				String geneName = null;
				if (text_includes_genename) { geneName = fields[field_index++]; }
				String name = fields[field_index++];
				//        gene_names[count] = geneName;
				//        names[count] = name;
				//        name_hash.put(geneName, null);
				//        name_hash.put(name, null);
				String chrom = fields[field_index++];
				String strand = fields[field_index++];
				String txStart = fields[field_index++];  // min base of transcript on genome
				String txEnd = fields[field_index++];  // max base of transcript on genome
				String cdsStart = fields[field_index++];  // min base of CDS on genome
				String cdsEnd = fields[field_index++];  // max base of CDS on genome
				String exonCount = fields[field_index++]; // number of exons
				String exonStarts = fields[field_index++];
				String exonEnds = fields[field_index++];
				int tmin = Integer.parseInt(txStart);
				int tmax = Integer.parseInt(txEnd);
				int tlength = tmax - tmin;
				int cmin = Integer.parseInt(cdsStart);
				int cmax = Integer.parseInt(cdsEnd);
				int clength = cmax - cmin;
				int ecount = Integer.parseInt(exonCount);
				String[] emins = emin_regex.split(exonStarts);
				String[] emaxs = emax_regex.split(exonEnds);

				if (! text_includes_genename) { geneName = name; }
				if (write_from_text) {
					dos.writeUTF(geneName);
					dos.writeUTF(name);
					/*
					   dos.write((byte)geneName.length());
					   dos.writeBytes(geneName);
					   dos.write((byte)name.length());
					   dos.writeBytes(name);
					   */
					dos.writeUTF(chrom);
					dos.writeUTF(strand);
					dos.writeInt(tmin);
					dos.writeInt(tmax);
					dos.writeInt(cmin);
					dos.writeInt(cmax);
					dos.writeInt(ecount);
				}

				if (ecount != emins.length || ecount != emaxs.length) {
					System.out.println("EXON COUNTS DON'T MATCH UP FOR " + geneName + " !!!");
				}
				else {
					int spliced_length = 0;
					for (int i=0; i<ecount; i++) {
						int emin = Integer.parseInt(emins[i]);
						if (write_from_text) { dos.writeInt(emin); }
					}
					for (int i=0; i<ecount; i++) {
						int emax = Integer.parseInt(emaxs[i]);
						if (write_from_text) { dos.writeInt(emax); }
					}
				}
				if (tlength >= 500000) {
					biguns++;
				}

				total_exon_count += ecount;
				max_exons = Math.max(max_exons, ecount);
				max_tlength = Math.max(max_tlength, tlength);
				//        name_hash.put(fields[0], fields[0]);
			}

			if (write_from_text) {
				dos.flush();
				dos.close();
			}
			// simplest load, straight into a byte array
			//      byte[] bytebuf = new byte[(int)flength];
			//      bread = bis.read(bytebuf);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(dis);
			GeneralUtils.safeClose(dos);
		}

		System.out.println("load time: " + tim.read()/1000f);
		System.out.println("line count = " + count);
		System.out.println("file length = " + flength);
		//    System.out.println("bytes read = " + bread);
		System.out.println("max genomic transcript length: " + max_tlength);
		//    System.out.println("biguns: " + biguns);
		System.out.println("max exons in single transcript: " + max_exons);
		System.out.println("total exons: " + total_exon_count);
		System.out.println("spliced transcripts > 65000: " + big_spliced);
		//    Enumeration enum = name_hash.keys();
		//    int namecount = 0;
		/*
		   while (enum.hasMoreElements() && namecount < 10) {
		   String name = (String)enum.nextElement();
		   System.out.println(name);
		   namecount++;
		   }
		   */
	}

/**
 *  Reads a text file and writes a binary file.
 *<p>
 *  Typical Command-line Usage to convert from RefFlat text files to brs files
 *  <code>java -classpath genometry.jar:genometryImpl.jar com.affymetrix.genometryImpl.parsers.BrsParser
 *    refFlat.txt refseq.brs</code>
 *<p>
 */
public static void main(String[] args) {
	String text_file = null;
	String bin_file = null;
	if (args.length == 2) {
		text_file = args[0];
		bin_file = args[1];
	} else {
		System.out.println("Usage:  java ... BrsParser <text infile> <binary outfile>");
		System.exit(1);
	}
	BrsParser test = new BrsParser();
	test.convertTextToBinary(text_file, bin_file);
	System.exit(0);
}

/**
 *  Implementing AnnotationWriter interface to write out annotations
 *    to an output stream as "binary UCSC refseq gene". File extension ".brs".
 **/
public boolean writeAnnotations(Collection<SeqSymmetry> syms, MutableAnnotatedBioSeq seq,
		String type, OutputStream outstream) {
	System.out.println("in BrsParser.writeAnnotations()");
	boolean success = true;
	try {
		DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(outstream));
		Iterator iterator = syms.iterator();
		while (iterator.hasNext()) {
			SeqSymmetry sym = (SeqSymmetry)iterator.next();
			if (! (sym instanceof UcscGeneSym)) {
				System.err.println("trying to output non-UcscGeneSym as UcscGeneSym!");
			}
			outputBrsFormat((UcscGeneSym)sym, dos);
		}
		dos.flush();
	}
	catch (Exception ex) {
		ex.printStackTrace();
		success = false;
	}
	return success;
}


	public void writeSymmetry(SeqSymmetry sym, MutableAnnotatedBioSeq seq, OutputStream os) throws IOException {
		DataOutputStream dos = null;
		if (os instanceof DataOutputStream) {
			dos = (DataOutputStream)os;
		} else {
			dos = new DataOutputStream(os);
		}
		outputBrsFormat((UcscGeneSym)sym, dos);
	}

	public List parse(DataInputStream dis, String annot_type, AnnotatedSeqGroup group) {
		try {
			return this.parse((InputStream) dis, annot_type, group, null);
		} catch (IOException ex) {
			Logger.getLogger(BrsParser.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
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
		return BrsParser.pref_list;
	}

	/**
	 *  Implementing AnnotationWriter interface to write out annotations
	 *    to an output stream as "binary UCSC refseq gene"
	 **/
	public String getMimeType() {
		return "binary/brs";
	}

}
