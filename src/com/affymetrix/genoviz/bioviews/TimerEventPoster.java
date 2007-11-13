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

// when a TimerEventPoster is created (in the
//   NeoTimerEventClock.postTimeEventLater() method), eventually its run()
// is called via
// invokeLater(), which forces posting of timer events to occur on
//   EventDispatchThread synchronously with event handling and paint calls
//
// Note that this is _not_ implementing Runnable to be run on its own
//   thread.  run() should be called and executes on the event dispatch
//   thread.  Runnable is just a convenient interface that was chosen
//   as the interface chosen for the object passed to
//   invokeLater(), to be used as a callback to the object

public class TimerEventPoster implements Runnable {
  NeoTimerEventClock clock;
  int cnt;
  TimerEventPoster(NeoTimerEventClock c, int n) {
    clock = c;
    cnt = n;
  }
  public void run() {
    clock.postTimerEvent(clock, cnt);
  }
}
