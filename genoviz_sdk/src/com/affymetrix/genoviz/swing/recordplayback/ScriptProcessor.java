package com.affymetrix.genoviz.swing.recordplayback;

public interface ScriptProcessor {
	public String getExtension();
	public String getHeader();
	public String getCommand(Operation operation);
}
