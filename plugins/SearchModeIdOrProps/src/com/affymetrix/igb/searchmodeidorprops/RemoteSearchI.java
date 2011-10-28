package com.affymetrix.igb.searchmodeidorprops;

import java.util.List;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSymmetry;

public interface RemoteSearchI {
	public List<SeqSymmetry> searchFeatures(AnnotatedSeqGroup group, String name, BioSeq chrFilter);
}
