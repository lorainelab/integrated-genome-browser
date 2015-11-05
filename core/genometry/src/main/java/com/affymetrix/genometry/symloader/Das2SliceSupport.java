/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometry.symloader;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.util.List;

/**
 *
 * @author dcnorris
 */
public interface Das2SliceSupport {

    public List<SeqSymmetry> parseAll(BioSeq seq, String method);
}
