
package com.affymetrix.igb.tiers;

import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.util.GeneralUtils;

public final class IGBStateProvider extends DefaultStateProvider {
 
  @Override
  public GraphState getGraphState(String id, String human_name, String extension, java.util.Map<String, String> props) {
	if(human_name == null){
		String unzippedName = GeneralUtils.getUnzippedName(id);
		human_name = unzippedName.substring(unzippedName.lastIndexOf("/") + 1);
	}
	return super.getGraphState(id, human_name, extension, props);
  }
  	
  @Override
  public ITrackStyleExtended getAnnotStyle(String name) {
    return TrackStyle.getInstance(name);
  }

  @Override
  public ITrackStyleExtended getAnnotStyle(String name, String human_name, String file_type, java.util.Map<String, String> props) {
	 return TrackStyle.getInstance(name, human_name, file_type, props);
  }
  
}
