package com.affymetrix.igb.swing.script;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptProcessorHolder {

    private static final ScriptProcessorHolder INSTANCE = new ScriptProcessorHolder();
    private final Map<String, ScriptProcessor> scriptProcessors = new HashMap<>();

    private ScriptProcessorHolder() {
        super();
        // IGBF-1182: We don't support javascript file to run a script(Tools->Script->Run Script). 
    }

    public static ScriptProcessorHolder getInstance() {
        return INSTANCE;
    }

    public void addScriptProcessor(ScriptProcessor scriptProcessor) {
        scriptProcessors.put(scriptProcessor.getExtension(), scriptProcessor);
    }

    public void removeScriptProcessor(ScriptProcessor scriptProcessor) {
        scriptProcessors.remove(scriptProcessor.getExtension());
    }

    public ScriptProcessor getScriptProcessor(String extension) {
        return scriptProcessors.get(extension);
    }

    public List<String> getScriptExtensions() {
        ArrayList<String> scriptExtensions = new ArrayList<>();
        scriptExtensions.addAll(scriptProcessors.keySet());
        return scriptExtensions;
    }

    public Collection<String> getSaveScriptExtensions() {
        List<String> saveScriptExtensions = new ArrayList<>();
        scriptProcessors.keySet().stream()
                .filter(extension -> scriptProcessors.get(extension).canWriteScript())
                .forEach(saveScriptExtensions::add);
        return saveScriptExtensions;
    }
}