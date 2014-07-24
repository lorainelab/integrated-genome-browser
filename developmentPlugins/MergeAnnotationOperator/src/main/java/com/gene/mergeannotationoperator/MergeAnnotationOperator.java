package com.gene.mergeannotationoperator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.operator.AbstractAnnotationOperator;
import com.affymetrix.genometryImpl.operator.Operator;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.impl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.symmetry.impl.TypeContainerAnnot;

public class MergeAnnotationOperator extends AbstractAnnotationOperator implements Operator {

    MergeAnnotationOperator(FileTypeCategory category) {
        super(category);
    }

    @Override
    public String getName() {
        return category.toString().toLowerCase() + "_merge";
    }

    @Override
    public String getDisplay() {
        return "Merge";
    }

    @Override
    public int getOperandCountMax(FileTypeCategory category) {
        return category == this.category ? Integer.MAX_VALUE : 0;
    }

    @Override
    public SeqSymmetry operate(BioSeq aseq, List<SeqSymmetry> symList) {
        SimpleSymWithProps result = new SimpleSymWithProps();
        result.setProperties(new HashMap<String, Object>());
        for (int i = 0; i < symList.size(); i++) {
            if (symList.get(i) instanceof TypeContainerAnnot) {
                TypeContainerAnnot t = (TypeContainerAnnot) symList.get(i);
                if (result.getProperty("type") == null) {
                    result.setProperties(new HashMap<String, Object>());
                    result.setProperty("type", t.getType());
                }
                // copy children
                for (int j = 0; j < t.getChildCount(); j++) {
                    result.addChild(t.getChild(j));
                }
                // copy spans
                for (int j = 0; j < t.getSpanCount(); j++) {
                    result.addSpan(t.getSpan(j));
                }
                // copy properties
                Map<String, Object> properties = t.getProperties();
                for (String key : properties.keySet()) {
                    Object val = result.getProperty(key);
                    Object loopVal = properties.get(key);
                    if (val == null) {
                        result.setProperty(key, loopVal);
                    } else if (val instanceof String && loopVal instanceof String && ("," + val + ",").indexOf("," + loopVal + ",") == -1) {
                        result.setProperty(key, ((String) val) + "," + loopVal);
                    }
                }
//				result.setProperties(new HashMap<String,Object>(t.getProperties()));
            }
        }
        if (result.getID() == null) {
            result.setProperty("id", "");
        }
        return result;
    }
}
