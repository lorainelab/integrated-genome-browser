package com.affymetrix.genometry.operator;

import com.affymetrix.genometry.GenometryConstants;

/**
 *
 * @author lfrohman
 */
public abstract class AbstractLogTransform extends AbstractMathTransform {

    public AbstractLogTransform(Double base) {
        super();
        this.base = base;
        name = getBaseName() + "_" + base;
        parameterized = true;
    }

    protected abstract String getBaseName();

    @Override
    public String getDisplay() {
        if (base == Math.E) {
            return GenometryConstants.BUNDLE.getString("operator_" + getBaseName() + "_ln");
        } else {
            return GenometryConstants.BUNDLE.getString("operator_" + getBaseName());
        }
    }

    @Override
    public boolean setParameterValue(String key, Object value) {
        if (parameterized && value != null) {
            if ("e".equals(value.toString().trim().toLowerCase())) {
                base = Math.E;
                return true;
            }
        }
        return super.setParameterValue(key, value);
    }

    @Override
    protected boolean allowZero() {
        return false;
    }

    @Override
    protected boolean allowNegative() {
        return false;
    }
}
