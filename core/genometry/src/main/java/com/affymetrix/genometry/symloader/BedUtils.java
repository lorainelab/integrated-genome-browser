package com.affymetrix.genometry.symloader;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.Scored;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.UcscBedDetailSym;
import com.affymetrix.genometry.symmetry.impl.UcscBedSym;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

/**
 *
 * @author dcnorris
 */
public class BedUtils {

    private static final Pattern COMMA_REGEX = Pattern.compile(",");

    /**
     * Converts the data in the score field, if present, to a floating-point
     * number.
     */
    public static float parseScore(String s) {
        if (s == null || s.length() == 0 || s.equals(".") || s.equals("-")) {
            return 0.0f;
        }
        return Float.parseFloat(s);
    }

    /**
     * Parses the name field from the file. Gene names are allowed to be
     * non-unique.
     *
     * @param s
     * @return annot_name
     */
    public static String parseName(String s) {
        String annot_name = s; // create a new String so the entire input line doesn't get preserved
        return annot_name;
    }

    public static int[] parseIntArray(String int_array) {
        if (int_array == null || int_array.length() == 0) {
            return new int[0];
        }
        String[] intstrings = COMMA_REGEX.split(int_array);
        int count = intstrings.length;
        int[] results = new int[count];
        for (int i = 0; i < count; i++) {
            int val = Integer.parseInt(intstrings[i]);
            results[i] = val;
        }
        return results;
    }

    /**
     * Converting blockStarts to blockMins.
     *
     * @param blockStarts in coords relative to min of annotation
     * @return blockMins in coords relative to sequence that annotation is "on"
     */
    public static int[] makeBlockMins(int min, int[] blockStarts) {
        int count = blockStarts.length;
        int[] blockMins = new int[count];
        for (int i = 0; i < count; i++) {
            blockMins[i] = blockStarts[i] + min;
        }
        return blockMins;
    }

    public static int[] makeBlockMaxs(int[] blockMins, int[] blockSizes) {
        int count = blockMins.length;
        int[] blockMaxs = new int[count];
        for (int i = 0; i < count; i++) {
            blockMaxs[i] = blockMins[i] + blockSizes[i];
        }
        return blockMaxs;
    }

    public static void writeBedFormat(DataOutputStream out, List<SeqSymmetry> syms, BioSeq seq) throws IOException {
        for (SeqSymmetry sym : syms) {
            writeSymmetry(out, sym, seq);
        }
    }

    /**
     * Writes bed file format. WARNING. This currently assumes that each child
     * symmetry contains a span on the seq given as an argument.
     */
    public static void writeSymmetry(DataOutputStream out, SeqSymmetry sym, BioSeq seq) throws IOException {
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

        if (sym instanceof UcscBedSym) {
            UcscBedSym bedsym = (UcscBedSym) sym;
            if (seq == bedsym.getBioSeq()) {
                bedsym.outputBedFormat(out);
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
                }
            }
            out.write('\t');
            if ((propsym != null) && (propsym.getProperty("score") != null)) {
                out.write(propsym.getProperty("score").toString().getBytes());
            } else if (sym instanceof Scored) {
                out.write(Float.toString(((Scored) sym).getScore()).getBytes());
            } else {
                out.write('0');
            }
            out.write('\t');
            if (span.isForward()) {
                out.write('+');
            } else {
                out.write('-');
            }
            if (childcount > 0) {
                writeOutChildren(out, propsym, min, max, childcount, sym, seq);
            }
        }
        out.write('\n');
    }

    public static void writeOutChildren(DataOutputStream out, SymWithProps propsym, int min, int max, int childcount, SeqSymmetry sym, BioSeq seq) throws IOException {
        out.write('\t');
        if ((propsym != null) && (propsym.getProperty("cds min") != null)) {
            out.write(propsym.getProperty("cds min").toString().getBytes());
        } else {
            out.write(Integer.toString(min).getBytes());
        }
        out.write('\t');
        if ((propsym != null) && (propsym.getProperty("cds max") != null)) {
            out.write(propsym.getProperty("cds max").toString().getBytes());
        } else {
            out.write(Integer.toString(max).getBytes());
        }
        out.write('\t');
        out.write('0');
        out.write('\t');
        out.write(Integer.toString(childcount).getBytes());
        out.write('\t');
        int[] blockSizes = new int[childcount];
        int[] blockStarts = new int[childcount];
        for (int i = 0; i < childcount; i++) {
            SeqSymmetry csym = sym.getChild(i);
            SeqSpan cspan = csym.getSpan(seq);
            blockSizes[i] = cspan.getLength();
            blockStarts[i] = cspan.getMin() - min;
        }
        for (int i = 0; i < childcount; i++) {
            out.write(Integer.toString(blockSizes[i]).getBytes());
            out.write(',');
        }
        out.write('\t');
        for (int i = 0; i < childcount; i++) {
            out.write(Integer.toString(blockStarts[i]).getBytes());
            out.write(',');
        }
    }

    public static boolean checkRange(int start, int end, int min, int max) {
        return !(end < min || start > max);
    }
}
