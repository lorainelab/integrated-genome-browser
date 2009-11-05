
package com.affymetrix.igb.tiers;

import java.util.*;

import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphStateI;
import com.affymetrix.genometryImpl.style.IAnnotStyleExtended;
import com.affymetrix.genometryImpl.style.StateProvider;

public final class IGBStateProvider implements StateProvider {

  private final Map<String,GraphState> id2graphState = new HashMap<String,GraphState>();

  public IAnnotStyleExtended getAnnotStyle(String name) {
    return AnnotStyle.getInstance(name);
  }

  public GraphStateI getGraphState(String id) {
      GraphState state = id2graphState.get(id);
      if (state == null) {
          state = new GraphState(id);
          id2graphState.put(id, state);
      }
      return state;
  }

}
