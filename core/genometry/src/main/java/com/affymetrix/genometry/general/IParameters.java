package com.affymetrix.genometry.general;

import java.util.List;
import java.util.Map;

/**
 *
 * @author hiralv
 */
public interface IParameters {

    /**
     * @param key
     * @return Returns values for given key
     */
    public Object getParameterValue(String key);

    /**
     * @param key
     * @return Returns list of possible values for given key, if any otherwise
     * returns null
     */
    public List<Object> getParametersPossibleValues(String key);

    /**
     * @return parameters for this instance
     */
    public Map<String, Class<?>> getParametersType();

    /**
     * Sets value for given key
     *
     * @param key
     * @param value
     * @return
     */
    public boolean setParameterValue(String key, Object value);

    /**
     * Used for properties specific to a type of instance.
     */
    public boolean setParametersValue(Map<String, Object> params);

    /**
     * @return Returns string contain properties and it's value
     */
    public String getPrintableString();

}
