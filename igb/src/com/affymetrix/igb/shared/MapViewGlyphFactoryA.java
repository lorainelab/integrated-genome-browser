package com.affymetrix.igb.shared;

import java.util.Map;

import com.affymetrix.igb.IGBConstants;

public abstract class MapViewGlyphFactoryA implements MapViewGlyphFactoryI {
	
	@Override
	public void init(Map<String, Object> options) {	}
	
	@Override
	public String getDisplayName() {
		return IGBConstants.BUNDLE.getString("viewmode_" + getName());
	}
	
	@Override
	public boolean isURISupported(String uri) {
		return true;
	}

	@Override
	public boolean canAutoLoad(String uri) {
		return false;
	}
}
