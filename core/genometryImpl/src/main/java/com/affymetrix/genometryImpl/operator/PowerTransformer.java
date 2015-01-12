package com.affymetrix.genometryImpl.operator;

import com.affymetrix.genometryImpl.general.IParameters;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PowerTransformer extends AbstractFloatTransformer implements Operator, IParameters {
	private static final DecimalFormat DF = new DecimalFormat("#,##0.##");
	double exponent;
	final String paramPrompt;
	final String name;
	final boolean parameterized;

	public PowerTransformer() {
		super();
		this.exponent = 2.0;
		paramPrompt = "Exponent";
		name = "Power";
		parameterized = true;
	}
	public PowerTransformer(Double exponent) {
		super();
		this.exponent = exponent;
		paramPrompt = null;
		name = getBaseName();
		parameterized = false;
	}
	private String getBaseName() {
		if (exponent == 0.5) {
			return "Sqrt";
		}
		else {
			return parameterized ? "Power" : "Power" + DF.format(exponent);
		}
	}
	@Override
	public String getParamPrompt() { return paramPrompt; }
	
	public String getName() {
		return name;
	}
	public String getDisplay() {
		return parameterized ? getBaseName() : name;
	}
	public float transform(float x) {
		return (float)Math.pow(x, exponent);
	}
	
	@Override
	public Map<String, Class<?>> getParametersType() {
		if (getParamPrompt() == null) {
			return null;
		}
		Map<String, Class<?>> parameters = new HashMap<>();
		parameters.put(getParamPrompt(), String.class);
		return parameters;
	}
	
	@Override
	public boolean setParametersValue(Map<String, Object> parms) {
		if (getParamPrompt() != null && parms.size() == 1 && parms.get(getParamPrompt()) instanceof String) {
			setParameterValue(getParamPrompt(), (String)parms.get(getParamPrompt()));
			return true;
		}
		return false;
	}
	
	@Override
	public boolean setParameterValue(String key, Object value) {
		if (parameterized && value != null) {
			if ("sqrt".equals(value.toString().trim().toLowerCase())) {
				exponent = 0.5;
			}
			else {
				try {
					exponent = Double.parseDouble(value.toString());
				}
				catch (Exception x) {
					return false;
				}
			}
		}
		return true;
	}

	@Override
	public Object getParameterValue(String key) {
		if(key != null && key.equalsIgnoreCase(getParamPrompt())){
			return exponent;
		}
		return null;
	}
	
	@Override
	public List<Object> getParametersPossibleValues(String key){
		return null;
	}
	
	@Override
	public String getPrintableString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getParamPrompt()).append(":").append(getParameterValue(getParamPrompt()));
		return sb.toString();
	}
}
