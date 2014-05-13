package com.gene.overlapannotationoperator;

import java.util.HashMap;
import java.util.List;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.operator.AbstractAnnotationOperator;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleSymWithProps;
import com.affymetrix.genometryImpl.symmetry.TypeContainerAnnot;

public class OverlapAnnotationOperator extends AbstractAnnotationOperator implements Operator {

    OverlapAnnotationOperator(FileTypeCategory category) {
        super(category);
    }

    @Override
    public String getName() {
        return this.category.toString().toLowerCase() + "_subset_by_overlaps";
    }

    @Override
    public String getDisplay() {
        return "Subset by overlaps";
    }

    private boolean overlap(BioSeq aseq, SeqSymmetry s0, SeqSymmetry s1) {
        return s0.getSpan(aseq) != null && s1.getSpan(aseq) != null && s0.getSpan(aseq).getMax() > s1.getSpan(aseq).getMin() && s0.getSpan(aseq).getMin() < s1.getSpan(aseq).getMax();
    }

    @Override
    public SeqSymmetry operate(BioSeq aseq, List<SeqSymmetry> symList) {
        SimpleSymWithProps result = new SimpleSymWithProps();
        result.setProperties(new HashMap<String, Object>());
        TypeContainerAnnot t0 = (TypeContainerAnnot) symList.get(0);
        TypeContainerAnnot t1 = (TypeContainerAnnot) symList.get(1);
        result.setProperty("type", t0.getType() + " " + getName());
        for (int i = 0; i < t0.getChildCount(); i++) {
            SeqSymmetry s0 = t0.getChild(i);
            for (int j = 0; j < t1.getChildCount(); j++) {
                SeqSymmetry s1 = t1.getChild(j);
                if (overlap(aseq, s0, s1)) {
                    result.addChild(s0);
                    result.addSpan(s0.getSpan(aseq));
                    break;
                }
            }
        }
//		if (result.getID() == null) {
//			result.setProperty("id", "");
//		}
        return result;
    }
}
