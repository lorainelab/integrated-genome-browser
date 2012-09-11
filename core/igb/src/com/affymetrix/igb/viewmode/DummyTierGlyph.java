package com.affymetrix.igb.viewmode;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.igb.shared.FasterExpandPacker;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DummyTierGlyph extends AbstractTierGlyph {
	private static final Map<String,Class<?>> PREFERENCES;
	static {
		Map<String,Class<?>> temp = new HashMap<String,Class<?>>();
		PREFERENCES = Collections.unmodifiableMap(temp);
	}
	
	public DummyTierGlyph(ITrackStyleExtended style) {
		super();
		setStyle(style);
		this.setPacker(new FasterExpandPacker());
	}

	@Override
	public Map<String, Class<?>> getPreferences() {
		return PREFERENCES;
	}

	@Override
	public void setPreferences(Map<String, Object> preferences) {
	}
}
