package com.affymetrix.genoviz.swing.recordplayback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class ScriptProcessorHolder {
	private static final ScriptProcessorHolder instance = new ScriptProcessorHolder();
	private Map<String, ScriptProcessor> scriptProcessors = new HashMap<String, ScriptProcessor>();
	private ScriptProcessorHolder() {
		super();
		addScriptProcessor(new JavascriptScriptProcessor());
		ScriptEngine engine = new ScriptEngineManager().getEngineByName("python");
		System.out.println("************ " + engine);
		addScriptProcessor(new PythonScriptProcessor());
	}
	public static ScriptProcessorHolder getInstance() {
		return instance;
	}

	public void addScriptProcessor(ScriptProcessor scriptProcessor) {
		scriptProcessors.put(scriptProcessor.getExtension(), scriptProcessor);
	}

	public ScriptProcessor getScriptProcessor(String extension) {
		return scriptProcessors.get(extension);
	}

	public String[] getScriptExtensions() {
		ArrayList<String> scriptExtensions = new ArrayList<String>();
		scriptExtensions.add("igb");
		scriptExtensions.addAll(scriptProcessors.keySet());
		return scriptExtensions.toArray(new String[scriptExtensions.size()]);
	}
}
