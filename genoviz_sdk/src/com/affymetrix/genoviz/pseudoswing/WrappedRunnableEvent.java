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

package com.affymetrix.genoviz.pseudoswing;

import java.util.EventObject;

public class WrappedRunnableEvent extends EventObject
 implements Runnable {

  // id doesn't really matter, since only one id for this type of event
   //  protected static int WRAPPED = 30005;  // jdk1.1

  protected static Object dummy_source = new Object();
  protected Runnable the_runnable;
  protected Object lock;
  protected Exception problem = null;

  public WrappedRunnableEvent(Runnable the_runnable, Object lock) {
    // super(dummy_source, WRAPPED); // jdk1.1
    super(dummy_source); // jdk1.0
    this.the_runnable = the_runnable;
    this.lock = lock;
  }

  /**
   * calls the_runnable.run().  If there's a lock
   * then need to synchronize the run() call and save any resulting
   * exception so it can be retrieved with getException().
   */
  public void run() {
    // if there's no lock, then don't need to worry about synchronizing
    if (lock == null) {
      the_runnable.run();
    }
    else {
      synchronized(lock) {
        try {
          the_runnable.run();
        }
        catch (Exception e) {
          problem = e;
        }
        finally {
          if (lock != null) {
            lock.notify();
          }
        }
      }
    }
  }

  public Exception getException() {
    return problem;
  }

}
