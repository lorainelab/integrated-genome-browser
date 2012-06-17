package com.affymetrix.genometryImpl.thread;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.affymetrix.genometryImpl.util.ThreadUtils;

public class CThreadHolder implements WaitHelperI {
	private static final boolean DEBUG = false;
	private final Map<Thread, CThreadWorker<?,?>> thread2CThreadWorker = new HashMap<Thread, CThreadWorker<?,?>>();
	private static CThreadHolder singleton;
	private final Set<CThreadListener> listeners;
	private Object threadLatchLock = new Object();
	private CountDownLatch threadLatch;
	
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
			if (thread2CThreadWorker.get(Thread.currentThread()) != null) {
				Logger.getLogger(this.getClass().getName()).log(Level.WARNING,
					"Thread " + Thread.currentThread() + " already has " + thread2CThreadWorker.get(Thread.currentThread()) +
					" = " + thread2CThreadWorker.get(Thread.currentThread()).getMessage() + 
					", and is starting " + worker + " = " + worker.getMessage());
			}
			thread2CThreadWorker.put(Thread.currentThread(), worker);
		}
		fireThreadEvent(worker, CThreadEvent.STARTED);
	}

	public void notifyBackgroundDone (CThreadWorker<?,?> worker){
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
				synchronized (threadLatchLock) {
					if (thread2CThreadWorker.size() == 0 && threadLatch != null) {
						if (DEBUG) System.out.println("))))) notifyEndThread countDown Thread = " + Thread.currentThread());
						threadLatch.countDown();
						if (DEBUG) System.out.println("))))) notifyEndThread countDown returned, Thread = " + Thread.currentThread());
					}
				}
			}
		}
	}

	public void notifyEndThread (CThreadWorker<?,?> worker){
		fireThreadEvent(worker, CThreadEvent.ENDED);
	}

	private void fireThreadEvent(CThreadWorker<?,?> worker, int state){		
		CThreadEvent event = new CThreadEvent(worker, state);
		for(CThreadListener listener : listeners){
			listener.heardThreadEvent(event);
		}
	}

	public Boolean waitForAll() {
		if (DEBUG) System.out.println("))))) get wait helper Thread = " + Thread.currentThread());
		if (DEBUG) System.out.println("))))) get wait helper run() Thread = " + Thread.currentThread());
		
		if(getCThreadWorkerCount() == 0){
			if (DEBUG) System.out.println("))))) no active thread.");
			return Boolean.TRUE;
		}
		
		synchronized (threadLatchLock) {
			if (threadLatch == null) {
				threadLatch = new CountDownLatch(1);
			}
		}
		try {
			if (DEBUG) System.out.println("))))) get wait helper await() Thread = " + Thread.currentThread());
			threadLatch.await();
			if (DEBUG) System.out.println("))))) get wait helper await() returned, Thread = " + Thread.currentThread());
		} catch (InterruptedException x) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Script getWaitHelper().run() interrupted", x);
		}
		if (DEBUG) System.out.println("))))) get wait helper run() returned, Thread = " + Thread.currentThread());

		return Boolean.TRUE;
	}
}
