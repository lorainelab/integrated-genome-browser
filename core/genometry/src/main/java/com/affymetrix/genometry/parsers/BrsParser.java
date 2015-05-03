/**
 * Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License").
 * A copy of the license must be included with any distribution of
 * this source code.
 * Distributions from Affymetrix, Inc., place this in the
 * IGB_LICENSE.html file.
 *
 * The license is also available at
 * http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.genometry.parsers;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.GenomeVersion;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.comparator.SeqSymMinComparator;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleSymWithProps;
import com.affymetrix.genometry.symmetry.impl.UcscGeneSym;
import com.affymetrix.genometry.util.GeneralUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * java -classpath genometry.jar:genometry.jar com.affymetrix.genometry.parsers.BrsParser
 * reflat_input_file brs_output_file.brs
 *
 * This class can handle both "refFlat.txt" and "refGene.txt",
 * but "refFlat.txt" is preferred.
 * (refFlat contains gene names, while refGene does not.)
 */
public final class BrsParser implements AnnotationWriter, IndexWriter, Parser {

    private static final List<String> pref_list = new ArrayList<>();

    static {
        pref_list.add("brs");
    }

    private static final boolean DEBUG = false;

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
    private static final Pattern line_regex = Pattern.compile("\t");
    private static final Pattern emin_regex = Pattern.compile(",");
    private static final Pattern emax_regex = Pattern.compile(",");

    /*public static List<SeqSymmetry> parse(String file_name, String annot_type, GenomeVersion seq_group)
     throws IOException {
     System.out.println("loading file: " + file_name);
     List<SeqSymmetry> result = null;
     FileInputStream fis = null;
     try {
     File fil = new File(file_name);
     long blength = fil.length();
     fis = new FileInputStream(fil);
     result = parse(fis, annot_type, seq_group, true, blength);
     } finally {
     GeneralUtils.safeClose(fis);
     }
     return result;
     }*/
    public static List<SeqSymmetry> parse(InputStream istr, String annot_type, GenomeVersion seq_group)
            throws IOException {
        return parse(istr, annot_type, seq_group, true);
    }

    public static List<SeqSymmetry> parse(InputStream istr, String annot_type, GenomeVersion seq_group, boolean annotate_seq)
            throws IOException {

        // annots is list of top-level parent syms (max 1 per seq in seq_group) that get
        //    added as annotations to the annotated BioSeqs -- their children
        //    are then actual transcript annotations
        List<SeqSymmetry> annots = new ArrayList<>();
        // results is list actual transcript annotations
        List<SeqSymmetry> results = new ArrayList<>(15000);
        // chrom2sym is temporary hash to put top-level parent syms in to map
        //     seq id to top-level symmetry, prior to adding these parent syms
        //     to the actual annotated seqs
        Map<String, SeqSymmetry> chrom2sym = new HashMap<>(); // maps chrom name to top-level symmetry

        int total_exon_count = 0;
        int count = 0;
        BufferedInputStream bis = new BufferedInputStream(istr);
        DataInputStream dis = null;

        try {
            dis = new DataInputStream(bis);
            // just keep looping till hitting end-of-file throws an EOFException
            Thread thread = Thread.currentThread();
            while (!thread.isInterrupted()) {
                String geneName = dis.readUTF();
                String name = dis.readUTF();
                String chrom_name = dis.readUTF();

                String strand = dis.readUTF();
                boolean forward = (strand.equals("+") || (strand.equals("++")));
                int tmin = dis.readInt();
                int tmax = dis.readInt();
                int cmin = dis.readInt();
                int cmax = dis.readInt();
                int ecount = dis.readInt();
                int[] emins = new int[ecount];
                int[] emaxs = new int[ecount];
                for (int i = 0; i < ecount; i++) {
                    emins[i] = dis.readInt();
                }
                for (int i = 0; i < ecount; i++) {
                    emaxs[i] = dis.readInt();
                }

                BioSeq chromseq = seq_group.getSeq(chrom_name);
                if (chromseq == null) {
                    chromseq = seq_group.addSeq(chrom_name, tmax, annot_type);
                }

                if (name.length() == 0 && geneName.length() == 0) {
                    name = seq_group.getName();
                }

                UcscGeneSym sym = new UcscGeneSym(annot_type, geneName, name, chromseq, forward,
                        tmin, tmax, cmin, cmax, emins, emaxs);

                if (geneName.length() != 0) {
//					seq_group.addToIndex(geneName, sym);
                }
                if (name.length() != 0) {
//					seq_group.addToIndex(name, sym);
                }

                results.add(sym);
                if (chromseq.getLength() < tmax) {
                    chromseq.setLength(tmax);
                }

                if (annotate_seq) {
                    SimpleSymWithProps parent_sym = (SimpleSymWithProps) chrom2sym.get(chrom_name);
                    if (parent_sym == null) {
                        parent_sym = new SimpleSymWithProps();
                        parent_sym.addSpan(new SimpleSeqSpan(0, chromseq.getLength(), chromseq));
                        parent_sym.setProperty("method", annot_type);
                        parent_sym.setProperty("preferred_formats", pref_list);
                        parent_sym.setProperty(SimpleSymWithProps.CONTAINER_PROP, Boolean.TRUE);
                        annots.add(parent_sym);
                        chrom2sym.put(chrom_name, parent_sym);
                    }
                    parent_sym.addChild(sym);
                }

                total_exon_count += ecount;
                count++;
            }
        } catch (EOFException ex) {
            // System.out.println("end of file reached, file successfully loaded");
        }

        if (annotate_seq) {
            for (SeqSymmetry annot : annots) {
                BioSeq chromseq = annot.getSpan(0).getBioSeq();
                chromseq.addAnnotation(annot);
            }
        }
        if (DEBUG) {
            Logger.getLogger(BrsParser.class.getName()).log(
                    Level.FINE, "transcript count = {0}", count);
            Logger.getLogger(BrsParser.class.getName()).log(
                    Level.FINE, "exon count = {0}", total_exon_count);
            if (count > 0) {
                Logger.getLogger(BrsParser.class.getName()).log(
                        Level.FINE, "average exons / transcript = {0}", ((double) total_exon_count / (double) count));
            }
        }
        return results;
    }

