package com.affymetrix.genoviz.swing.recordplayback;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.python.util.PythonInterpreter;

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
		if (widgets.get(widget.getID()) != null) {
			Logger.getLogger(getClass().getName()).log(Level.WARNING, "duplicate id for widget " + widget.getID());
		}
		widgets.put(widget.getID(), widget);
	}
	public String getScript() {
		StringBuffer sb = new StringBuffer();
		sb.append(IMPORT);
		sb.append(ASSIGN);
		for (Operation operation : operations) {
			sb.append(operation.toString() + "\n");
		}
		return sb.toString();
	}
	public void recordOperation(Operation operation) {
		int lastIndex = operations.size() - 1;
		if (lastIndex >= 0) {
			Operation lastOperation = operations.get(lastIndex);
			if (operation.getId().equals(lastOperation.getId())) {
				operations.set(lastIndex, operation);
				return;
			}
		}
		operations.add(operation);
	}
	public void runScript(String fileName) {
		PythonInterpreter interp = new PythonInterpreter();
		interp.execfile(fileName);
	}
	public void execute(final String id, final String... parms) {
		JRPWidget widget = widgets.get(id);
		if (widget == null) {
			Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Record/Playback error, id: " + id + " was not found");
		}
		else {
			widget.execute(parms);
			try {
				Thread.sleep(1000);	// user actions don't happen instantaneously, so give a short sleep time between batch actions.
			}
			catch (InterruptedException x) {}
		}
/*
		(new SwingWorker<Void, Void>() {
			@Override
			protected Void doInBackground() throws Exception {
				widgets.get(id).execute(parms);
				return null;
			}
			
		}).execute();
*/
	}
	public void pause() {
		JOptionPane.showMessageDialog(null, "script paused ...");
	}
}
