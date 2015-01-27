/**
 * Copyright (c) 2001-2004 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.genometry;

/**
 * Implementations model a contiguous section of a {@link BioSeq}.
 */
public interface SeqSpan {

    public int getStart();

    public int getEnd();

    public int getMin();

    public int getMax();

    public int getLength();

    public boolean isForward();

    public BioSeq getBioSeq();

    public double getStartDouble();

    public double getEndDouble();

    public double getMaxDouble();

    public double getMinDouble();

    public double getLengthDouble();

    public boolean isIntegral();
}
