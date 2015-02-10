package com.gene.igbscript;

import javax.script.ScriptEngineFactory;

import com.affymetrix.igb.swing.Operation;
import com.affymetrix.igb.swing.script.ScriptProcessor;
import com.affymetrix.igb.service.api.IgbService;

public class IGBScriptProcessor implements ScriptProcessor {

    private IgbService igbService;

    public IGBScriptProcessor(IgbService igbService) {
        super();
        this.igbService = igbService;
    }

    @Override
    public String getExtension() {
        return "igb";
    }

    @Override
    public ScriptEngineFactory getScriptEngineFactory() {
        return new IGBScriptEngineFactory(igbService);
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
}
