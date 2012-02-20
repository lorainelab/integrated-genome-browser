
package com.affymetrix.genometryImpl.operator;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author lfrohman
 */
public abstract class AbsractLogTransform extends AbstractFloatTransformer implements Operator{

	protected static final DecimalFormat DF = new DecimalFormat("#,##0.##");
	double base;
	final String paramPrompt;
	final String name;
	final boolean parameterized;
	
	public AbsractLogTransform(String nm) {
		super();
		paramPrompt = "Base";
		name = nm;
		parameterized = true;
	}
	public AbsractLogTransform(Double base) {
		super();
		this.base = base;
		paramPrompt = null;
		name = getBaseName();
		parameterized = false;
	}
	
	protected abstract String getBaseName();
	
	@Override
	public Map<String, Class<?>> getParameters() {
		if (paramPrompt == null) {
			return null;
		}
		Map<String, Class<?>> parameters = new HashMap<String, Class<?>>();
		parameters.put(paramPrompt, String.class);
		return parameters;
	}
	
	@Override
	public boolean setParameters(Map<String, Object> parms) {
		if (paramPrompt != null && parms.size() == 1 && parms.get(paramPrompt) instanceof String) {
			setParameter((String)parms.get(paramPrompt));
			return true;
		}
		return false;
	}
	
	protected boolean setParameter(String s) {
		if (parameterized) {
			if ("e".equals(s.trim().toLowerCase())) {
				base = Math.E;
			}
			else {
				try {
					base = Double.parseDouble(s);
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
}
