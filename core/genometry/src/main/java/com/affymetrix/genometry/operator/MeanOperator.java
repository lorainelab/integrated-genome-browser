/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometry.operator;

import com.affymetrix.genometry.GenometryConstants;
import java.util.List;

/**
 *
 * @author auser
 */
public class MeanOperator extends AbstractGraphOperator implements Operator, Operator.Order {

    @Override
    protected String getSymbol() {
        return null;
    }

    @Override
    protected float operate(List<Float> operands) {
        float total = 0;
        for (Float f : operands) {
            total += f;
        }
        return total / operands.size();
    }

    @Override
    public String getName() {
        return "mean";
    }

    @Override
    public String getDisplay() {
        return GenometryConstants.BUNDLE.getString("operator_" + getName());
    }

    @Override
    public int getOrder() {
        return 5;
    }
}
