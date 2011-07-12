package com.affymetrix.igb.osgi.service;

import java.util.List;
import java.util.Map;

import com.affymetrix.genometryImpl.SeqSymmetry;

public interface PropertyHolder {
	public List<Map<String, Object>> getProperties();
	public Map<String, Object> determineProps(SeqSymmetry sym);
}
