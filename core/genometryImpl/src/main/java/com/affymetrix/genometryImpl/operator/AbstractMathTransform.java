package com.affymetrix.genometryImpl.operator;

import com.affymetrix.genometryImpl.GenometryConstants;
import com.affymetrix.genometryImpl.general.IParameters;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author hiralv
 */
public abstract class AbstractMathTransform extends AbstractFloatTransformer implements Operator, IParameters{

	protected static final DecimalFormat DF = new DecimalFormat("#,##0.##");
	
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
	public List<Object> getParametersPossibleValues(String key){
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
	
	@Override
	public String getPrintableString() {
		StringBuilder sb = new StringBuilder();
		sb.append(getParamPrompt()).append(":").append(getParameterValue(getParamPrompt()));
		return sb.toString();
	}
	
	protected boolean allowZero(){
		return true;
	}
	
	protected boolean allowNegative(){
		return true;
	}
}
