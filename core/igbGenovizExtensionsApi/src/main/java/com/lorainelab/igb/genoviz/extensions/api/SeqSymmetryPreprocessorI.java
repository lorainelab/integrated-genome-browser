package com.lorainelab.igb.genoviz.extensions.api;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.FileTypeCategory;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.RootSeqSymmetry;

/**
 *
 * @author dcnorris
 */
public interface SeqSymmetryPreprocessorI {

    public String getName();
    
    public FileTypeCategory getCategory();

    public void process(RootSeqSymmetry sym, ITrackStyleExtended style, SeqMapViewExtendedI gviewer, BioSeq seq);

}
