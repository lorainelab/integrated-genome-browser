package com.affymetrix.igb.thread;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.swing.SwingWorker;

/**
 *
 * @author hiralv
 */
public abstract class CThreadWorker extends SwingWorker{
	
	private Set<CThreadListener> listeners= new CopyOnWriteArraySet<CThreadListener>();
	
	final private String message;
	
	public CThreadWorker(String msg){
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
	
	@Override
	protected final Object doInBackground() throws Exception {
		fireThreadEvent(this, CThreadEvent.STARTED);
		return runInBackground();
	}
	
	protected abstract Object runInBackground();
	
	protected abstract void finished();

	public void addThreadListener(CThreadListener listener){
		listeners.add(listener);
	}
	
	private void fireThreadEvent(CThreadWorker worker, int state){		
		CThreadEvent event = new CThreadEvent(worker, state);
		for(CThreadListener listener : listeners){
			listener.heardThreadEvent(event);
		}
	}

}
