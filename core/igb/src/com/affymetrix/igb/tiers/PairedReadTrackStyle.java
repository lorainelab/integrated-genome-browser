package com.affymetrix.igb.tiers;

import com.affymetrix.igb.stylesheet.PropertyMap;
import java.util.Map;

/**
 *
 * @author hiralv
 */
public class PairedReadTrackStyle extends TrackStyle {
	
	public PairedReadTrackStyle(PropertyMap props) {
		super(props);
	}

	PairedReadTrackStyle(String unique_ame, String track_name, String file_type, boolean is_persistent, TrackStyle template, Map<String, String> properties) {
		super(unique_ame, track_name, file_type, is_persistent, template, properties);
	}
}
