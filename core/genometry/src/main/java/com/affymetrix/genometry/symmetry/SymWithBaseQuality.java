package com.affymetrix.genometry.symmetry;

/**
 *
 * @author hiralv
 */
public interface SymWithBaseQuality extends SymWithResidues {

    public String getBaseQuality();

    public String getBaseQuality(int start, int end);

    public int getAverageQuality();
}
