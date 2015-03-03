package com.affymetrix.genometry.operator;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.parsers.FileTypeCategory;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.TypeContainerAnnot;
import java.util.HashMap;
import java.util.List;

public class CopyXOperator extends AbstractAnnotationTransformer implements Operator, ICopy {

    public CopyXOperator(FileTypeCategory category) {
        super(category);
    }

    @Override
    public String getName() {
        return fileTypeCategory.toString().toLowerCase() + "_copy";
    }

    @Override
    public SeqSymmetry operate(BioSeq aseq, List<SeqSymmetry> symList) {
        if (symList.size() != 1 || !(symList.get(0) instanceof TypeContainerAnnot)) {
            return null;
        }
        TypeContainerAnnot result = null;
        TypeContainerAnnot t = (TypeContainerAnnot) symList.get(0);
        result = new TypeContainerAnnot(t.getType());
        // copy children
        for (int i = 0; i < t.getChildCount(); i++) {
            result.addChild(t.getChild(i));
        }
        // copy spans
        for (int i = 0; i < t.getSpanCount(); i++) {
            result.addSpan(t.getSpan(i));
        }
        // copy properties
        result.setProperties(new HashMap<>(t.getProperties()));
        return result;
    }

    @Override
    public FileTypeCategory getOutputCategory() {
        return this.fileTypeCategory;
    }
}
