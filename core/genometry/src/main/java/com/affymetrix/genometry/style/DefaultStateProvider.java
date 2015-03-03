/**
 * Copyright (c) 2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License").
 * A copy of the license must be included with any distribution of
 * this source code.
 * Distributions from Affymetrix, Inc., place this in the
 * IGB_LICENSE.html file.
 *
 * The license is also available at
 * http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.genometry.style;

import java.util.HashMap;
import java.util.Map;

public class DefaultStateProvider implements StateProvider {

    private static final Map<String, ITrackStyleExtended> id2annotState = new HashMap<>();
    private static final Map<String, GraphState> id2graphState = new HashMap<>();
    private static StateProvider globalStateProvider = new DefaultStateProvider();

    /**
     * Creates a new instance of DefaultIAnnotStyleProvider
     */
    protected DefaultStateProvider() {
    }

    public static StateProvider getGlobalStateProvider() {
        return globalStateProvider;
    }

    public static void setGlobalStateProvider(StateProvider sp) {
        globalStateProvider = sp;
    }

    /**
     * Returns a style for the given name. These styles remain associated
     * with the given name while the program is running, but do not get
     * persisted to a permanent storage. (Subclasses may choose to store
     * them in persistent storage.)
     *
     * @param name Unique name for a style
     * @return A new or existing style
     */
    public ITrackStyleExtended getAnnotStyle(String name) {
        ITrackStyleExtended style = id2annotState.get(name.toLowerCase());
        if (style == null) {
            style = new SimpleTrackStyle(name, false);
            id2annotState.put(name.toLowerCase(), style);
        }
        return style;
    }

    public ITrackStyleExtended getAnnotStyle(String name, String human_name, String file_type, Map<String, String> props) {
        return getAnnotStyle(name);
    }

    public GraphState getGraphState(String id) {
        return getGraphState(id, null, null, null);
    }

    public GraphState getGraphState(String id, String human_name, String extension, Map<String, String> props) {
        GraphState state = id2graphState.get(id);
        if (state == null) {
            state = new GraphState(id, human_name, extension, props);
            id2graphState.put(id, state);
        }
        return state;
    }

    public void removeAnnotStyle(String name) {
        id2annotState.remove(name);
    }

    public void removeGraphState(String name) {
        GraphState state = id2graphState.get(name);
        if (state != null) {
            ITrackStyleExtended style = state.getTierStyle();
            if (style != null) {
                getGlobalStateProvider().removeAnnotStyle(style.getMethodName());
            }
        }
        id2graphState.remove(name);
    }
}
