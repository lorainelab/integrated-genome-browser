package com.affymetrix.genometryImpl.operator.extra;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.comparator.SeqSymMinComparator;
import com.affymetrix.genometryImpl.operator.AbstractAnnotationTransformer;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SimpleScoredSymWithProps;
import com.affymetrix.genometryImpl.symmetry.impl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.util.SeqUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author hiralv
 */
public class ParentSummaryOperator extends AbstractAnnotationTransformer implements Operator {

    public ParentSummaryOperator(FileTypeCategory fileTypeCategory) {
        super(fileTypeCategory);
    }

    @Override
    public String getName() {
        return fileTypeCategory.toString().toLowerCase() + "_parent_summary";
    }

    @Override
    public String getDisplay() {
        return ParentOperatorConstants.BUNDLE.getString("operator_" + getName());
    }

    @Override
    public SeqSymmetry operate(BioSeq aseq, List<SeqSymmetry> symList) {
        if (symList.isEmpty()) {
            return new SimpleSymWithProps();
        }

        SeqSymmetry topSym = symList.get(0);
        List<SeqSymmetry> syms = new ArrayList<>();
        for (int i = 0; i < topSym.getChildCount(); i++) {
            syms.add(topSym.getChild(i));
        }

        Collections.sort(syms, new SeqSymMinComparator(aseq));

        SimpleSymWithProps result = new SimpleScoredSymWithProps(0);
        List<SeqSymmetry> temp = new ArrayList<>();
        double lastMax = syms.get(0).getSpan(aseq).getMax();

        for (SeqSymmetry sym : syms) {
            SeqSpan currentSpan = sym.getSpan(aseq);

            if (currentSpan.getMin() > lastMax) {
                MutableSeqSymmetry resultSym = new SimpleScoredSymWithProps(temp.size());
                SeqUtils.union(temp, resultSym, aseq, 2);
                result.addChild(resultSym);

                lastMax = Integer.MIN_VALUE;
                temp.clear();
            }

            temp.add(sym);
            lastMax = Math.max(lastMax, currentSpan.getMax());

        }

        //Remaining
        MutableSeqSymmetry resultSym = new SimpleScoredSymWithProps(temp.size());
        SeqUtils.union(temp, resultSym, aseq, 2);
        result.addChild(resultSym);
        temp.clear();

        syms.clear();

        return result;
    }
}
