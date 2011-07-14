package com.affymetrix.genoviz.swing.recordplayback;

public class Operation {
	public static final String PLAYBACK_COMMAND = "rph.execute";
	private final String[] parms;
	public Operation(JRPWidget widget) {
		super();
		parms = widget.getParms();
	}
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(PLAYBACK_COMMAND + "(");
    	boolean started = false;
    	for (String s : parms) {
    		if (started) {
    			sb.append(", ");
    		}
    		else {
    			started = true;
    		}
    		sb.append("\"" + s + "\"");
    	}
		sb.append(")");
		return sb.toString();
	}
}
