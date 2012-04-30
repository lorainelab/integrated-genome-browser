package com.affymetrix.genometryImpl.thread;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.swing.SwingWorker;

/**
 *
 * @author hiralv
 */
@SuppressWarnings("rawtypes")
public abstract class CThreadWorker<T,V> extends SwingWorker<T,V>{
	private static Map<Thread, CThreadWorker> thread2CThreadWorker = new HashMap<Thread, CThreadWorker>();
	
	private Set<CThreadListener> listeners= new CopyOnWriteArraySet<CThreadListener>();
	
	final private String message;
	
	public CThreadWorker(String msg){
		super();
		if(msg == null || msg.length() == 0){
			throw new IllegalArgumentException("Invalid Statusbar Message");
		}
		message = msg;
	}
	
	public String getMessage(){
		return message;
	}
	
	@Override
	public final void done() {
		finished();
		fireThreadEvent(this, CThreadEvent.ENDED);
	}

	public void setProgressAsPercent(double percent) {
		if (percent > 1) {
			percent = 1;
		}
		setProgress((int)(percent * 100.0));
	}

	@Override
	protected final T doInBackground() throws Exception {
		thread2CThreadWorker.put(Thread.currentThread(), this);
		fireThreadEvent(this, CThreadEvent.STARTED);
		T t = runInBackground();
		thread2CThreadWorker.remove(Thread.currentThread());
		return t;
	}
	
	protected abstract T runInBackground();
	
	protected abstract void finished();

	protected boolean showCancelConfirmation(){
		return true;
	}
	
	public void cancelThread(boolean b){
		if(!showCancelConfirmation()){
			return;
		}
		this.cancel(b);
	}
	
	public void addThreadListener(CThreadListener listener){
		listeners.add(listener);
	}
	
	private void fireThreadEvent(CThreadWorker worker, int state){		
		CThreadEvent event = new CThreadEvent(worker, state);
		for(CThreadListener listener : listeners){
			listener.heardThreadEvent(event);
		}
	}

	public static CThreadWorker getCurrentCThreadWorker() {
		return thread2CThreadWorker.get(Thread.currentThread());
	}
}
