package com.affymetrix.genometryImpl.thread;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.affymetrix.genometryImpl.util.ThreadUtils;

public class CThreadHolder {
	private final Map<Thread, CThreadWorker<?,?>> thread2CThreadWorker = new HashMap<Thread, CThreadWorker<?,?>>();
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
		for (final CThreadWorker<?,?> worker : getAllCThreadWorkers()) {
			if(worker != null && !worker.isCancelled() && !worker.isDone()){
				worker.cancelThread(true);
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
		synchronized(thread2CThreadWorker) {
			return thread2CThreadWorker.get(Thread.currentThread());
		}
	}

	public Set<CThreadWorker<?,?>> getAllCThreadWorkers() {
		synchronized(thread2CThreadWorker) {
			return new CopyOnWriteArraySet<CThreadWorker<?,?>>(thread2CThreadWorker.values());
		}
	}

	public int getCThreadWorkerCount() {
		synchronized(thread2CThreadWorker) {
			return thread2CThreadWorker.size();
		}
	}

	public void notifyStartThread(CThreadWorker<?,?> worker) {
		synchronized(thread2CThreadWorker) {
			thread2CThreadWorker.put(Thread.currentThread(), worker);
		}
		fireThreadEvent(worker, CThreadEvent.STARTED);
	}

	public void notifyEndThread (CThreadWorker<?,?> worker){
		synchronized(thread2CThreadWorker) {
			Thread thread = null;
			for (Thread threadLoop : thread2CThreadWorker.keySet()) {
				if (worker == thread2CThreadWorker.get(threadLoop)) {
					thread = threadLoop;
					break;
				}
			}
			if (thread == null) {
				Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "could not find thread for " + worker.getMessage());
			}
			else {
				thread2CThreadWorker.remove(thread);
			}
		}
		fireThreadEvent(worker, CThreadEvent.ENDED);
	}

	private void fireThreadEvent(CThreadWorker<?,?> worker, int state){		
		CThreadEvent event = new CThreadEvent(worker, state);
		for(CThreadListener listener : listeners){
			listener.heardThreadEvent(event);
		}
	}
}
