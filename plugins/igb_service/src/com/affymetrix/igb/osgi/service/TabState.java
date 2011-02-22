package com.affymetrix.igb.osgi.service;

public enum TabState {
	COMPONENT_STATE_LEFT_TAB("LEFT_TAB", true),
	COMPONENT_STATE_RIGHT_TAB("RIGHT_TAB", true),
	COMPONENT_STATE_BOTTOM_TAB("BOTTOM_TAB", true),
	COMPONENT_STATE_WINDOW("WINDOW", false),
	COMPONENT_STATE_HIDDEN("HIDDEN", false);

	private final String name;
	private final boolean tab;

	TabState(String name, boolean tab) {
		this.name = name;
		this.tab = tab;
	}

	public String getName() {
		return name;
	}

	public boolean isTab() {
		return tab;
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
