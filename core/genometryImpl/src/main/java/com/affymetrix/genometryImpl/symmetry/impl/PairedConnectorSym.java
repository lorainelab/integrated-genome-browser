/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometryImpl.symmetry.impl;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symmetry.BasicSeqSymmetry;

/**
 *
 * @author dcnorris
 */
public class PairedConnectorSym extends BasicSeqSymmetry {

    public PairedConnectorSym(String type, BioSeq seq, int txMin, int txMax, String name, boolean forward, int[] blockMins, int[] blockMaxs) {
        super(type, seq, txMin, txMax, name, forward, blockMins, blockMaxs);
    }

    @Override
    public SeqSymmetry getChild(int index) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
