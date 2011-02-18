package com.affymetrix.igb.osgi.service;

public enum TabState {
	COMPONENT_STATE_LEFT_TAB("LEFT_TAB"),
	COMPONENT_STATE_RIGHT_TAB("RIGHT_TAB"),
	COMPONENT_STATE_BOTTOM_TAB("BOTTOM_TAB"),
	COMPONENT_STATE_WINDOW("WINDOW"),
	COMPONENT_STATE_HIDDEN("HIDDEN");

	private final String name;
	TabState(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public static TabState getTabStateByName(String tabStateString) {
		for (TabState tabStateLoop : TabState.values()) {
			if (tabStateLoop.getName().equals(tabStateString)) {
				return tabStateLoop;
			}
		}
		return getDefaultTabState();
	}

	public static TabState getDefaultTabState() {
		return COMPONENT_STATE_BOTTOM_TAB;
	}
}
