package com.affymetrix.genometry.operator;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LogTransform extends AbstractLogTransform implements Operator {

    private static final Logger logger = LoggerFactory.getLogger(LogTransform.class);
    private static final String BASE_NAME = "log";
    private static final String PARAMETER_NAME = "base";
    private boolean hideParamConfigOptions;
    double LN_BASE;
    float LOG_1;

    public LogTransform() {
        this(0.0);
    }

    public LogTransform(Double base) {
        super(base);
        LN_BASE = Math.log(base);
        LOG_1 = (float) (Math.log(1) / LN_BASE);
    }

    public LogTransform(Double base, boolean hideParamConfigOptions) {
        super(base);
        LN_BASE = Math.log(base);
        LOG_1 = (float) (Math.log(1) / LN_BASE);
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

    public float transform(float x) {
        return (x <= 1) ? LOG_1 : (float) (Math.log(x) / LN_BASE);
    }

    @Override
    public boolean setParameterValue(String key, Object value) {
        if (parameterized && super.setParameterValue(key, value)) {
            if (!("e".equals(value.toString().trim().toLowerCase()))) {
                LN_BASE = Math.log(base);
                LOG_1 = (float) (Math.log(1) / LN_BASE);
                return true;
            }
        }
        return false;
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
