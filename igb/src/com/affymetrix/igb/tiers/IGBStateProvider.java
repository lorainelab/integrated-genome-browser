
package com.affymetrix.igb.tiers;

import java.util.*;

import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphStateI;
import com.affymetrix.genometryImpl.style.IAnnotStyleExtended;
import com.affymetrix.genometryImpl.style.StateProvider;

public class IGBStateProvider implements StateProvider {

   Map<String,GraphState> id2graphState = new HashMap<String,GraphState>();

  /** Creates a new instance of IGBStateProvider */
  public IGBStateProvider() {
  }

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
     //    return new GraphState(name);
  }

}
