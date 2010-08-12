
package com.affymetrix.igb.tiers;

import java.util.*;

import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.style.StateProvider;

public final class IGBStateProvider implements StateProvider {

  private final Map<String,GraphState> id2graphState = new HashMap<String,GraphState>();

  public ITrackStyleExtended getAnnotStyle(String name) {
    return TrackStyle.getInstance(name);
  }

  public GraphState getGraphState(String id) {
      GraphState state = id2graphState.get(id);
      if (state == null) {
          state = new GraphState(id);
          id2graphState.put(id, state);
      }
      return state;
  }

  public ITrackStyleExtended getAnnotStyle(String name, String human_name) {
	 return TrackStyle.getInstance(name,human_name);
  }

}
