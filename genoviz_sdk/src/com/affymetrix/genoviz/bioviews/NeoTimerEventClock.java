/**
 *   Copyright (c) 1998-2005 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */

package com.affymetrix.genoviz.bioviews;

import java.util.Vector;

import com.affymetrix.genoviz.event.NeoTimerEvent;
import com.affymetrix.genoviz.event.NeoTimerListener;

/**
 *  A timer thread that generates NeoTimerEvents at specified intervals.
 */
public class NeoTimerEventClock extends Thread {

	// interval between generation of events ("ticks"), in milliseconds
	protected int time_interval = 50;

	// interval between call to run and first "tick", in milliseconds
	protected int initial_interval = time_interval;

	// number of clock "ticks" since thread.run() was called
	protected int count = 0;

	/** an arbitrary object to include in generated NeoTimerEvents */
	protected Object arg = null;

	// probably should also have a time field in milliseconds, either
	//   absolute, relative, or both...

	// standard listener list for events
	protected Vector<NeoTimerListener> listeners = new Vector<NeoTimerListener>();

	public NeoTimerEventClock(int time_interval) {
		this.time_interval = time_interval;
		this.initial_interval = time_interval;
	}

	public NeoTimerEventClock(int initial_interval, int time_interval) {
		this.time_interval = time_interval;
		this.initial_interval = initial_interval;
	}

	public NeoTimerEventClock(int initial_interval,
			int time_interval, Object arg) {
		this.time_interval = time_interval;
		this.initial_interval = initial_interval;
		this.arg = arg;
	}

	public void run() {
		try {
			sleep(initial_interval);
		}
		catch (Exception ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}
		// GAH 12-2-98
		// incrementing count here and using it in postTimerEvent() can give
		// rise to race condition if lazy_event_posting.  Not that it matters,
		// since event tick count isn't really being used, but should deal with
		// it.  Easy way is to just get rid of count.  But it's indicative of
		// a less obvious problem, and better way is probably to correctly
		// synchronize run/post/etc. in NeoTimerEventClock
		count++;
		postTimerEventLater();

		while (true) {
			try {
				// should probably replace this with wait(interval),
				// and also synchronized more methods!
				sleep(time_interval);
			}
			catch (Exception ex) {
				System.out.println(ex.getMessage());
				ex.printStackTrace();
			}
			// see above -- count increment and lazy_event_posting can combine
			// to give race conditions (though for count it doesn't really matter)
			count++;
			postTimerEventLater();
		}
	}

	public void postTimerEvent() {
		postTimerEvent(this, count);
	}

	/**
	 * If lazy timing is turned on, this method is called
	 *   synchronously with AWT events and paints, on the event dispatch thread
	 * Rather than calling this method directly, lazy timing works by setting
	 *   up a TimerEventPoster object, which in turn invokes this method
	 *   when its turn comes up on the event dispatch queue
	 */
	public void postTimerEvent(Object src, int cnt) {
		int num_listeners = listeners.size();
		if (num_listeners <= 0) {
			return;
		}
		NeoTimerEvent nte = new NeoTimerEvent(src, arg, count);
		NeoTimerListener listener;
		for (int i=0; i<listeners.size(); i++) {
			listener = listeners.elementAt(0);
			listener.heardTimerEvent(nte);
		}
	}

	// using pseudoswing-style notifyLater() to make
	//   listener calls synchronous with paint/event handling
	public void postTimerEventLater() {
		TimerEventPoster poster = new TimerEventPoster(this, count);
		javax.swing.SwingUtilities.invokeLater(poster);
	}

	public void addTimerListener(NeoTimerListener ntl) {
		listeners.addElement(ntl);
	}

	public void removeTimerListener(NeoTimerListener ntl) {
		listeners.removeElement(ntl);
	}

	public Vector getTimerListeners() {
		return listeners;
	}
}
