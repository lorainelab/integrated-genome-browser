/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
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

package com.affymetrix.genometryImpl;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class SingletonGenometryModel extends GenometryModel {

	static SingletonGenometryModel smodel = new SingletonGenometryModel();

	/** Constructor is protected to allow for subclassing. */
	protected SingletonGenometryModel() {
		super();
	}

	public static SingletonGenometryModel getGenometryModel() {
		return smodel;
	}

	static Logger default_logger = Logger.getLogger(SingletonGenometryModel.class.getName());
	static Logger logger = default_logger;
	public static Logger getLogger() {
		return logger;
	}

	public static void setLogger(Logger log) {
		logger = log;
		if (logger == null) {
			logger = default_logger;
		}
	}

	public static void logError(String msg) {
		getLogger().log(Level.SEVERE, msg);
	}

	public static void logWarning(String msg) {
		getLogger().log(Level.WARNING, msg);
	}

	public static void logInfo(String msg) {
		getLogger().log(Level.INFO, msg);
	}

	public static void logDebug(String msg) {
		getLogger().log(Level.FINE, msg);
	}
}
