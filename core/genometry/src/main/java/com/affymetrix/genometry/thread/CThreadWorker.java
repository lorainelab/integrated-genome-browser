package com.affymetrix.genometry.thread;

import com.google.common.collect.Range;
import javax.swing.SwingWorker;
import org.apache.commons.lang3.StringUtils;

/**
 *
 * @author hiralv
 */
public abstract class CThreadWorker<T, V> extends SwingWorker<T, V> {

    private static final boolean DEBUG = false;
    private final String message;
    private final int priority;
    private static final Range VALID_PRIORITY_RANGE = Range.closed(Thread.MIN_PRIORITY, Thread.MAX_PRIORITY);

    public CThreadWorker(String msg) {
        this(msg, Thread.NORM_PRIORITY);
    }

    public CThreadWorker(String message, int priority) {
        super();
        if (StringUtils.isBlank(message)) {
            throw new IllegalArgumentException("Invalid Statusbar Message");
        }
        this.message = message;

        if (!VALID_PRIORITY_RANGE.contains(priority)) {
            throw new IllegalArgumentException("Invalid Thread priority " + priority);
        }
        this.priority = priority;
    }

    public String getMessage() {
        return message;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public final void done() {
        if (DEBUG) {
            System.out.println("))))) Thread " + Thread.currentThread() + " = " + getMessage() + " done");
        }
        finished();
        //CThreadHolder.getInstance().notifyEndThread(this);
    }

    public void setProgressAsPercent(double percent) {
        if (percent > 1.0) {
            percent = 1.0;
        }
        if (percent < 0.0) {
            percent = 0.0;
        }
        setProgress((int) (percent * 100.0));
    }

    @Override
    protected final T doInBackground() throws Exception {
        CThreadHolder.getInstance().notifyStartThread(this);
        if (DEBUG) {
            System.out.println("))))) Thread " + Thread.currentThread() + " = " + getMessage() + " started");
        }
        T t;
        try {
            t = runInBackground();
        } catch (Exception x) {
            throw (x);
        }
        if (DEBUG) {
            System.out.println("))))) Thread " + Thread.currentThread() + " = " + getMessage() + " background done");
        }
        CThreadHolder.getInstance().notifyEndThread(this);
        //CThreadHolder.getInstance().notifyBackgroundDone(this);
        return t;
    }

    protected abstract T runInBackground();

    protected abstract void finished();

    protected boolean showCancelConfirmation() {
        return true;
    }

    public boolean cancelThread(boolean b) {
        boolean confirm = showCancelConfirmation();
        if (confirm) {
            this.cancel(b);
        }
        return confirm;
    }
}
