package com.affymetrix.genometryImpl.general;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author hiralv
 */
public class Parameters implements IParameters {
	
	private Map<String, Class<?>> PARAMETERS_TYPE;
	private Map<String, Parameter> PARAMETERS_VALUE;
	
	public Parameters(){
		PARAMETERS_TYPE = new HashMap<String, Class<?>>();
		PARAMETERS_VALUE = new HashMap<String, Parameter>();
	}
	
	public void addParameter(String key, Class<?> clazz, Parameter parameter){
		PARAMETERS_TYPE.put(key, clazz);
		PARAMETERS_VALUE.put(key, parameter);
	}
	
	@Override
	public Map<String, Class<?>> getParametersType(){
		return PARAMETERS_TYPE;
	}
	
	@Override
	public void setParametersValue(Map<String, Object> params){
		for(Map.Entry<String, Object> param : params.entrySet()){
			setParameterValue(param.getKey(), param.getValue());
		}
	}
	
	@Override
	public boolean setParameterValue(String key, Object value) {
		if (PARAMETERS_TYPE.get(key).isInstance(value)) {
			return PARAMETERS_VALUE.get(key).set(value);
		}
		return false;
	}

	@Override
	public Object getParameterValue(String key) {
		return PARAMETERS_VALUE.get(key).get();
	}
}
