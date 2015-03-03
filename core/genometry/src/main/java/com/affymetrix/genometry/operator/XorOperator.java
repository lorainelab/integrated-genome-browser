package com.affymetrix.genometry.operator;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometry.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.GraphSym;
import com.affymetrix.genometry.symmetry.impl.SeqSymSummarizer;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleSymWithProps;
import com.affymetrix.genometry.symmetry.impl.SingletonSeqSymmetry;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author lfrohman
 */
public class XorOperator extends AbstractAnnotationOperator implements Operator {

    public XorOperator(FileTypeCategory fileTypeCategory) {
        super(fileTypeCategory);
    }

    @Override
    public String getName() {
        return category.toString().toLowerCase() + "_xor";
    }

    @Override
    public SeqSymmetry operate(BioSeq aseq, java.util.List<SeqSymmetry> symList) {
        return getXor(aseq, findChildSyms(symList.get(0)), findChildSyms(symList.get(1)));
    }

    protected static SeqSymmetry getXor(BioSeq seq, List<SeqSymmetry> symsA, List<SeqSymmetry> symsB) {
        MutableSeqSymmetry psym = new SimpleSymWithProps();
        SeqSymmetry unionA = SeqSymSummarizer.getUnion(symsA, seq);
        SeqSymmetry unionB = SeqSymSummarizer.getUnion(symsB, seq);
        List<SeqSymmetry> symsAB = new ArrayList<>();
        symsAB.add(unionA);
        symsAB.add(unionB);
        GraphSym combo_graph = SeqSymSummarizer.getSymmetrySummary(symsAB, seq, false, "");
		// combo_graph should now be landscape where:
        //    no coverage ==> depth = 0;
        //    A not B     ==> depth = 1;
        //    B not A     ==> depth = 1;
        //    A && B      ==> depth = 2;

        // so any regions with depth == 1 are XOR regions
        int num_points = combo_graph.getPointCount();

        int current_region_start = 0;
        int current_region_end = 0;
        boolean in_region = false;
        for (int i = 0; i < num_points; i++) {
            int xpos = combo_graph.getGraphXCoord(i);
            float ypos = combo_graph.getGraphYCoord(i);
            if (in_region) {
                if (ypos < 1 || ypos > 1) { // reached end of xor region, make SeqSpan
                    in_region = false;
                    current_region_end = xpos;
                    SeqSymmetry newsym
                            = new SingletonSeqSymmetry(current_region_start, current_region_end, seq);
                    psym.addChild(newsym);
                }
            } else {  // not already in_region
                if (ypos == 1) {
                    in_region = true;
                    current_region_start = xpos;
                }
            }
        }
        if (in_region) {  // last point was still in_region, so make a span to end?
            // pretty sure this won't happen, based on how getSymmetrySummary()/getSpanSummary() work
            System.err.println("still in a covered region at end of getUnion() loop!");
        }

        if (psym.getChildCount() <= 0) {
            psym = null;
        } else {
            // landscape is already sorted, so should be able to derive parent min and max
            int pmin = psym.getChild(0).getSpan(seq).getMin();
            int pmax = psym.getChild(psym.getChildCount() - 1).getSpan(seq).getMax();
            SeqSpan pspan = new SimpleSeqSpan(pmin, pmax, seq);
            psym.addSpan(pspan);
        }
        return psym;
    }

}
