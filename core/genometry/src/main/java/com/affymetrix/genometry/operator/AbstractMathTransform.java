package com.affymetrix.genometry.operator;

import com.affymetrix.genometry.GenometryConstants;
import com.affymetrix.genometry.general.IParameters;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author hiralv
 */
public abstract class AbstractMathTransform extends AbstractFloatTransformer implements Operator, IParameters {

    protected static final DecimalFormat DF = new DecimalFormat("#,##0.##");
    private static final Logger logger = LoggerFactory.getLogger(AbstractMathTransform.class);

    private String paramPrompt;
    protected double base;
    protected String name;
    protected boolean parameterized;

    public AbstractMathTransform() {
        super();
        paramPrompt = getParameterName();
        name = getBaseName();
        parameterized = true;
    }

    protected abstract String getParameterName();

    protected abstract String getBaseName();

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisplay() {
        return GenometryConstants.BUNDLE.getString("operator_" + getBaseName());
    }

    @Override
    public String getParamPrompt() {
        return paramPrompt;
    }

    @Override
    public Map<String, Class<?>> getParametersType() {
        if (paramPrompt == null) {
            return null;
        }
        Map<String, Class<?>> parameters = new HashMap<>();
        parameters.put(paramPrompt, String.class);
        return parameters;
    }

    @Override
    public boolean setParametersValue(Map<String, Object> parms) {
        if (paramPrompt != null && parms.size() == 1 && parms.get(paramPrompt) instanceof String) {
            setParameterValue(paramPrompt, parms.get(paramPrompt));
            return true;
        }
        return false;
    }

    @Override
    public Object getParameterValue(String key) {
        if (key != null && key.equalsIgnoreCase(getParamPrompt())) {
            return base;
        }
        return null;
    }

    @Override
    public List<Object> getParametersPossibleValues(String key) {
        return null;
    }

    @Override
    public boolean setParameterValue(String key, Object value) {
        if (parameterized && value != null) {
            try {
                base = Double.parseDouble(value.toString());
                if (!allowNegative() && base < 0) {
                    return false;
                }
                if (!allowZero() && base == 0) {
                    return false;
                }
            } catch (Exception x) {
                return false;
            }

        }
        return true;
    }

    @Override
    public Operator newInstance() {
        try {
            if (parameterized) {
                try {
                    return getClass().getConstructor(Double.class).newInstance(base);
                } catch (NoSuchMethodException ex) {
                    return getClass().getConstructor().newInstance();
                }
            }
            return getClass().getConstructor().newInstance();
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return null;
    }

    @Override
    public String getPrintableString() {
        return getParamPrompt() + ":" + getParameterValue(getParamPrompt());
    }

    protected boolean allowZero() {
        return true;
    }

    protected boolean allowNegative() {
        return true;
    }

}
