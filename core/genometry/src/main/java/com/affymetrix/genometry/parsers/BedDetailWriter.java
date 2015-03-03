package com.affymetrix.genometry.parsers;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.Scored;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.UcscBedDetailSym;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;

/**
 *
 * @author hiralv
 */
public class BedDetailWriter extends BedParser implements AnnotationWriter {

    private static final boolean DEBUG = false;

    @Override
    public boolean writeAnnotations(Collection<? extends SeqSymmetry> syms, BioSeq seq,
            String type, OutputStream outstream) {
        if (DEBUG) {
            System.out.println("in BedParser.writeAnnotations()");
        }
        DataOutputStream dos = null;
        try {
            dos = new DataOutputStream(new BufferedOutputStream(outstream));
            for (SeqSymmetry sym : syms) {
                writeSymmetry(dos, sym, seq);
            }
            dos.flush();
            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public static void writeSymmetry(DataOutputStream out, SeqSymmetry sym, BioSeq seq)
            throws IOException {
        if (DEBUG) {
            System.out.println("writing sym: " + sym);
        }
        SeqSpan span = sym.getSpan(seq);
        if (span == null) {
            return;
        }

        if (sym instanceof UcscBedDetailSym) {
            UcscBedDetailSym bedsym = (UcscBedDetailSym) sym;
            if (seq == bedsym.getBioSeq()) {
                bedsym.outputBedDetailFormat(out);
                out.write('\n');
            }
            return;
        }

        SymWithProps propsym = null;
        if (sym instanceof SymWithProps) {
            propsym = (SymWithProps) sym;
        }

        writeOutFile(out, seq, span, sym, propsym);
    }

    private static void writeOutFile(DataOutputStream out, BioSeq seq, SeqSpan span, SeqSymmetry sym, SymWithProps propsym) throws IOException {
        out.write(seq.getID().getBytes());
        out.write('\t');
        int min = span.getMin();
        int max = span.getMax();
        out.write(Integer.toString(min).getBytes());
        out.write('\t');
        out.write(Integer.toString(max).getBytes());
        int childcount = sym.getChildCount();
        if ((!span.isForward()) || (childcount > 0) || (propsym != null)) {
            out.write('\t');
            if (propsym != null) {
                if (propsym.getProperty("name") != null) {
                    out.write(((String) propsym.getProperty("name")).getBytes());
                } else if (propsym.getProperty("id") != null) {
                    out.write(((String) propsym.getProperty("id")).getBytes());
                } else {
                    out.write((seq.getID() + ":" + Integer.toString(min) + "-" + Integer.toString(max) + ":" + (span.isForward() ? "+" : "-")).getBytes());
                }
            }
            out.write('\t');
            if ((propsym != null) && (propsym.getProperty("score") != null)) {

                Float score = (Float) propsym.getProperty("score");
                if (Float.compare(score, Scored.UNKNOWN_SCORE) == 0) {
                    out.write('.');
                }
                if (score == Math.round(score)) {
                    out.write(Integer.toString(score.intValue()).getBytes());
                } else {
                    out.write(score.toString().getBytes());
                }

            } else if (sym instanceof Scored) {

                Float score = ((Scored) sym).getScore();
                if (Float.compare(score, Scored.UNKNOWN_SCORE) == 0) {
                    out.write('.');
                } else if (score == Math.round(score)) {
                    out.write(Integer.toString(score.intValue()).getBytes());
                } else {
                    out.write(Float.toString(score).getBytes());
                }

            } else {
                out.write('0');
            }
            out.write('\t');
            if (span.isForward()) {
                out.write('+');
            } else {
                out.write('-');
            }

            writeOutChildren(out, propsym, min, max, childcount, sym, seq);

            out.write('\t');

            if ((propsym != null) && (propsym.getProperty("gene name") != null)) {
                out.write(propsym.getProperty("gene name").toString().getBytes());
            } else {
                out.write("N/A".getBytes());
            }

            out.write('\t');

            if ((propsym != null) && (propsym.getProperty("description") != null)) {
                out.write(propsym.getProperty("description").toString().getBytes());
            } else {
                out.write("N/A".getBytes());
            }

        }
        out.write('\n');
    }

    @Override
    public String getMimeType() {
        return "text/bed";
    }
}
