package com.gene.igbscript;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import com.affymetrix.igb.swing.Operation;
import com.affymetrix.igb.swing.script.ScriptProcessor;
import com.affymetrix.igb.swing.script.ScriptProcessorHolder;
import javax.script.ScriptEngineFactory;

@Component(name = IGBScriptProcessor.COMPONENT_NAME, immediate = true, service = ScriptProcessor.class)
public class IGBScriptProcessor implements ScriptProcessor {

    public static final String COMPONENT_NAME = "IGBScriptProcessor";
    private IGBScriptEngineFactory scriptEngineFactory;

    public IGBScriptProcessor() {
    }

    @Activate
    public void activate() {
        ScriptProcessorHolder.getInstance().addScriptProcessor(this);
    }

    @Override
    public String getExtension() {
        return "igb";
    }

    @Override
    public ScriptEngineFactory getScriptEngineFactory() {
        return scriptEngineFactory;
    }

    @Override
    public String getHeader() {
        return "";
    }

    @Override
    public String getCommand(Operation operation) {
        return null;
    }

    @Override
    public boolean canWriteScript() {
        return false;
    }

    @Reference
    public void setScriptEngineFactory(IGBScriptEngineFactory scriptEngineFactory) {
        this.scriptEngineFactory = scriptEngineFactory;
    }

}
