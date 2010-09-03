
package com.affymetrix.igb.tiers;

import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;

public final class IGBStateProvider extends DefaultStateProvider {

	@Override
  public ITrackStyleExtended getAnnotStyle(String name) {
    return TrackStyle.getInstance(name,true);
  }

	@Override
  public ITrackStyleExtended getAnnotStyle(String name, String human_name) {
	 return TrackStyle.getInstance(name,human_name);
  }

}
