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
	private final Set<CThreadWorker<?,?>> RUNNING_CTHREADWORKERS = new HashSet<CThreadWorker<?,?>>();
	private static final CThreadWorker<?,?> NOOP = new CThreadWorker<Void,Void>("noop") {
		@Override protected Void runInBackground() { return null; }
		@Override protected void finished() {}
	};
	private final Map<Thread, CThreadWorker<?,?>> thread2CThreadWorker = new HashMap<Thread, CThreadWorker<?,?>>();
	private static CThreadHolder singleton;
	private final Set<CThreadListener> listeners;
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
			CThreadWorker<?,?> currentCThreadWorker = thread2CThreadWorker.get(Thread.currentThread());
			if (currentCThreadWorker == null) { // to prevent NPE in test programs
				currentCThreadWorker = NOOP;
			}
			return currentCThreadWorker;
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
		if (DEBUG) System.out.println("))))) notifyStartThread CThreadWorker = " + worker.getMessage());
		RUNNING_CTHREADWORKERS.add(worker);
		if (DEBUG) System.out.println("))))) notifyBackgroundDone RUNNING_CTHREADWORKERS.add Thread = " + Thread.currentThread() + "=" + worker.getMessage());
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
		if (DEBUG) System.out.println("))))) notifyBackgroundDone CThreadWorker = " + worker.getMessage());
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
				if (DEBUG) System.out.println("))))) notifyBackgroundDone thread2CThreadWorker.remove Thread = " + Thread.currentThread() + "=" + thread2CThreadWorker.get(Thread.currentThread()).getMessage());
				thread2CThreadWorker.remove(thread);
			}
		}
//		notifyEndThread(worker);
	}

	public void notifyEndThread (CThreadWorker<?,?> worker){
		fireThreadEvent(worker, CThreadEvent.ENDED);
		synchronized (RUNNING_CTHREADWORKERS) {
			if (DEBUG) System.out.println("))))) notifyBackgroundDone RUNNING_CTHREADWORKERS.remove Thread = " + Thread.currentThread() + "=" + worker.getMessage());
			RUNNING_CTHREADWORKERS.remove(worker);
			if (RUNNING_CTHREADWORKERS.size() == 0 && threadLatch != null) {
				ThreadUtils.runOnEventQueue(
					new Runnable() {
						@Override
						public void run() {
							if (DEBUG) System.out.println("))))) notifyBackgroundDone countDown Thread = " + Thread.currentThread());
							threadLatch.countDown();
							if (DEBUG) System.out.println("))))) notifyBackgroundDone countDown returned, Thread = " + Thread.currentThread());
						}
					}
				);
			}
		}
	}

	private void fireThreadEvent(CThreadWorker<?,?> worker, int state){		
		CThreadEvent event = new CThreadEvent(worker, state);
		for(CThreadListener listener : listeners){
			listener.heardThreadEvent(event);
		}
	}

	public Boolean waitForAll() {
		if (DEBUG) System.out.println("))))) waitForAll Thread = " + Thread.currentThread());
		
		synchronized (RUNNING_CTHREADWORKERS) {
			if (DEBUG) System.out.println("))))) RUNNING_CTHREADWORKERS.size() = " + RUNNING_CTHREADWORKERS.size());
			if (DEBUG) {
				for (CThreadWorker<?, ?> worker : RUNNING_CTHREADWORKERS) {
					System.out.println("))))) worker = " + worker.getMessage());
				}
			}
			if (RUNNING_CTHREADWORKERS.size() == 0){
				if (DEBUG) System.out.println("))))) no active thread.");
				return Boolean.TRUE;
			}
			if (threadLatch == null || threadLatch.getCount() == 0) {
				threadLatch = new CountDownLatch(1);
			}
		}
		try {
			if (DEBUG) System.out.println("))))) waitForAll await() Thread = " + Thread.currentThread());
			threadLatch.await();
			if (DEBUG) System.out.println("))))) waitForAll await() returned, Thread = " + Thread.currentThread());
		} catch (InterruptedException x) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Script getWaitHelper().run() interrupted", x);
		}
		if (DEBUG) System.out.println("))))) waitForAll returned, Thread = " + Thread.currentThread());
		return Boolean.TRUE;
	}
}
