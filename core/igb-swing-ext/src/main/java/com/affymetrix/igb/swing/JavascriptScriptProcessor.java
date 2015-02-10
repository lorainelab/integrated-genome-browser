package com.affymetrix.igb.swing;

import com.affymetrix.igb.swing.script.ScriptProcessor;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;

public class JavascriptScriptProcessor implements ScriptProcessor {

	private static final String EXTENSION = "js";

	@Override
	public String getExtension() {
		return EXTENSION;
	}

	@Override
	public String getHeader() {
		return "var bundleContext = com.affymetrix.main.OSGiHandler.getInstance().getBundleContext();\n"
				+ "var serviceReference = bundleContext.getServiceReference(\"com.affymetrix.com.affymetrix.igb.swing.ScriptManager\");\n"
				+ "var sm = bundleContext.getService(serviceReference);\n"
				+ "var serviceReferenceWHF = bundleContext.getServiceReference(\"com.affymetrix.genometry.thread.WaitHelperI\");\n"
				+ "var whf = bundleContext.getService(serviceReferenceWHF);\n";
	}

	@Override
	public String getCommand(Operation operation) {
		return "sm.getWidget(\"" + operation.getId() + "\")." + operation.toString() + ";";
	}

	@Override
	public ScriptEngineFactory getScriptEngineFactory() {
		ClassLoader loader = Thread.currentThread().getContextClassLoader();
		ScriptEngineManager scriptEngineManager = new ScriptEngineManager(loader);
		for (ScriptEngineFactory factory : scriptEngineManager.getEngineFactories()) {
			if (factory.getExtensions().contains("js")) {
				return factory;
			}
		}
		return null;
	}

	@Override
	public boolean canWriteScript() {
		return true;
	}
}