    private void outputBrsFormat(UcscGeneSym gsym, DataOutputStream dos) throws IOException {
        SeqSpan tspan = gsym.getSpan(0);
        SeqSpan cspan = gsym.getCdsSpan();
        BioSeq seq = tspan.getBioSeq();
        dos.writeUTF(gsym.getGeneName());
        dos.writeUTF(gsym.getName());
        dos.writeUTF(seq.getId());
        if (tspan.isForward()) {
            dos.writeUTF("+");
        } else {
            dos.writeUTF("-");
        }
        dos.writeInt(tspan.getMin());
        dos.writeInt(tspan.getMax());
        dos.writeInt(cspan.getMin());
        dos.writeInt(cspan.getMax());
        dos.writeInt(gsym.getChildCount());
        int childcount = gsym.getChildCount();
        for (int k = 0; k < childcount; k++) {
            SeqSpan child = gsym.getChild(k).getSpan(seq);
            dos.writeInt(child.getMin());
        }
        for (int k = 0; k < childcount; k++) {
            SeqSpan child = gsym.getChild(k).getSpan(seq);
            dos.writeInt(child.getMax());
        }
    }

    private void convertTextToBinary(String file_name, String bin_file) {
        Logger.getLogger(BrsParser.class.getName()).log(
                Level.FINE, "loading file: {0}", file_name);
        int count = 0;
        long flength = 0;
        int max_tlength = Integer.MIN_VALUE;
        int max_exons = Integer.MIN_VALUE;
        int total_exon_count = 0;
        int big_spliced = 0;

        DataOutputStream dos = null;
        FileInputStream fis = null;
        BufferedReader br = null;
        BufferedInputStream bis = null;
        try {
            File fil = new File(file_name);
            flength = fil.length();
            fis = new FileInputStream(fil);
            bis = new BufferedInputStream(fis);

            byte[] bytebuf = new byte[(int) flength];
            bis.read(bytebuf);
            ByteArrayInputStream bytestream = new ByteArrayInputStream(bytebuf);
            br = new BufferedReader(new InputStreamReader(bytestream));
            String line;

            File outfile = new File(bin_file);
            FileOutputStream fos = new FileOutputStream(outfile);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            dos = new DataOutputStream(bos);

            // trying to handle both refFlat and refGene files
            //   if refGene, then file doesn't include geneName field (and can be detected because
            //   it has one fewer tab-delimited field)
            boolean text_includes_genename;
            while ((line = br.readLine()) != null) {
                count++;
                int field_index = 0;
                String[] fields = line_regex.split(line);
                text_includes_genename = (fields.length > 10);
                String geneName = null;
                if (text_includes_genename) {
                    geneName = fields[field_index++];
                }
                String name = fields[field_index++];
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
                int ecount = Integer.parseInt(exonCount);
                String[] emins = emin_regex.split(exonStarts);
                String[] emaxs = emax_regex.split(exonEnds);

                if (!text_includes_genename) {
                    geneName = name;
                }
                dos.writeUTF(geneName);
                dos.writeUTF(name);
                dos.writeUTF(chrom);
                dos.writeUTF(strand);
                dos.writeInt(tmin);
                dos.writeInt(tmax);
                dos.writeInt(cmin);
                dos.writeInt(cmax);
                dos.writeInt(ecount);

                if (ecount != emins.length || ecount != emaxs.length) {
                    System.out.println("EXON COUNTS DON'T MATCH UP FOR " + geneName + " !!!");
                } else {
                    for (int i = 0; i < ecount; i++) {
                        int emin = Integer.parseInt(emins[i]);
                        dos.writeInt(emin);
                    }
                    for (int i = 0; i < ecount; i++) {
                        int emax = Integer.parseInt(emaxs[i]);
                        dos.writeInt(emax);
                    }
                }
                if (tlength >= 500000) {
                }

                total_exon_count += ecount;
                max_exons = Math.max(max_exons, ecount);
                max_tlength = Math.max(max_tlength, tlength);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            GeneralUtils.safeClose(br);
            GeneralUtils.safeClose(bis);
            GeneralUtils.safeClose(fis);
            GeneralUtils.safeClose(dos);
        }

        System.out.println("line count = " + count);
        System.out.println("file length = " + flength);
        System.out.println("max genomic transcript length: " + max_tlength);
        System.out.println("max exons in single transcript: " + max_exons);
        System.out.println("total exons: " + total_exon_count);
        System.out.println("spliced transcripts > 65000: " + big_spliced);
    }

    /**
     * Reads a text file and writes a binary file.
     * <p>
     * Typical Command-line Usage to convert from RefFlat text files to brs files
     * <code>java -classpath genometry.jar:genometry.jar com.affymetrix.genometry.parsers.BrsParser
     *    refFlat.txt refseq.brs</code>
     * <p>
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
     * Implementing AnnotationWriter interface to write out annotations
     * to an output stream as "binary UCSC refseq gene". File extension ".brs".
     *
     */
    public boolean writeAnnotations(Collection<? extends SeqSymmetry> syms, BioSeq seq,
            String type, OutputStream outstream) {
        System.out.println("in BrsParser.writeAnnotations()");
        boolean success = true;
        try {
            DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(outstream));
            for (SeqSymmetry sym : syms) {
                if (!(sym instanceof UcscGeneSym)) {
                    System.err.println("trying to output non-UcscGeneSym as UcscGeneSym!");
                }
                outputBrsFormat((UcscGeneSym) sym, dos);
            }
            dos.flush();
        } catch (Exception ex) {
            ex.printStackTrace();
            success = false;
        }
        return success;
    }

