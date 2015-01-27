package com.affymetrix.genometry.event;

import java.util.List;
import java.util.Map;

import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;

public interface PropertyHolder {
	public List<Map<String, Object>> getProperties();
	public Map<String, Object> determineProps(SeqSymmetry sym);
}
