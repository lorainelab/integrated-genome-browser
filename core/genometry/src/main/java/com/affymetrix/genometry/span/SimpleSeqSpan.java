/**
 * Copyright (c) 2001-2006 Affymetrix, Inc.
 *
 * Licensed under the Common Public License, Version 1.0 (the "License"). A copy
 * of the license must be included with any distribution of this source code.
 * Distributions from Affymetrix, Inc., place this in the IGB_LICENSE.html file.
 *
 * The license is also available at http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.genometry.span;

import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.BioSeq;

public class SimpleSeqSpan implements SeqSpan, Cloneable {

    protected int start;
    protected int end;
    protected BioSeq seq;

    public SimpleSeqSpan(int start, int end, BioSeq seq) {
        this.start = start;
        this.end = end;
        this.seq = seq;
    }

    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    /**
     * Using a "real-number" coordinate representation, such that integer
     * numbers fall <em>between</em> bases. Thus a sequence span covering ACTG
     * would now have for example start = 0, end = 4, with length = 4 (but still
     * designated A = base 0 C = base 1 G = base 2 T = base 3)
     */
    public int getLength() {
        return (end > start ? end - start : start - end);
    }

    public BioSeq getBioSeq() {
        return seq;
    }

    public int getMin() {
        return (start < end ? start : end);
    }

    public int getMax() {
        return (end > start ? end : start);
    }

    public boolean isForward() {
        return (end >= start);
    }

    public double getStartDouble() {
        return start;
    }

    public double getEndDouble() {
        return end;
    }

    public double getMinDouble() {
        return getMin();
    }

    public double getMaxDouble() {
        return getMax();
    }

    public double getLengthDouble() {
        return getLength();
    }

    public boolean isIntegral() {
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + this.start;
        hash = 31 * hash + this.end;
        hash = 31 * hash + (this.seq != null ? this.seq.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SimpleSeqSpan other = (SimpleSeqSpan) obj;
        if (end != other.end) {
            return false;
        }
        if (seq == null) {
            if (other.seq != null) {
                return false;
            }
        } else if (!seq.equals(other.seq)) {
            return false;
        }
        if (start != other.start) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return seq.toString() + ":" + start + "-" + end;
    }
}
