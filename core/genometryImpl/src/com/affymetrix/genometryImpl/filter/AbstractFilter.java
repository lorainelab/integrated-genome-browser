package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.general.IParameters;
import com.affymetrix.genometryImpl.general.Parameters;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import java.util.Map;

/**
 *
 * @author hiralv
 */
public abstract class AbstractFilter implements SymmetryFilterI, IParameters {
	protected Parameters parameters;
	
	protected AbstractFilter(){
		parameters = new Parameters();
	}
	
	@Override
	public Map<String, Class<?>> getParametersType(){
		return parameters.getParametersType();
	}

	@Override
	public final boolean setParametersValue(Map<String, Object> params){
		return parameters.setParametersValue(params);
	}
	
	@Override
	public boolean setParameterValue(String key, Object value) {
		return parameters.setParameterValue(key, value);
	}

	@Override
	public Object getParameterValue(String key) {
		return parameters.getParameterValue(key);
	}
	
	@Override
	public boolean isFileTypeCategorySupported(FileTypeCategory fileTypeCategory){
		return true;
	}
	
	@Override
	public SymmetryFilterI newInstance(){
		try {
			return getClass().getConstructor().newInstance();
		} catch (Exception ex) {
		}
		return null;
	}
}
