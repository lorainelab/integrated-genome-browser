
package com.affymetrix.genometryImpl.regionfinder;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import java.util.List;

/**
 *
 * @author hiralv
 */
public interface RegionFinder {
	public SeqSpan findInterestingRegion(BioSeq aseq, List<SeqSymmetry> syms);
}
