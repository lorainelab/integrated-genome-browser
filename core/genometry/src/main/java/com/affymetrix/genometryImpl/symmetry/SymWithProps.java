package com.affymetrix.genometry.symmetry;

import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.util.Map;

/**
 * A SeqSymmetry with Properties.
 */
public interface SymWithProps extends SeqSymmetry {

    public Map<String, Object> getProperties();

    public Map<String, Object> cloneProperties();

    public Object getProperty(String key);

    public boolean setProperty(String key, Object val);
}
