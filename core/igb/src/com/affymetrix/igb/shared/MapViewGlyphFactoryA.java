package com.affymetrix.igb.shared;

import java.util.Map;
import java.util.MissingResourceException;

import com.affymetrix.igb.IGBConstants;

public abstract class MapViewGlyphFactoryA implements MapViewGlyphFactoryI {
	
	@Override
	public void init(Map<String, Object> options) {	}
	
	@Override
	public String getDisplayName() {
		String displayName = null;
		try {
			displayName = IGBConstants.BUNDLE.getString("viewmode_" + getName());
		}
		catch(MissingResourceException x) {
			displayName = getName();
		}
		return displayName;
	}

	@Override
	public boolean supportsTwoTrack() {
		return false;
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
