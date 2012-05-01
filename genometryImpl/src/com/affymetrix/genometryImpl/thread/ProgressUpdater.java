package com.affymetrix.genometryImpl.thread;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.affymetrix.genometryImpl.thread.CThreadWorker;

public class ProgressUpdater {
	private static final int NUM_THREADS = 1;
	private static final boolean DONT_INTERRUPT_IF_RUNNING = false;
	private static final int SECONDS_BETWEEN_UPDATE = 3;
	private ScheduledFuture<?> progressUpdateFuture;
	private static final class ProgressUpdateTask implements Runnable {
		private final long startPosition;
		private final long endPosition;
		private final PositionCalculator positionCalculator;
		private final CThreadWorker<?,?> ctw;
		private ProgressUpdateTask(CThreadWorker<?,?> ctw, long startPosition, long endPosition,
				PositionCalculator positionCalculator) {
			super();
			this.startPosition = startPosition;
			this.endPosition = endPosition;
			this.positionCalculator = positionCalculator;
			this.ctw = ctw;
		}
		public void run() {
			double progress = (double)(positionCalculator.getCurrentPosition() - startPosition) / (double)(endPosition - startPosition);
			ctw.setProgressAsPercent(progress);
		}
	}

	public ProgressUpdater(long startPosition, long endPosition, PositionCalculator positionCalculator) {
		super();
		CThreadWorker<?,?> ctw = CThreadWorker.getCurrentCThreadWorker();
		if (ctw != null) {
			ScheduledExecutorService fScheduler = Executors.newScheduledThreadPool(NUM_THREADS);
			Runnable progressUpdateTask = new ProgressUpdateTask(ctw, startPosition, endPosition, positionCalculator);
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
