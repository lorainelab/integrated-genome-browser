package com.affymetrix.genometryImpl.operator;

import com.affymetrix.genometryImpl.GenometryConstants;
import com.affymetrix.genometryImpl.general.IParameters;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author hiralv
 */
public abstract class AbstractMathTransform extends AbstractFloatTransformer implements Operator, IParameters{

	protected static final DecimalFormat DF = new DecimalFormat("#,##0.##");
	double base;
	protected String paramPrompt;
	protected String name;
	protected boolean parameterized;
	
	public AbstractMathTransform() {
		super();
		paramPrompt = "Value";
		name = getBaseName();
		parameterized = true;
	}
	
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
		Map<String, Class<?>> parameters = new HashMap<String, Class<?>>();
		parameters.put(paramPrompt, String.class);
		return parameters;
	}
	
	@Override
	public boolean setParametersValue(Map<String, Object> parms) {
		if (paramPrompt != null && parms.size() == 1 && parms.get(paramPrompt) instanceof String) {
			setParameterValue(paramPrompt, (String)parms.get(paramPrompt));
			return true;
		}
		return false;
	}
	
	@Override
	public Object getParameterValue(String key) {
		if(key != null && key.equalsIgnoreCase(getParamPrompt())){
			return base;
		}
		return null;
	}

	@Override
	public boolean setParameterValue(String key, Object value) {
		if (parameterized && value != null) {
			if ("e".equals(value.toString().trim().toLowerCase())) {
				base = Math.E;
			}
			else {
				try {
					base = Double.parseDouble(value.toString());
					if (base <= 0) {
						return false;
					}
				}
				catch (Exception x) {
					return false;
				}
			}
		}
		return true;
	}
	
	@Override
	public Operator newInstance(){
		try {
			if(parameterized){
				return getClass().getConstructor().newInstance();
			}
			return getClass().getConstructor(Double.class).newInstance(base);
		} catch (Exception ex) {
		}
		return null;
	}
}
