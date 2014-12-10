package com.affymetrix.main;

import com.affymetrix.common.CommonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
/**
 * Main class for the Integrated Genome Browser (IGB, pronounced ig-bee).
 *
 */
public final class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    /**
     * Start the program. Nothing to it, just start OSGi and all the bundles.
     */
    public static void main(final String[] args) {
        log.info("Starting OSGI container");
        // replacing singleton pattern would be preferred
        CommonUtils commonUtils = CommonUtils.getInstance();
        try {
            OSGiHandler osgiHandler = new OSGiHandler(commonUtils);
            osgiHandler.startOSGi(args);
        } catch (Throwable t) {
            log.error("Error starting osgi runtime container", t);
            System.exit(1);
        }
    }
}
