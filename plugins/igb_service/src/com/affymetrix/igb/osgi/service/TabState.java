package com.affymetrix.igb.osgi.service;

public enum TabState {
	COMPONENT_STATE_LEFT_TAB(true),
	COMPONENT_STATE_RIGHT_TAB(true),
	COMPONENT_STATE_BOTTOM_TAB(true),
	COMPONENT_STATE_WINDOW(false),
	COMPONENT_STATE_HIDDEN(false);

	private final boolean tab;

	TabState(boolean tab) {
		this.tab = tab;
	}

	public boolean isTab() {
		return tab;
	}

	public static TabState getDefaultTabState() {
		return COMPONENT_STATE_BOTTOM_TAB;
	}
}
