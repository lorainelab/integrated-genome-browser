package com.affymetrix.igb.swing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScriptProcessorHolder {

	private static final ScriptProcessorHolder instance = new ScriptProcessorHolder();
	private Map<String, ScriptProcessor> scriptProcessors = new HashMap<String, ScriptProcessor>();

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
		ArrayList<String> scriptExtensions = new ArrayList<String>();
		scriptExtensions.addAll(scriptProcessors.keySet());
		return scriptExtensions;
	}

	public String[] getSaveScriptExtensions() {
		ArrayList<String> saveScriptExtensions = new ArrayList<String>();
		for (String extension : scriptProcessors.keySet()) {
			if (scriptProcessors.get(extension).canWriteScript()) {
				saveScriptExtensions.add(extension);
			}
		}
		return saveScriptExtensions.toArray(new String[saveScriptExtensions.size()]);
	}
}
