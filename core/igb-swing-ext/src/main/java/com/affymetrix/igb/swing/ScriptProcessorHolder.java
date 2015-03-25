package com.affymetrix.igb.swing;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptProcessorHolder {

    private static final ScriptProcessorHolder instance = new ScriptProcessorHolder();
    private Map<String, ScriptProcessor> scriptProcessors = new HashMap<>();

    private ScriptProcessorHolder() {
        super();
        addScriptProcessor(new JavascriptScriptProcessor()); // guaranteed to be available
    }

    public static ScriptProcessorHolder getInstance() {
        return instance;
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
