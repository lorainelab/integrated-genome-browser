package com.affymetrix.genometryImpl.color;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author hiralv
 */
public class Parameters {
	
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
	
	public Map<String, Class<?>> getParametersType(){
		return PARAMETERS_TYPE;
	}
	
	public void setParametersValue(Map<String, Object> params){
		for(Map.Entry<String, Object> param : params.entrySet()){
			setParameterValue(param.getKey(), param.getValue());
		}
	}
	
	public boolean setParameterValue(String key, Object value) {
		for(Entry<String, Parameter> parameter : PARAMETERS_VALUE.entrySet()){
			if(parameter.getKey().equals(key)){
				parameter.getValue().set(value);
				return true;
			}
		}
		return false;
	}

	public Object getParameterValue(String key) {
		for(Entry<String, Parameter> parameter : PARAMETERS_VALUE.entrySet()){
			if(parameter.getKey().equals(key)){
				return parameter.getValue().get();
			}
		}
		return null;
	}
}
