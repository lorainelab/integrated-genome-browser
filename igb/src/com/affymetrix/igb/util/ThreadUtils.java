package com.affymetrix.igb.util;

import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public final class ThreadUtils {
  static Map obj2exec = new HashMap();

  /**
   *   Gets the primary executor for a given object key
   *   Creates a new exector if didn't exist before
   *   Currently returns  an Executor that uses a single worker thread operating off an unbounded queue
   *      therefore tasks (Runnables added via exec.execute()) on the Executor are guaranteed to 
   *      execute sequentially in order, and no more than one task will be active at any given time
   */
  public synchronized static Executor getPrimaryExecutor(Object key) {
    Executor exec = (Executor)obj2exec.get(key);
    if (exec == null) {
      exec = Executors.newSingleThreadExecutor();
      obj2exec.put(key, exec);
    }
    return exec;
  }
}
