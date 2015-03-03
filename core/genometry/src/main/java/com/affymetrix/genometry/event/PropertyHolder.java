package com.affymetrix.genometry.event;

import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.util.List;
import java.util.Map;

public interface PropertyHolder {

    public List<Map<String, Object>> getProperties();

    public Map<String, Object> determineProps(SeqSymmetry sym);
}
