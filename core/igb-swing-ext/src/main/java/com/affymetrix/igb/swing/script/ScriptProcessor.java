package com.affymetrix.igb.swing.script;

import com.affymetrix.igb.swing.Operation;
import javax.script.ScriptEngineFactory;

public interface ScriptProcessor {

    public String getExtension();

    public ScriptEngineFactory getScriptEngineFactory();

    public String getHeader();

    public String getCommand(Operation operation);

    public boolean canWriteScript();
}
