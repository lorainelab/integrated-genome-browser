package com.lorainelab.main;

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

    /**
     * Start the program. Nothing to it, just start OSGi and all the bundles.
     */
    public static void main(final String[] args) {
        try {
            //check to see if IGB is already running, if so bring it to the front, if not launch it.
            OSGiHandler.getInstance().startOSGi(args);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }
}
