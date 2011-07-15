package com.affymetrix.genoviz.swing.recordplayback;

public class Operation {
	public static final String PLAYBACK_COMMAND = "rph.";
	private final String id;
	private final String commandString;
	public Operation(String id, String commandString) {
		super();
		this.id = id;
		this.commandString = commandString;
	}
	public String toString() {
		return id + "." + commandString;
	}

	public String getId() {
		return id;
	}
}
