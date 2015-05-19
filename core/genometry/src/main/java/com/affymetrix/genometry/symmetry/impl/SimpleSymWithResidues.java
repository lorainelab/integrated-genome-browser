package com.affymetrix.genometry.symmetry.impl;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.symmetry.SymWithResidues;
import java.util.BitSet;
import java.util.Optional;

/**
 *
 * @author hiralv
 */
public class SimpleSymWithResidues extends UcscBedSym implements SymWithResidues {

    private final String residues;
    private BitSet residueMask;

    public SimpleSymWithResidues(String type, BioSeq seq, int txMin, int txMax, String name, float score,
            boolean forward, int cdsMin, int cdsMax, int[] blockMins, int[] blockMaxs, Optional<StringBuilder> residues) {
        this(type, seq, txMin, txMax, name, score, forward, cdsMin, cdsMax, blockMins, blockMaxs, residues.isPresent() ? residues.get().toString() : "");
    }

    public SimpleSymWithResidues(String type, BioSeq seq, int txMin, int txMax, String name, float score,
            boolean forward, int cdsMin, int cdsMax, int[] blockMins, int[] blockMaxs, String residues) {
        super(type, seq, txMin, txMax, name, score, forward, cdsMin, cdsMax, blockMins, blockMaxs);
        this.residues = residues;
    }

    public String getResidues() {
        return residues;
    }

    public BitSet getResidueMask() {
        return residueMask;
    }

    public void setResidueMask(BitSet bitset) {
        this.residueMask = bitset;
    }

    public String getResidues(int start, int end) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
