package com.affymetrix.genometryImpl.thread;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.affymetrix.genometryImpl.util.ThreadUtils;

public class CThreadHolder {
	private static CThreadHolder singleton;
	private final Set<CThreadWorker<?,?>> workers;
	private final Set<CThreadListener> listeners;
	
	public static CThreadHolder getInstance(){
		if(singleton == null){
			singleton = new CThreadHolder();
		}
		return singleton;
	}
	
	private CThreadHolder(){
		super();
		workers = new LinkedHashSet<CThreadWorker<?,?>>();
		listeners = new HashSet<CThreadListener>();
	}

	public void cancelAllTasks() {
		for (final CThreadWorker<?,?> worker : new CopyOnWriteArraySet<CThreadWorker<?,?>>(workers)) {
			if(worker != null && !worker.isCancelled() && !worker.isDone()){
				worker.cancelThread(true);
			}
		}		
	}
	
	public void execute(Object obj, CThreadWorker<?,?> worker){
		if(obj == null || worker == null){
			throw new IllegalArgumentException("None of parameters can be null");
		}
		for (CThreadListener listener : listeners) {
			worker.addThreadListener(listener);
		}
		ThreadUtils.getPrimaryExecutor(obj).execute(worker);
	}

	public Set<CThreadWorker<?, ?>> getWorkers() {
		return workers;
	}

	public void addListener(CThreadListener listener) {
		listeners.add(listener);
	}
}
