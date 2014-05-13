package com.affymetrix.genometryImpl.filter;

import com.affymetrix.genometryImpl.GenometryConstants;
import com.affymetrix.genometryImpl.general.IParameters;
import com.affymetrix.genometryImpl.general.Parameters;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import java.util.List;
import java.util.Map;

/**
 *
 * @author hiralv
 */
public abstract class SymmetryFilter implements SymmetryFilterI, IParameters {
	protected Parameters parameters;
	
	protected SymmetryFilter(){
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
	public List<Object> getParametersPossibleValues(String key){
		return parameters.getParametersPossibleValues(key);
	}
	
	@Override
	public String getDisplay() {
		return GenometryConstants.BUNDLE.getString("filter_" + getName());
	}
	
	@Override
	public boolean isFileTypeCategorySupported(FileTypeCategory fileTypeCategory) {
		return fileTypeCategory == FileTypeCategory.Annotation 
				|| fileTypeCategory == FileTypeCategory.Alignment
				|| fileTypeCategory == FileTypeCategory.ProbeSet;
	}
	
	@Override
	public SymmetryFilterI newInstance(){
		try {
			SymmetryFilterI newInstance = getClass().getConstructor().newInstance();
			if(newInstance instanceof IParameters) {
				for (String key : getParametersType().keySet()) {
					((IParameters) newInstance).setParameterValue(key, getParameterValue(key));
				}
			}
			return newInstance;
		} catch (Exception ex) {
		}
		return null;
	}
	
	@Override
	public String getPrintableString() {
		return parameters.getPrintableString();
	}
}
