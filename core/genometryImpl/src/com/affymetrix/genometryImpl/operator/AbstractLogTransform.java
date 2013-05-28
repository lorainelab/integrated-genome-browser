
package com.affymetrix.genometryImpl.operator;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

import com.affymetrix.genometryImpl.GenometryConstants;
import com.affymetrix.genometryImpl.general.IParameters;

/**
 *
 * @author lfrohman
 */
public abstract class AbstractLogTransform extends AbstractFloatTransformer implements Operator, IParameters{

	protected static final DecimalFormat DF = new DecimalFormat("#,##0.##");
	double base;
	protected String paramPrompt;
	protected String name;
	protected boolean parameterized;
	
	public AbstractLogTransform() {
		super();
		paramPrompt = "Base";
		name = getBaseName();
		parameterized = true;
	}
	public AbstractLogTransform(Double base) {
		super();
		this.base = base;
		paramPrompt = null;
		name = getBaseName() + "_" + base;
		parameterized = false;
	}
	
	protected abstract String getBaseName();
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getDisplay() {
		if (base == Math.E) {
			return GenometryConstants.BUNDLE.getString("operator_" + getBaseName() + "_ln");
		}
		else {
			return GenometryConstants.BUNDLE.getString("operator_" + getBaseName()) + (base == 0 ? "" : " " + DF.format(base));
		}
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
	public Operator clone(){
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
