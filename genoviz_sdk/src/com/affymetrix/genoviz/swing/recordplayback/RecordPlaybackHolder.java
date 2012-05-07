package com.affymetrix.genoviz.swing.recordplayback;

import java.awt.AWTEvent;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.MouseEvent;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.swing.JOptionPane;

import com.affymetrix.common.ExtensionPointHandler;

public class RecordPlaybackHolder {
	private static final RecordPlaybackHolder instance = new RecordPlaybackHolder();
	private List<Operation> operations = new ArrayList<Operation>();
	private Map<String, JRPWidget> widgets = new HashMap<String, JRPWidget>();
	private Map<String, ScriptEngineFactory> ext2ScriptEngineFactory = new HashMap<String, ScriptEngineFactory>();
	private boolean mouseDown;
	public static RecordPlaybackHolder getInstance() {
		return instance;
	}
	private RecordPlaybackHolder() {
		super();
		mouseDown = false;
		long eventMask = AWTEvent.MOUSE_EVENT_MASK;

		Toolkit.getDefaultToolkit().addAWTEventListener( new AWTEventListener() {
			public void eventDispatched(AWTEvent e) {
				if (e.getID() == MouseEvent.MOUSE_PRESSED) {
					mouseDown = true;
				}
				if (e.getID() == MouseEvent.MOUSE_RELEASED) {
					mouseDown = false;
				}
			}
		}, eventMask);	
	}

	public boolean isMouseDown() {
		return mouseDown;
	}

	public void addWidget(JRPWidget widget) {
		if (widgets.get(widget.getId()) != null) {
//			Logger.getLogger(getClass().getName()).log(Level.WARNING, "duplicate id for widget " + widget.getId());
		}
		widgets.put(widget.getId(), widget);
		if (ExtensionPointHandler.getExtensionPoint(JRPWidgetDecorator.class) != null) {
			for (JRPWidgetDecorator decorator : ExtensionPointHandler.getExtensionPoint(JRPWidgetDecorator.class).getExtensionPointImpls()) {
				decorator.widgetAdded(widget);
			}
		}
	}
	public void removeWidget(String id) {
		widgets.remove(id);
	}
	public String getScript(ScriptProcessor scriptManager) {
		StringBuffer sb = new StringBuffer();
		sb.append(scriptManager.getHeader());
		sb.append("\n");
		for (Operation operation : operations) {
			sb.append(scriptManager.getCommand(operation));
			sb.append("\n");
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
		try {
			ScriptEngineManager engineMgr = new ScriptEngineManager();
			int pos = fileName.lastIndexOf('.');
			if (pos == -1) {
				Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "no extension for file " + fileName);
				return;
			}
			String extension = fileName.substring(pos + 1);
			ScriptEngine engine = engineMgr.getEngineByExtension(extension);
			if (engine == null && ext2ScriptEngineFactory.get(extension) != null) {
				engine = ext2ScriptEngineFactory.get(extension).getScriptEngine();
			}
			if (engine == null) {
				Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "engine is null for extension " + extension);
				return;
			}
			InputStream is = new FileInputStream(fileName);
			Reader reader = new InputStreamReader(is);
			engine.eval(reader);
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public void pause() {
		JOptionPane.showMessageDialog(null, "script paused ...");
	}

	public synchronized void addDecorator(JRPWidgetDecorator decorator) {
		for (JRPWidget widget : widgets.values()) {
			decorator.widgetAdded(widget);
		}
	}

	public void addScriptEngineFactory(List<String> extensions, ScriptEngineFactory scriptEngineFactory) {
		for (String extension : extensions) {
			ext2ScriptEngineFactory.put(extension, scriptEngineFactory);
		}
	}
}
