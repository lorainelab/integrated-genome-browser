package com.affymetrix.genometryImpl.thread;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.affymetrix.genometryImpl.util.ThreadUtils;

public class CThreadHolder {
	private static final boolean DEBUG = false;
	private final Set<CThreadWorker<?,?>> RUNNING_CTHREADWORKERS = new HashSet<CThreadWorker<?,?>>();
	private static final CThreadWorker<?,?> NOOP = new CThreadWorker<Void,Void>("noop") {
		@Override protected Void runInBackground() { return null; }
		@Override protected void finished() {}
	};
	private static CThreadHolder singleton;
	private final Set<CThreadListener> listeners;
	
	public static CThreadHolder getInstance(){
		if(singleton == null){
			singleton = new CThreadHolder();
		}
		return singleton;
	}
	
	private CThreadHolder(){
		super();
		listeners = new HashSet<CThreadListener>();
	}

	public void cancelAllTasks() {
		synchronized (RUNNING_CTHREADWORKERS) {
			for (final CThreadWorker<?, ?> worker : RUNNING_CTHREADWORKERS) {
				if (worker != null && !worker.isCancelled() && !worker.isDone()) {
					worker.cancelThread(true);
				}
			}
		}
	}
	
	public void execute(Object obj, CThreadWorker<?,?> worker){
		if(obj == null || worker == null){
			throw new IllegalArgumentException("None of parameters can be null");
		}
		ThreadUtils.getPrimaryExecutor(obj).execute(worker);
	}

	public void addListener(CThreadListener listener) {
		listeners.add(listener);
	}

	public CThreadWorker<?,?> getCurrentCThreadWorker() {
		return NOOP;
	}

	public Set<CThreadWorker<?,?>> getAllCThreadWorkers() {
		synchronized(RUNNING_CTHREADWORKERS) {
			return new CopyOnWriteArraySet<CThreadWorker<?,?>>(RUNNING_CTHREADWORKERS);
		}
	}

	public int getCThreadWorkerCount() {
		synchronized(RUNNING_CTHREADWORKERS) {
			return RUNNING_CTHREADWORKERS.size();
		}
	}

	public void notifyStartThread(CThreadWorker<?,?> worker) {
		if (DEBUG) {
			System.out.println("))))) notifyStartThread CThreadWorker = " + worker.getMessage());
		}
		synchronized (RUNNING_CTHREADWORKERS) {
			RUNNING_CTHREADWORKERS.add(worker);
			fireThreadEvent(worker, CThreadEvent.STARTED);
		}
		if (DEBUG) {
			System.out.println("))))) notifyBackgroundDone RUNNING_CTHREADWORKERS.add Thread = " + Thread.currentThread() + "=" + worker.getMessage());
		}
	}

	public void notifyEndThread (CThreadWorker<?,?> worker){
		synchronized (RUNNING_CTHREADWORKERS) {
			if (DEBUG) {
				System.out.println("))))) notifyBackgroundDone RUNNING_CTHREADWORKERS.remove Thread = " + Thread.currentThread() + "=" + worker.getMessage());
			}
			RUNNING_CTHREADWORKERS.remove(worker);
			fireThreadEvent(worker, CThreadEvent.ENDED);
		}
	}

	private void fireThreadEvent(CThreadWorker<?,?> worker, int state){		
		CThreadEvent event = new CThreadEvent(worker, state);
		for(CThreadListener listener : listeners){
			listener.heardThreadEvent(event);
		}
	}
}
