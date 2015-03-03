package com.affymetrix.genometry.operator;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.comparator.SeqSymMinComparator;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.SimpleScoredSymWithProps;
import com.affymetrix.genometry.symmetry.impl.SimpleSymWithProps;
import com.affymetrix.genometry.util.SeqUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author hiralv
 */
public class SummaryOperator extends AbstractAnnotationTransformer implements Operator {

    public SummaryOperator(FileTypeCategory fileTypeCategory) {
        super(fileTypeCategory);
    }

    @Override
    public String getName() {
        return fileTypeCategory.toString().toLowerCase() + "_summary";
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
                SeqUtils.union(temp, resultSym, aseq);
                result.addChild(resultSym);

                lastMax = Integer.MIN_VALUE;
                temp.clear();
            }

            temp.add(sym);
            lastMax = Math.max(lastMax, currentSpan.getMax());

        }

        //Remaining
        MutableSeqSymmetry resultSym = new SimpleScoredSymWithProps(temp.size());
        SeqUtils.union(temp, resultSym, aseq);
        result.addChild(resultSym);
        temp.clear();

        syms.clear();

        return result;
    }

}
