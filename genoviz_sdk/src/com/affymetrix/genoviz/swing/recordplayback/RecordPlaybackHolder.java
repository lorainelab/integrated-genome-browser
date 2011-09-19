package com.affymetrix.genoviz.swing.recordplayback;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.swing.JOptionPane;

public class RecordPlaybackHolder {
	private static final RecordPlaybackHolder instance = new RecordPlaybackHolder();
	private static final String IMPORT = "from com.affymetrix.genoviz.swing.recordplayback import RecordPlaybackHolder\n";
	private static final String ASSIGN = "rph = RecordPlaybackHolder.getInstance()\n";
	private List<Operation> operations = new ArrayList<Operation>();
	private Map<String, JRPWidget> widgets = new HashMap<String, JRPWidget>();
	public static RecordPlaybackHolder getInstance() {
		return instance;
	}
	private RecordPlaybackHolder() {
		super();
	}
	public void addWidget(JRPWidget widget) {
		if (widgets.get(widget.getId()) != null) {
//			Logger.getLogger(getClass().getName()).log(Level.WARNING, "duplicate id for widget " + widget.getId());
		}
		widgets.put(widget.getId(), widget);
	}
	public void removeWidget(String id) {
		widgets.remove(id);
	}
	public String getScript() {
		Set<String> namedIds = new HashSet<String>();
		StringBuffer sb = new StringBuffer();
		sb.append(IMPORT);
		sb.append(ASSIGN);
		for (Operation operation : operations) {
			String id = operation.getId();
			if (!namedIds.contains(id)) {
				sb.append(id + " = rph.getWidget(\"" + id + "\")\n");
				namedIds.add(id);
			}
			sb.append(operation.toString() + "\n");
		}
		return sb.toString();
	}
	public JRPWidget getWidget(String id) {
		return widgets.get(id);
	}

	public void recordOperation(Operation operation) {
		if (!operation.getWidget().consecutiveOK()) {
			int lastIndex = operations.size() - 1;
			if (lastIndex >= 0) {
				Operation lastOperation = operations.get(lastIndex);
				if (operation.getId().equals(lastOperation.getId())) {
					operations.set(lastIndex, operation);
					return;
				}
			}
		}
		operations.add(operation);
	}

	public void runScript(String fileName) {
//		PythonInterpreter interp = new PythonInterpreter();
//		interp.execfile(fileName);
		ScriptEngineManager engineMgr = new ScriptEngineManager();
		int pos = fileName.lastIndexOf('.');
		if (pos == -1) {
			return;
		}
		String extension = fileName.substring(pos + 1);
		ScriptEngine engine = engineMgr.getEngineByExtension(extension);
		try {
			InputStream is = new FileInputStream(fileName);
			Reader reader = new InputStreamReader(is);
			engine.eval(reader);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void pause() {
		JOptionPane.showMessageDialog(null, "script paused ...");
	}
}
