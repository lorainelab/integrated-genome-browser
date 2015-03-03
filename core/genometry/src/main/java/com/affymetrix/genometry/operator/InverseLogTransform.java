package com.affymetrix.genometry.operator;

/**
 *
 * @author auser
 */
public class InverseLogTransform extends AbstractLogTransform implements Operator {

    private static final String BASE_NAME = "inverse_log";
    private static final String PARAMETER_NAME = "base";

    public InverseLogTransform() {
        this(0.0);
    }

    public InverseLogTransform(Double base) {
        super(base);
    }

    @Override
    protected String getParameterName() {
        return PARAMETER_NAME;
    }

    @Override
    protected String getBaseName() {
        return BASE_NAME;
    }

    @Override
    public float transform(float x) {
        return (float) (Math.pow(base, x));
    }
}
