package com.affymetrix.igb.osgi.service;

/**
 * Represents any routine that should be run when IGB exits.
 * The routines will be run only if IGB is exited by the user
 * closing the program (not abnormal shutdown) and will not
 * be run in any specified order.
 */
public interface IStopRoutine {
	/**
	 * the routine to run at IGB exit.
	 */
	public void stop();
}
