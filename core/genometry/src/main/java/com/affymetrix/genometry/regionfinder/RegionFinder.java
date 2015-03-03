package com.affymetrix.genometry.regionfinder;

import com.affymetrix.genometry.BioSeq;
import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.util.List;

/**
 *
 * @author hiralv
 */
public interface RegionFinder {

    public SeqSpan findInterestingRegion(BioSeq aseq, List<SeqSymmetry> syms);
}
