/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometry.util;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleMutableSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SingletonSymWithProps;
import com.affymetrix.genometry.symmetry.TypedSym;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author dcnorris
 */
public class BioSeqUtils {

    /**
     * Finds the "method" for a SeqSymmetry. Looks for the "method" in four
     * places, in order: (1) the property "method", (2) the property "meth", (3)
     * the property "type", (4) TypedSym.getType(). If no method is found,
     * returns null.
     *
     * @param sym
     * @return
     */
    public static String determineMethod(SeqSymmetry sym) {
        String meth = null;
        if (sym instanceof SymWithProps) {
            SymWithProps psym = (SymWithProps) sym;
            meth = (String) psym.getProperty("method");
            if (meth == null) {
                meth = (String) psym.getProperty("meth");
            }
            if (meth == null) {
                meth = (String) psym.getProperty("type");
            }
        }
        if (meth == null) {
            if (sym instanceof TypedSym) {
                meth = ((TypedSym) sym).getType();
            }
        }
        return meth;
    }

    public static List<SingletonSymWithProps> searchForRegexInResidues(boolean forward, Pattern regex, String residues, int residue_offset, BioSeq seq) {
        List<SingletonSymWithProps> results = new ArrayList<>();

        Matcher matcher = regex.matcher(residues);
        while (matcher.find() && !Thread.currentThread().isInterrupted()) {
            int residue_start = residue_offset + (forward ? matcher.start(0) : -matcher.end(0));
            int residue_end = residue_offset + (forward ? matcher.end(0) : -matcher.start(0));
            //int end = matcher.end(0) + residue_offset;
            SingletonSymWithProps info = forward ? new SingletonSymWithProps(residue_start, residue_end, seq)
                    : new SingletonSymWithProps(residue_end, residue_start, seq);
            info.setProperty("method", "Search term:" + regex.pattern());
            info.setProperty("direction", forward ? "forward" : "reverse");
            info.setProperty("match", matcher.group(0));
            info.setProperty("pattern", regex.pattern());

            results.add(info);
        }
        return results;
    }

    /**
     * Add residues to composition (full sequence loaded).
     *
     * @param aseq
     */
    public static void addResiduesToComposition(BioSeq aseq) {
        if (aseq.getResiduesProvider() != null) {
            SeqSpan span = new SimpleSeqSpan(0, aseq.getResiduesProvider().getLength(), aseq);
            BioSeq subseq = new BioSeq(
                    aseq.getID() + ":" + span.getMin() + "-" + span.getMax(), aseq.getResiduesProvider().getLength());
            subseq.setResiduesProvider(aseq.getResiduesProvider());
            addSubseqToComposition(aseq, span, subseq);
            return;
        }
        String residues = aseq.getResidues();
        SeqSpan span = new SimpleSeqSpan(0, residues.length(), aseq);
        addResiduesToComposition(aseq, residues, span);
    }

    /**
     * Adds the residues to the composite sequence. This allows merging of
     * subsequences.
     *
     * @param aseq
     * @param residues
     * @param span
     */
    public static void addResiduesToComposition(BioSeq aseq, String residues, SeqSpan span) {
        BioSeq subseq = new BioSeq(
                aseq.getID() + ":" + span.getMin() + "-" + span.getMax(), residues.length());
        subseq.setResidues(residues);
        addSubseqToComposition(aseq, span, subseq);
    }

    private static void addSubseqToComposition(BioSeq aseq, SeqSpan span, BioSeq subSeq) {
        SeqSpan subSpan = new SimpleSeqSpan(0, span.getLength(), subSeq);
        SeqSpan mainSpan = span;
        MutableSeqSymmetry subsym = new SimpleMutableSeqSymmetry();
        subsym.addSpan(subSpan);
        subsym.addSpan(mainSpan);
        MutableSeqSymmetry compsym = (MutableSeqSymmetry) aseq.getComposition();
        if (compsym == null) {
            //No children.  Add one.
            compsym = new SimpleMutableSeqSymmetry();
            compsym.addChild(subsym);
            compsym.addSpan(new SimpleSeqSpan(mainSpan.getMin(), mainSpan.getMax(), aseq));
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
