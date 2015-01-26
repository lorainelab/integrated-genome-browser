package com.affymetrix.genometry.operator.extra;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.operator.AbstractAnnotationOperator;
import com.affymetrix.genometry.operator.Operator;
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

public class ParentIntersectionOperator extends AbstractAnnotationOperator implements Operator {

    public ParentIntersectionOperator(FileTypeCategory fileTypeCategory) {
        super(fileTypeCategory);
    }

    @Override
    public String getName() {
        return category.toString().toLowerCase() + "_parent_intersection";
    }

    @Override
    public String getDisplay() {
        return ParentOperatorConstants.BUNDLE.getString("operator_" + getName());
    }

    @Override
    public SeqSymmetry operate(BioSeq aseq, List<SeqSymmetry> symList) {
        SeqSymmetry unionA = SeqSymSummarizer.getUnion(findChildSyms(symList.get(0)), aseq, 2);
        SeqSymmetry unionB = SeqSymSummarizer.getUnion(findChildSyms(symList.get(1)), aseq, 2);
        return intersect(aseq, unionA, unionB);
    }

    private static SeqSymmetry intersect(BioSeq seq, SeqSymmetry unionA, SeqSymmetry unionB) {
        MutableSeqSymmetry psym = new SimpleSymWithProps();
        List<SeqSymmetry> symsAB = new ArrayList<>();
        symsAB.add(unionA);
        symsAB.add(unionB);
        GraphSym combo_graph = SeqSymSummarizer.getSymmetrySummary(symsAB, seq, false, "");
		// combo_graph should now be landscape where:
        //    no coverage ==> depth = 0;
        //    A not B     ==> depth = 1;
        //    B not A     ==> depth = 1;
        //    A && B      ==> depth = 2;

        // so any regions with depth == 2 are intersection
        int num_points = combo_graph.getPointCount();

        int current_region_start = 0;
        int current_region_end = 0;
        boolean in_region = false;
        for (int i = 0; i < num_points; i++) {
            int xpos = combo_graph.getGraphXCoord(i);
            float ypos = combo_graph.getGraphYCoord(i);
            if (in_region) {
                if (ypos < 2) { // reached end of intersection region, make SeqSpan
                    in_region = false;
                    current_region_end = xpos;
                    SeqSymmetry newsym
                            = new SingletonSeqSymmetry(current_region_start, current_region_end, seq);
                    psym.addChild(newsym);
                }
            } else {  // not already in_region
                if (ypos >= 2) {
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
