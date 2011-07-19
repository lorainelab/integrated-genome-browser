package com.affymetrix.genoviz.swing.recordplayback;

public class Operation {
	public static final String PLAYBACK_COMMAND = "rph.";
	private final JRPWidget widget;
	private final String commandString;
	public Operation(JRPWidget widget, String commandString) {
		super();
		this.widget = widget;
		this.commandString = commandString;
	}
	public String toString() {
		return widget.getId() + "." + commandString;
	}

	public String getId() {
		return widget.getId();
	}

	public JRPWidget getWidget() {
		return widget;
	}
}
