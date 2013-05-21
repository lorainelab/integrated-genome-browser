package com.affymetrix.genometryImpl.general;

import java.util.Map;

/**
 *
 * @author hiralv
 */
public interface IParameters {

	public Object getParameterValue(String key);

	public Map<String, Class<?>> getParametersType();

	public boolean setParameterValue(String key, Object value);

	public void setParametersValue(Map<String, Object> params);
    
}
