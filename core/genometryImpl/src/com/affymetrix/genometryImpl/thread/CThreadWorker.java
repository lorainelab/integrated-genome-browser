package com.affymetrix.genometryImpl.thread;

import javax.swing.SwingWorker;

/**
 *
 * @author hiralv
 */
public abstract class CThreadWorker<T,V> extends SwingWorker<T,V>{
	private final String message;
	private final int priority;

	/**
	 * @return if Threads can sleep periodically to share cpu time.
	 * note - this can cause some Threads to take much longer to finish.
	 */
	public static boolean allowThreadSleep() {
		return false;
	}

	public CThreadWorker(String msg){
		this(msg, Thread.NORM_PRIORITY);
	}
	
	public CThreadWorker(String msg, int priority){
		super();
		if(msg == null || msg.length() == 0){
			throw new IllegalArgumentException("Invalid Statusbar Message");
		}
		message = msg;
		if(priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY){
			throw new IllegalArgumentException("Invalid Thread priority " + priority);
		}
		this.priority = priority;
	}
	
	public String getMessage(){
		return message;
	}
	
	public int getPriority() {
		return priority;
	}

	@Override
	public final void done() {
		finished();
		CThreadHolder.getInstance().notifyEndThread(this);
	}

	public void setProgressAsPercent(double percent) {
		if (percent > 1.0) {
			percent = 1.0;
		}
		if (percent < 0.0) {
			percent = 0.0;
		}
		setProgress((int)(percent * 100.0));
	}

	@Override
	protected final T doInBackground() throws Exception {
		CThreadHolder.getInstance().notifyStartThread(this);
		return runInBackground();
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
}
