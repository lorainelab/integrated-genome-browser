package com.affymetrix.genometryImpl.symmetry.impl;

/**
 *
 * @author hiralv
 */
public interface SymWithBaseQuality extends SymWithResidues {

    public String getBaseQuality();

    public String getBaseQuality(int start, int end);

    public int getAverageQuality();
}
