package com.affymetrix.genoviz.swing.recordplayback;

import com.affymetrix.genoviz.swing.recordplayback.Operation;
import com.affymetrix.genoviz.swing.recordplayback.ScriptProcessor;

public class PythonScriptProcessor implements ScriptProcessor {
	private static final String EXTENSION = "py";
	@Override
	public String getExtension() {
		return EXTENSION;
	}

	@Override
	public String getHeader() {
		return "from com.affymetrix.main import OSGiHandler\n" +
		"bundleContext = OSGiHandler.getInstance().getBundleContext()\n" +
		"serviceReference = bundleContext.getServiceReference(\"com.affymetrix.genoviz.swing.recordplayback.ScriptManager\")\n" +
		"sm = bundleContext.getService(serviceReference)\n";
	}

	@Override
	public String getCommand(Operation operation) {
		return "sm.getWidget(\"" + operation.getId() + "\")." + operation.toString();
	}
}
