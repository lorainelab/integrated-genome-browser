package com.gene.geometricmean;

import java.util.List;

import com.affymetrix.genometry.operator.AbstractGraphOperator;
import com.affymetrix.genometry.operator.Operator;

public class GeometricMeanOperator extends AbstractGraphOperator implements Operator {

    @Override
    public String getName() {
        return "geometric_mean";
    }

    public String getDisplay() {
        return "Geometric Mean";
    }

    @Override
    public String getSymbol() {
        return null;
    }

    @Override
    public float operate(List<Float> operands) {
        float total = 1;
        for (Float f : operands) {
            total *= f.floatValue();
        }
        return (float) Math.pow(total, 1.0 / operands.size());
    }
}
