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
	private Map<String, Object> PARAMETERS_VALUE;
	
	public Parameters(){
		PARAMETERS_TYPE = new HashMap<String, Class<?>>();
		PARAMETERS_VALUE = new HashMap<String, Object>();
	}
	
	public void addParameter(String key, Class<?> clazz, Object parameter){
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
		for(Entry<String, Object> parameter : PARAMETERS_VALUE.entrySet()){
			if(parameter.getKey().equals(key) && PARAMETERS_TYPE.get(parameter.getKey()).isInstance(value)){
				parameter.setValue(value);
				return true;
			}
		}
		return false;
	}

	public Object getParameterValue(String key) {
		for(Entry<String, Object> parameter : PARAMETERS_VALUE.entrySet()){
			if(parameter.getKey().equals(key)){
				return parameter.getValue();
			}
		}
		return null;
	}
}
