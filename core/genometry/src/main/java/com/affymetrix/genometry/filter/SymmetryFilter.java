package com.affymetrix.genometry.filter;

import com.affymetrix.genometry.GenometryConstants;
import com.affymetrix.genometry.general.IParameters;
import com.affymetrix.genometry.general.Parameters;
import com.affymetrix.genometry.parsers.FileTypeCategory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *
 * @author hiralv
 */
public abstract class SymmetryFilter implements SymmetryFilterI, IParameters {

    protected Parameters parameters;

    protected Map<String, List<String>> filterMap;

    public enum Direction {

        FORWARD(" (+)"), NONE(""), REVERSE(" (-)"), BOTH(" (+|-)"), AXIS("");
        private final String display;

        private Direction(String display) {
            this.display = display;
        }

        public String getDisplay() {
            return display;
        }
    }

    protected SymmetryFilter() {
        parameters = new Parameters();
        filterMap = new HashMap<>();
    }

    @Override
    public Map<String, Class<?>> getParametersType() {
        return parameters.getParametersType();
    }

    public Map<String, List<String>> getFilterMap() {
        return filterMap;
    }

    public void setFilterMap(Map<String, List<String>> filterMap) {
        this.filterMap = filterMap;
    }
    @Override
    public boolean setParametersValue(Map<String, Object> params) {
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
    public List<Object> getParametersPossibleValues(String key) {
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
    public SymmetryFilterI newInstance() {
        try {
            SymmetryFilter newInstance = getClass().getConstructor().newInstance();
            if (newInstance instanceof IParameters) {
                for (String key : getParametersType().keySet()) {
                    ((IParameters) newInstance).setParameterValue(key, getParameterValue(key));
                }
            }
            Map<String, List<String>> newFilterMap = new HashMap<>(filterMap);
            newInstance.setFilterMap(newFilterMap);
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
