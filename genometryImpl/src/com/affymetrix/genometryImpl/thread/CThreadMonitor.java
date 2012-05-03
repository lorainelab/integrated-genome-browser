package com.affymetrix.genometryImpl.thread;

/**
 *
 * @author hiralv
 */
public abstract class CThreadMonitor implements CThreadListener {

	final private java.util.List<CThreadWorker<?,?>> workers;

	public CThreadMonitor(java.util.List<CThreadWorker<?,?>> workers) {
		this.workers = workers;
		addListeners();
	}

	public void heardThreadEvent(CThreadEvent cte) {
		if (allFinished()) {
			workers.clear();
			done();
		}
	}

	public abstract void done();

	private void addListeners() {
		for (CThreadWorker<?,?> worker : workers) {
			worker.addThreadListener(this);
		}
		heardThreadEvent(null);
	}

	private boolean allFinished() {
		for (CThreadWorker<?,?> worker : workers) {
			if (!worker.isDone()) {
				return false;
			}
		}
		return true;
	}
}
