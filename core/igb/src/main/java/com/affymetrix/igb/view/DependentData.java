package com.affymetrix.igb.view;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import com.affymetrix.genometry.symmetry.impl.MisMatchGraphSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymSummarizer;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleSymWithProps;
import com.affymetrix.igb.view.load.GeneralLoadView;
import com.lorainelab.igb.genoviz.extensions.StyledGlyph;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author hiralv
 */
public class DependentData {

    static public enum DependentType {

        SUMMARY,
        COVERAGE,
        MISMATCH,
        MISMATCH_PILEUP
    }

    final private String id;
    final private String parent_method;
    final private DependentType type;
    private StyledGlyph.Direction direction;
    private SymWithProps sym;

    public DependentData(String id, DependentType type, String parentUrl) {
        this.id = id;
        this.parent_method = parentUrl;
        this.type = type;
    }

    public DependentData(String id, DependentType type, String parentUrl, StyledGlyph.Direction direction) {
        this(id, type, parentUrl);
        this.direction = direction;
    }

    public SymWithProps createTier(BioSeq aseq) {

        SeqSymmetry psym = aseq.getAnnotation(parent_method);
        if (psym == null) {
            return null;
        }

        if (type == DependentType.MISMATCH) {
            sym = createMisMatchGraph(aseq, psym, false);
        } else if (type == DependentType.MISMATCH_PILEUP) {
            sym = createMisMatchGraph(aseq, psym, true);
        } else if (type == DependentType.SUMMARY) { //Check if type is summary.
            sym = createSummaryGraph(aseq, psym);
        } else {	//If type is not summary then it should be coverage.
            sym = createCoverageTier(aseq, psym);
        }

        return sym;
    }

    private GraphSym createMisMatchGraph(BioSeq aseq, SeqSymmetry tsym, boolean pileup) {
        List<SeqSymmetry> syms = new ArrayList<>();
        syms.add(tsym);

        int[] startEnd = getStartEnd(tsym, aseq);
        SeqSpan loadSpan = new SimpleSeqSpan(startEnd[0], startEnd[1], aseq);

        //Load Residues
        if (!aseq.isAvailable(loadSpan)) {
            if (!GeneralLoadView.getLoadView().loadResidues(loadSpan, true)) {
                return (GraphSym) sym;
            }
        }

        MisMatchGraphSym mgsym = SeqSymSummarizer.getMismatchGraph(syms, aseq, false, id, startEnd[0], startEnd[1], pileup);

        return mgsym;
    }

    public static int[] getStartEnd(SeqSymmetry tsym, BioSeq aseq) {
        int start = Integer.MAX_VALUE, end = Integer.MIN_VALUE;

        for (int i = 0; i < tsym.getChildCount(); i++) {
            SeqSymmetry childSym = tsym.getChild(i);
            SeqSpan span = childSym.getSpan(aseq);
            if (span.getMax() > end) {
                end = span.getMax();
            }

            if (span.getMin() < start) {
                start = span.getMin();
            }
        }

        return new int[]{start, end};
    }

    private GraphSym createSummaryGraph(BioSeq aseq, SeqSymmetry sym) {
        List<SeqSymmetry> syms = new ArrayList<>();
        syms.add(sym);
        GraphSym gsym = null;
        if (direction == StyledGlyph.Direction.FORWARD || direction == StyledGlyph.Direction.REVERSE) {
            Boolean isForward = direction == StyledGlyph.Direction.FORWARD;
            gsym = SeqSymSummarizer.getSymmetrySummary(syms, aseq, false, id, isForward);
        } else {
            gsym = SeqSymSummarizer.getSymmetrySummary(syms, aseq, false, id);
        }

        gsym.setID(id);

        return gsym;
    }

    private SymWithProps createCoverageTier(BioSeq aseq, SeqSymmetry sym) {
        List<SeqSymmetry> syms = new ArrayList<>();
        syms.add(sym);
        SeqSymmetry union_sym = SeqSymSummarizer.getUnion(syms, aseq);
        SymWithProps wrapperSym;
        if (union_sym instanceof SymWithProps) {
            wrapperSym = (SymWithProps) union_sym;
        } else {
            wrapperSym = new SimpleSymWithProps();
            ((SimpleSymWithProps) wrapperSym).addChild(union_sym);
            for (int i = 0; i < union_sym.getSpanCount(); i++) {
                ((SimpleSymWithProps) wrapperSym).addSpan(union_sym.getSpan(i));
            }
        }
        wrapperSym.setProperty("method", id);
        wrapperSym.setProperty("id", id);

        return wrapperSym;
    }

    public String getParentMethod() {
        return parent_method;
    }

    public String getID() {
        return id;
    }

    public DependentType getType() {
        return type;
    }

    public StyledGlyph.Direction getDirection() {
        return direction;
    }

    public SymWithProps getSym() {
        return sym;
    }
}
