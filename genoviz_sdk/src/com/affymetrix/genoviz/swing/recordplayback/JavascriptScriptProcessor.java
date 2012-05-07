package com.affymetrix.genoviz.swing.recordplayback;

public class JavascriptScriptProcessor implements ScriptProcessor {
	private static final String EXTENSION = "js";

	@Override
	public String getExtension() {
		return EXTENSION;
	}

	@Override
	public String getHeader() {
		return "var bundleContext = com.affymetrix.main.OSGiHandler.getInstance().getBundleContext();\n" +
			"var serviceReference = bundleContext.getServiceReference(\"com.affymetrix.genoviz.swing.recordplayback.ScriptManager\");\n" +
			"var rph = bundleContext.getService(serviceReference);\n";
	}

	@Override
	public String getCommand(Operation operation) {
		return "rph.getWidget(\"" + operation.getId() + "\")." + operation.toString() + ";";
	}
}
