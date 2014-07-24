/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometryImpl.symmetry;

import com.affymetrix.genometryImpl.BioSeq;
import net.sf.samtools.Cigar;

/**
 *
 * @author dcnorris
 */
public class PairedBamSym extends BAMSym {

    public PairedBamSym(String type, BioSeq seq, int txMin, int txMax, String name, boolean forward, int[] blockMins, int[] blockMaxs, int[] iblockMins, int[] iblockMaxs, Cigar cigar, String residues) {
        super(type, seq, txMin, txMax, name, forward, blockMins, blockMaxs, iblockMins, iblockMaxs, cigar, residues);
    }

}
