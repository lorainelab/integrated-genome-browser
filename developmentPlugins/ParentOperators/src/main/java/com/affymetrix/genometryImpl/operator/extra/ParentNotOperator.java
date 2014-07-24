package com.affymetrix.genometryImpl.operator.extra;

import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.operator.AbstractAnnotationTransformer;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymSummarizer;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.symmetry.SingletonSeqSymmetry;

/**
 *
 * @author hiralv
 */
public class ParentNotOperator extends AbstractAnnotationTransformer implements Operator {

    public ParentNotOperator(FileTypeCategory fileTypeCategory) {
        super(fileTypeCategory);
    }

    @Override
    public String getName() {
        return fileTypeCategory.toString().toLowerCase() + "_parent_not";
    }

    @Override
    public String getDisplay() {
        return ParentOperatorConstants.BUNDLE.getString("operator_" + getName());
    }

    @Override
    public SeqSymmetry operate(BioSeq seq, List<SeqSymmetry> symList) {
        return getNot(symList, seq, true);
    }

    private static SeqSymmetry getNot(List<SeqSymmetry> syms, BioSeq seq, boolean include_ends) {
        SeqSymmetry union = SeqSymSummarizer.getUnion(syms, seq, 2);
        if (union == null) {
            return null;
        }
        int spanCount = union.getChildCount();

        // rest of this is pretty much pulled directly from SeqUtils.inverse()
        if (!include_ends) {
            if (spanCount <= 1) {
                return null;
            }  // no gaps, no resulting inversion
        }
        MutableSeqSymmetry invertedSym = new SimpleSymWithProps();
        if (include_ends) {
            if (spanCount < 1) {
                // no spans, so just return sym of whole range of seq
                invertedSym.addSpan(new SimpleSeqSpan(0, seq.getLength(), seq));
                return invertedSym;
            } else {
                SeqSpan firstSpan = union.getChild(0).getSpan(seq);
                if (firstSpan.getMin() > 0) {
                    SeqSymmetry beforeSym = new SingletonSeqSymmetry(0, firstSpan.getMin(), seq);
                    invertedSym.addChild(beforeSym);
                }
            }
        }
        for (int i = 0; i < spanCount - 1; i++) {
            SeqSpan preSpan = union.getChild(i).getSpan(seq);
            SeqSpan postSpan = union.getChild(i + 1).getSpan(seq);
            SeqSymmetry gapSym
                    = new SingletonSeqSymmetry(preSpan.getMax(), postSpan.getMin(), seq);
            invertedSym.addChild(gapSym);
        }
        if (include_ends) {
            SeqSpan lastSpan = union.getChild(spanCount - 1).getSpan(seq);
            if (lastSpan.getMax() < seq.getLength()) {
                SeqSymmetry afterSym = new SingletonSeqSymmetry(lastSpan.getMax(), seq.getLength(), seq);
                invertedSym.addChild(afterSym);
            }
        }
        if (include_ends) {
            invertedSym.addSpan(new SimpleSeqSpan(0, seq.getLength(), seq));
        } else {
            int min = union.getChild(0).getSpan(seq).getMax();
            int max = union.getChild(spanCount - 1).getSpan(seq).getMin();
            invertedSym.addSpan(new SimpleSeqSpan(min, max, seq));
        }
        return invertedSym;
    }
}
