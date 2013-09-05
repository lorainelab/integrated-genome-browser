package com.affymetrix.genometryImpl.general;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
	public boolean setParametersValue(Map<String, Object> params){
		boolean retValue = true;
		for(Map.Entry<String, Object> param : params.entrySet()){
			retValue &= setParameterValue(param.getKey(), param.getValue());
		}
		return retValue;
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
	
	@Override
	public String getPrintableString() {
		return toString();
	}
	
	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		for(Entry<String, Parameter> entry : PARAMETERS_VALUE.entrySet()){
			sb.append(entry.getKey()).append(":").append(entry.getValue().toString()).append(",");
		}
		return sb.toString();
	}
}
