
package com.affymetrix.igb.viewmode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.igb.shared.FasterExpandPacker;
import com.affymetrix.igb.shared.ViewModeGlyph;

/**
 *
 * @author hiralv
 */
public class ScoredContainerViewModeGlyph extends ViewModeGlyph{
	private static final Map<String,Class<?>> PREFERENCES;
	private FasterExpandPacker expand_packer = new FasterExpandPacker();
	static {
		Map<String,Class<?>> temp = new HashMap<String,Class<?>>();
		PREFERENCES = Collections.unmodifiableMap(temp);
	}

	ScoredContainerViewModeGlyph(ITrackStyleExtended style){
		super();
		setStyle(style);
	}
	

	@Override
	public void setStyle(ITrackStyleExtended style) {
		super.setStyle(style);
		setPacker(expand_packer);
	}
	
	@Override
	public void setPreferredHeight(double height, ViewI view) {
		// Still to be implemented
	}

	@Override
	public int getActualSlots() {
		return 1;
		// Still to be implemented
	}

	@Override
	public Map<String, Class<?>> getPreferences() {
		return new HashMap<String, Class<?>>(PREFERENCES);
	}

	@Override
	public void pack(ViewI view) {
		if(getChildCount() > 0){
			for(int i=0; i<getChildCount(); i++){
				getChild(i).pack(view);
			}
		}
		super.pack(view);
	}
	
	@Override
	public void setPreferences(Map<String, Object> preferences) {
	}
}
