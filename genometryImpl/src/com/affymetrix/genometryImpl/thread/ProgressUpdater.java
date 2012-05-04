package com.affymetrix.genometryImpl.thread;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.affymetrix.genometryImpl.thread.CThreadWorker;

public class ProgressUpdater {
	private static final int NUM_THREADS = 1;
	private static final boolean DONT_INTERRUPT_IF_RUNNING = false;
	private static final int SECONDS_BETWEEN_UPDATE = 2;
	private ScheduledFuture<?> progressUpdateFuture;
	private final long startPosition;
	private final long endPosition;
	private final PositionCalculator positionCalculator;
	private static final class ProgressUpdateTask implements Runnable {
		private final ProgressUpdater progressUpdater;
		private final CThreadWorker<?,?> ctw;
		private ProgressUpdateTask(ProgressUpdater progressUpdater) {
			super();
			this.progressUpdater = progressUpdater;
			ctw = CThreadWorker.getCurrentCThreadWorker();
		}
		public void run() {
			double progress = (double)(progressUpdater.getPositionCalculator().getCurrentPosition() - progressUpdater.getStartPosition()) / (double)(progressUpdater.getEndPosition() - progressUpdater.getStartPosition());
			ctw.setProgressAsPercent(progress);
		}
	}

	public ProgressUpdater(long startPosition, long endPosition, PositionCalculator positionCalculator) {
		super();
		this.startPosition = startPosition;
		this.endPosition = endPosition;
		this.positionCalculator = positionCalculator;
	}

	public long getStartPosition() {
		return startPosition;
	}

	public long getEndPosition() {
		return endPosition;
	}

	public PositionCalculator getPositionCalculator() {
		return positionCalculator;
	}

	public void start() {
		CThreadWorker<?,?> ctw = CThreadWorker.getCurrentCThreadWorker();
		if (ctw != null) {
			ScheduledExecutorService fScheduler = Executors.newScheduledThreadPool(NUM_THREADS);
			Runnable progressUpdateTask = new ProgressUpdateTask(this);
		    progressUpdateFuture = fScheduler.scheduleWithFixedDelay(
		    	progressUpdateTask, 0, SECONDS_BETWEEN_UPDATE, TimeUnit.SECONDS
		    );
		}
	}

	public void kill() {
		if (progressUpdateFuture != null) {
			progressUpdateFuture.cancel(DONT_INTERRUPT_IF_RUNNING);
		}
	}
}
