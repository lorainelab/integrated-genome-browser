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

package com.affymetrix.genometryImpl.util;
import java.text.NumberFormat;

/**
 * A little utility class to help monitor memory usage.
 */
public final class Memer {

	Runtime rt;
	long prevCheck, currCheck;
	long currFreeMem, currTotalMem, currMemUsed, deltaMemUsed,
		 prevFreeMem, prevTotalMem, prevMemUsed, maxMem;
	NumberFormat nf = NumberFormat.getIntegerInstance();

	public Memer() {
		rt = Runtime.getRuntime();
		currFreeMem = currTotalMem = currMemUsed = deltaMemUsed = 0;
		prevFreeMem = prevTotalMem = prevMemUsed = 0;
		calcMem();
	}

	/**
	 * Checks memory and prints a message if the total memory usage has changed
	 * since the last time this was called.
	 */
	/*public void checkMemory() {
		currCheck = rt.totalMemory();
		if (prevCheck != currCheck) {
			printMemory();
			prevCheck = currCheck;
		}
	}*/

	/**
	 * Refreshes this object's data from the Runtime.
	 */
	public void calcMem() {
		currFreeMem = rt.freeMemory();
		currTotalMem = rt.totalMemory();
		maxMem = rt.maxMemory();
		currMemUsed = currTotalMem - currFreeMem;
		deltaMemUsed = currMemUsed - prevMemUsed;
		prevFreeMem = currFreeMem;
		prevTotalMem = currTotalMem;
		prevMemUsed = currMemUsed;
	}

	/**
	 * Returns the change in memory usage
	 * between the last two calls to calcMem.
	 *
	 * @return the most recent change in memory usage.
	 * @see #calcMem
	 */
	public long getLastDelta() {
		return deltaMemUsed;
	}

	/**
	 * Prints this object's data to the given stream.
	 */
	public void printMemory(java.io.PrintStream str) {
		str.println(this.toString());
	}

	/**
	 * Prints this object's data to stderr.
	 */
	public void printMemory() {
		printMemory(System.err);
	}

	/**
	 * @return a string representation of this object.
	 */
	@Override
		public String toString() {
			calcMem();
			return " free: " + nf.format(currFreeMem) +
				"   total: " + nf.format(currTotalMem) +
				"   used: " + nf.format(currMemUsed) +
				"   delta: " + nf.format(deltaMemUsed) +
				"   max: " + nf.format(maxMem);
		}

}
