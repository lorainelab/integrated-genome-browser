package com.affymetrix.genometry.operator;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author auser
 */
public class InverseLogTransform extends AbstractLogTransform implements Operator {

    private static final String BASE_NAME = "inverse_log";
    private static final String PARAMETER_NAME = "base";
    private boolean hideParamConfigOptions;
    private static final Logger logger = LoggerFactory.getLogger(InverseLogTransform.class);

    public InverseLogTransform() {
        this(0.0);
    }

    public InverseLogTransform(Double base) {
        super(base);
    }

    public InverseLogTransform(Double base, boolean hideParamConfigOptions) {
        super(base);
        this.hideParamConfigOptions = hideParamConfigOptions;
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

    @Override
    public Map<String, Class<?>> getParametersType() {
        if (hideParamConfigOptions) {
            return null;
        }
        return super.getParametersType();
    }

    @Override
    public Operator newInstance() {
        try {
            if (hideParamConfigOptions) {
                return getClass().getConstructor(Double.class, boolean.class).newInstance(base, true);
            } else {
                return super.newInstance();
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return null;
    }
}
