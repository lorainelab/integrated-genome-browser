/**
 *   Copyright (c) 2007 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */

package com.affymetrix.genometryImpl.style;

import java.util.*;

public class DefaultStateProvider implements StateProvider {

	Map<String,IAnnotStyleExtended> id2annotState;
	Map<String,GraphState> id2graphState;
	static StateProvider globalStateProvider = new DefaultStateProvider();
	public static final String DEFAULT_INSTANCE_NAME = "* default *";
	SimpleAnnotStyle default_instance = new SimpleAnnotStyle(DEFAULT_INSTANCE_NAME, false);

	/** Creates a new instance of DefaultIAnnotStyleProvider */
	public DefaultStateProvider() {
		id2annotState = new HashMap<String,IAnnotStyleExtended>();
		id2graphState = new HashMap<String,GraphState>();
	}

	public static StateProvider getGlobalStateProvider() {
		return globalStateProvider;
	}

	public static void setGlobalStateProvider(StateProvider sp) {
		globalStateProvider = sp;
	}

	/**
	 *  Returns a style for the given name.  These styles remain associated
	 * with the given name while the program is running, but do not get
	 * persisted to a permanent storage.  (Subclasses may choose to store
	 * them in persistent storage.)
	 * @param name Unique name for a style
	 * @return A new or existing style
	 */
	public IAnnotStyleExtended getAnnotStyle(String name) {
		IAnnotStyleExtended style = id2annotState.get(name.toLowerCase());
		if (style == null) {
			style = new SimpleAnnotStyle(name, false);
			id2annotState.put(name.toLowerCase(), style);
		}
		return style;
	}

	public IAnnotStyleExtended getDefaultAnnotStyle() {
		if (id2annotState.get(DEFAULT_INSTANCE_NAME) == null) {
			id2annotState.put(DEFAULT_INSTANCE_NAME, default_instance);
		}
		return default_instance;
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
