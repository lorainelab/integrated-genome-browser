
package com.affymetrix.genometryImpl.regionfinder;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.symmetry.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.util.SeqUtils;
import java.util.List;

/**
 *
 * @author hiralv
 */
public class DefaultRegionFinder implements RegionFinder{

	public SeqSpan findInterestingRegion(BioSeq aseq, List<SeqSymmetry> syms) {
		MutableSeqSymmetry resultSym = new SimpleMutableSeqSymmetry();
		SeqUtils.union(syms, resultSym, aseq);
		return resultSym.getSpan(aseq);
	}
	
}