    public void writeSymmetry(SeqSymmetry sym, BioSeq seq, OutputStream os) throws IOException {
        DataOutputStream dos = null;
        if (os instanceof DataOutputStream) {
            dos = (DataOutputStream) os;
        } else {
            dos = new DataOutputStream(os);
        }
        outputBrsFormat((UcscGeneSym) sym, dos);
    }

    public List<SeqSymmetry> parse(DataInputStream dis, String annot_type, GenomeVersion genomeVersion) {
        try {
            return BrsParser.parse(dis, annot_type, genomeVersion, false);
        } catch (IOException ex) {
            Logger.getLogger(BrsParser.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    public Comparator<SeqSymmetry> getComparator(BioSeq seq) {
        return new SeqSymMinComparator(seq);
    }

    public int getMin(SeqSymmetry sym, BioSeq seq) {
        SeqSpan span = sym.getSpan(seq);
        return span.getMin();
    }

    public int getMax(SeqSymmetry sym, BioSeq seq) {
        SeqSpan span = sym.getSpan(seq);
        return span.getMax();
    }

    public List<String> getFormatPrefList() {
        return BrsParser.pref_list;
    }

    /**
     * Implementing AnnotationWriter interface to write out annotations
     * to an output stream as "binary UCSC refseq gene"
     *
     */
    public String getMimeType() {
        return "binary/brs";
    }

    @Override
    public List<? extends SeqSymmetry> parse(InputStream is,
            GenomeVersion genomeVersion, String nameType, String uri, boolean annotate_seq)
            throws Exception {
        return parse(is, uri, genomeVersion, annotate_seq);
    }
}
