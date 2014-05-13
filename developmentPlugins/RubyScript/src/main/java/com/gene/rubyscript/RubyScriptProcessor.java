package com.gene.rubyscript;

import javax.script.ScriptEngineFactory;

import com.sun.script.jruby.JRubyScriptEngineFactory;

import com.affymetrix.igb.swing.Operation;
import com.affymetrix.igb.swing.ScriptProcessor;

public class RubyScriptProcessor implements ScriptProcessor {

    private static final String EXTENSION = "rb";
    private ScriptEngineFactory factory = new JRubyScriptEngineFactory();

    @Override
    public String getExtension() {
        return EXTENSION;
    }

    @Override
    public String getHeader() {
        return "require \"java\"\n"
                + "include_package com.affymetrix.main\n"
                + "bundleContext = OSGiHandler.getInstance().getBundleContext()\n"
                + "serviceReference = bundleContext.getServiceReference(\"com.affymetrix.igb.swing.ScriptManager\")\n"
                + "sm = bundleContext.getService(serviceReference)\n";
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
