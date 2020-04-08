/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.genotyping;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.MutableSeqSpan;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.symmetry.BasicSeqSymmetry;
import com.affymetrix.genometry.symmetry.SymSpanWithCds;
import com.affymetrix.genometry.symmetry.SymWithProps;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import com.affymetrix.genometry.symmetry.impl.UcscBedSym;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Represents a polymorphism interrogated on the Affymetrix SNP
 * chip used by 23 and Me. 
 * 
 * Write this similar to BasicSeqSymmetry by Hiral Vora. 
 * The values returned for SeqSpan methods will be the region representing the 
 * genotyping probe - 25 bases, with 12 on either side of the interrogated 
 * SNP position.
 * 
 * On the screen, we will see the 12 bases on either end as separate clickable
 * Glyphs, the polymorphism site as a single clickable Glpyh, all of which
 * will be contained in a larger container glyph area that will display the rs id.
 * 
 * We are going to allow this object to hold properties so that we can decorate
 * the objects with meta-data about the polymorphism and its effects.
 * 
 * @author aloraine
 */
public class TwentyThreeAndMeVariationSym extends BasicSeqSymmetry{
    UcscBedSym bedSym;

    public TwentyThreeAndMeVariationSym(String trackUri, BioSeq seq, int txMin, int txMax, String name, float score, boolean forward, int cdsMin, int cdsMax, int[] blockMins, int[] blockMaxs) {
        super(trackUri, seq, txMin, txMax, name, forward, blockMins, blockMaxs);
        bedSym = new UcscBedSym(trackUri, seq, txMin, txMax, name, score, forward, cdsMin, cdsMax, blockMins, blockMaxs);
    }

    @Override
    public SeqSymmetry getChild(int index) {
        return bedSym.getChild(index);
    }

    @Override
    public Map<String, Object> getProperties() {
        return ImmutableMap.copyOf(props);
    }

}
