/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometry.symmetry.impl;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.Scored;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.SupportsCdsSpan;
import com.affymetrix.genometry.symmetry.BasicSeqSymmetry;
import com.affymetrix.genometry.symmetry.SymSpanWithCds;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

/**
 *
 * @author dcnorris
 */
public class NarrowPeakSym extends BasicSeqSymmetry implements SupportsCdsSpan, SymSpanWithCds, Scored {

    UcscBedSym bedSym;

    public NarrowPeakSym(String trackUri, BioSeq seq, int txMin, int txMax, String name, float score, boolean forward, int cdsMin, int cdsMax, int[] blockMins, int[] blockMaxs) {
        super(trackUri, seq, txMin, txMax, name, forward, blockMins, blockMaxs);
        bedSym = new UcscBedSym(trackUri, seq, txMin, txMax, name, score, forward, cdsMin, cdsMax, blockMins, blockMaxs);
    }

    @Override
    public SeqSymmetry getChild(int index) {
        return bedSym.getChild(index);
    }

    @Override
    public boolean hasCdsSpan() {
        return bedSym.hasCdsSpan();
    }

    @Override
    public SeqSpan getCdsSpan() {
        return bedSym.getCdsSpan();
    }

    @Override
    public boolean isCdsStartStopSame() {
        return bedSym.isCdsStartStopSame();
    }

    @Override
    public float getScore() {
        return bedSym.getScore();
    }
    
     @Override
    public Map<String, Object> getProperties() {
        return ImmutableMap.copyOf(props);
    }

}
