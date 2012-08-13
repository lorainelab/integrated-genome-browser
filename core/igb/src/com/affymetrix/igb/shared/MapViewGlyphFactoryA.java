package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.igb.IGBConstants;
import java.util.Map;
import java.util.MissingResourceException;

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
	public boolean canAutoLoad(String uri) {
		return false;
	}

	@Override
	public String toString() {
		return getDisplayName();
	}
	
}
