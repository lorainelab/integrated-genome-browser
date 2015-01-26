package com.affymetrix.igb.service.api;

/**
 * Represents any routine that should be run when IGB starts/exits.
 * The start routines will run when IGB start and frame is made visible.
 * The stop routines will be run only if IGB is exited by the user
 * closing the program (not abnormal shutdown) and will not
 * be run in any specified order.
 */
public interface IWindowRoutine {
	/**
	 * the routine to run at IGB exit.
	 */
	public void stop();
	
	/**
	 * the routine to run when IGB start and frame is made visible.
	 */
	public void start();
}
