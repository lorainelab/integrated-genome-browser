package com.affymetrix.genoviz.swing.recordplayback;

public interface JRPWidget {
	public static final String PLAYBACK_COMMAND = "rph.execute";
	public String getID();
	public void execute(String... params);
	public String[] getParms();
}
