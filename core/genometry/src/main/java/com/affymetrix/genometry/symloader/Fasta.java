package com.affymetrix.genometry.symloader;

import com.affymetrix.genometry.AnnotatedSeqGroup;
import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.parsers.AnnotationWriter;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleSymWithResidues;
import com.affymetrix.genometry.util.GeneralUtils;
import com.affymetrix.genometry.util.LocalUrlCacher;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author jnicol
 */
public class Fasta extends FastaCommon implements AnnotationWriter {

    private static final Pattern header_regex
            = Pattern.compile("^\\s*>\\s*(.+)");
    private static final int COLUMNS = 50;

    public Fasta() {
        this(null, null, null);
    }

    public Fasta(URI uri, String featureName, AnnotatedSeqGroup group) {
        super(uri, "", group);
    }

    /**
     * Get seqids and lengths for all chromosomes.
     */
    protected boolean initChromosomes() throws Exception {
        BufferedInputStream bis = null;
        BufferedReader br = null;
        Matcher matcher = header_regex.matcher("");
        try {
            bis = LocalUrlCacher.convertURIToBufferedUnzippedStream(uri);
            br = new BufferedReader(new InputStreamReader(bis));
            String header = br.readLine();
            while (br.ready() && (!Thread.currentThread().isInterrupted())) {  // loop through lines till find a header line
                if (header == null) {
                    continue;
                }
                matcher.reset(header);

                if (!matcher.matches()) {
                    continue;
                }
                String seqid = matcher.group(1).split(" ")[0];	//get rid of spaces
                BioSeq seq = group.getSeq(seqid);
                int count = 0;
                header = null;	// reset for next header
                String line = null;
                char firstChar;
                while (br.ready() && (!Thread.currentThread().isInterrupted())) {
                    line = br.readLine();
                    if (line == null) {
                        break;
                    }
                    if (line.length() == 0) {
                        continue;
                    }  // skip null and empty lines

                    firstChar = line.charAt(0);
                    if (firstChar == ';') {
                        continue;
                    } // skip comment lines

                    // break if hit header for another sequence --
                    if (firstChar == '>') {
                        header = line;
                        break;
                    }
                    count += line.trim().length();
                }
                if (seq == null) {
                    seq = group.addSeq(seqid, count, uri.toString());
                }
                chrSet.add(seq);
            }

            return !Thread.currentThread().isInterrupted();
        } catch (Exception ex) {
            throw ex;
        } finally {
            GeneralUtils.safeClose(br);
            GeneralUtils.safeClose(bis);
        }

    }

    @Override
    public String getRegionResidues(SeqSpan span) throws Exception {
        init();
        BufferedInputStream bis = LocalUrlCacher.convertURIToBufferedUnzippedStream(uri);
		//Bigger buffers should increase multitasking.  That is, the thread tha manages the buffer
        //can keep loading the file from the web or hardrive while the main program can compute.
        final BufferedReader br = new BufferedReader(new InputStreamReader(bis), 1024 * 1024 * 50);
        try {
            Iterator<String> it = new Iterator<String>() {
                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public String next() {
                    String line;
                    try {
                        line = br.readLine();
                    } catch (IOException x) {
                        Logger.getLogger(this.getClass().getName()).log(
                                Level.SEVERE, "error reading fasta file", x);
                        line = null;
                    }
                    return line;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };

            return parse(it, span.getBioSeq(), span.getMin(), span.getMax());

        } catch (Exception ex) {
            throw ex;
        } finally {
            GeneralUtils.safeClose(br);
            GeneralUtils.safeClose(bis);
        }
    }

    public String parse(Iterator<String> it, BioSeq spanSeq, int min, int max) throws Exception {
        int count = 0;
        String residues = "";
        Matcher matcher = header_regex.matcher("");
        String header = it.next();
        while (it.hasNext() && (!Thread.currentThread().isInterrupted())) {  // loop through lines till find a header line
            if (header == null) {
                break;
            }
            matcher.reset(header);

            if (!matcher.matches()) {
                continue;
            }
            String seqid = matcher.group(1).split(" ")[0];	// get rid of spaces
            BioSeq seq = group.getSeq(seqid);
            //equals ?
            boolean seqMatch = (seq != null && seq.equals(spanSeq));
            header = null;	// reset for next header

            StringBuffer buf = new StringBuffer();
            String line = null;
            char firstChar;
            while (it.hasNext() && (!Thread.currentThread().isInterrupted())) {
                line = it.next();
                if (line == null) {
                    break;
                }

                if (line.length() == 0) {
                    continue;
                }  // skip null and empty lines

                firstChar = line.charAt(0);
                if (firstChar == ';') {
                    continue;
                } // skip comment lines

                // break if hit header for another sequence --
                if (firstChar == '>') {
                    header = line;
                    break;
                }
                if (seqMatch) {
                    line = line.trim();
                    if (count + line.length() <= min) {
                        // skip lines
                        count += line.length();
                        continue;
                    }
                    if (count > max) {
                        break; // should never happen
                    }
                    if (count < min) {
                        // skip beginning characters
                        line = line.substring(min - count);
                        count = min;
                    }
                    if (count + line.length() >= max) {
                        //Special case when the whole line is read in one go
                        int toBeAdded = count + line.length() - max;
                        if (count + toBeAdded < max && line.length() >= max) {
                            line = line.substring(min, max);
                            count += line.length();
                        } else {
                            // skip ending characters
                            line = line.substring(0, count + line.length() - max);
                        }
                    }
                    buf.append(line);
                }
            }

			// Didn't use .toString() here because of a memory bug in Java
            // (See "stringbuffer memory java" for more details.)
            residues = new String(buf);
            buf.setLength(0);
            buf = null; // immediately allow the gc to use this memory
            residues = residues.trim();
            if (seqMatch) {
                break;
            }
        }

        if (Thread.currentThread().isInterrupted()) {
            return null;
        }

        return residues;
    }

    // only one sym in sym array
    @Override
    public boolean writeAnnotations(Collection<? extends SeqSymmetry> syms,
            BioSeq seq, String type, OutputStream outstream) throws IOException {
        if (syms == null || syms.size() != 1) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "bad symList in FastaAnnotationWriter");
            return false;
        }
        SeqSymmetry sym = syms.iterator().next();
        SimpleSymWithResidues residuesSym = (SimpleSymWithResidues) sym.getChild(0);
        String residues = residuesSym.getResidues();
        outstream.write('>');
        outstream.write(seq.toString().getBytes());
        outstream.write('\n');
        int pointer = 0;
        while (pointer < residues.length()) {
            int end = Math.min(pointer + COLUMNS, residues.length());
            outstream.write(residues.substring(pointer, end).getBytes());
            outstream.write('\n');
            pointer += COLUMNS;
        }
        return true;
    }

    @Override
    public String getMimeType() {
        return "text/fasta";
    }
}
