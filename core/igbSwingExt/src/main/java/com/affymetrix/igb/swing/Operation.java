package com.affymetrix.igb.swing;

public class Operation {

	private final JRPWidget widget;
	private final String commandString;

	public Operation(JRPWidget widget, String commandString) {
		super();
		this.widget = widget;
		this.commandString = commandString;
	}

	public String toString() {
		return commandString;
	}

	public String getId() {
		return widget.getId();
	}

	public JRPWidget getWidget() {
		return widget;
	}
}
