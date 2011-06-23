
package com.affymetrix.igb.tiers;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import java.util.Map;

public final class IGBStateProvider extends DefaultStateProvider {

	@Override
  public ITrackStyleExtended getAnnotStyle(String name) {
    return TrackStyle.getInstance(name,true);
  }

	@Override
  public ITrackStyleExtended getAnnotStyle(String name, String human_name, String file_type) {
	 return TrackStyle.getInstance(name,human_name,file_type);
  }

	@Override
  public ITrackStyleExtended getAnnotStyle(String name, String human_name, String file_type, Map<String, String> props) {
	 return TrackStyle.getInstance(name,human_name,file_type,props);
  }
}
