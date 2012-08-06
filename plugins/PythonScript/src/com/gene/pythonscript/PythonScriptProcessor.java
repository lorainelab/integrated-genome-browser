package com.gene.pythonscript;

import javax.script.ScriptEngineFactory;

import org.python.jsr223.PyScriptEngineFactory;

import com.affymetrix.genoviz.swing.recordplayback.Operation;
import com.affymetrix.genoviz.swing.recordplayback.ScriptProcessor;

public class PythonScriptProcessor implements ScriptProcessor {
	private static final String EXTENSION = "py";
	private ScriptEngineFactory factory = new PyScriptEngineFactory();
	@Override
	public String getExtension() {
		return EXTENSION;
	}

	@Override
	public String getHeader() {
		return "from com.affymetrix.main import OSGiHandler\n" +
		"bundleContext = OSGiHandler.getInstance().getBundleContext()\n" +
		"serviceReference = bundleContext.getServiceReference(\"com.affymetrix.genoviz.swing.recordplayback.ScriptManager\")\n" +
		"sm = bundleContext.getService(serviceReference)\n" +
		"serviceReferenceWHF = bundleContext.getServiceReference(\"com.affymetrix.genometryImpl.thread.WaitHelperI\")\n" +
		"whf = bundleContext.getService(serviceReferenceWHF)\n";
	}

	@Override
	public String getCommand(Operation operation) {
		return "sm.getWidget(\"" + operation.getId() + "\")." + operation.toString();
	}

	@Override
	public ScriptEngineFactory getScriptEngineFactory() {
		return factory;
	}

	@Override
	public boolean canWriteScript() {
		return true;
	}
}
