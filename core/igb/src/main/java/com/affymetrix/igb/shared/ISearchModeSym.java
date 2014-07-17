package com.affymetrix.igb.shared;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.symmetry.SeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.TypeContainerAnnot;
import java.util.List;

public interface ISearchModeSym extends ISearchMode {

    /**
     * perform the search on all tracks
     *
     * @param search_text the input text
     * @param chrFilter the chromosome / seq to search or null for all
     * @param statusHolder the status display for output messages
     * @param option the value of an option for this mode
     * @return a list of the syms found by the search
     */
    public SearchResults<SeqSymmetry> search(String search_text, final BioSeq chrFilter, IStatus statusHolder, boolean option);

    /**
     * perform the search on a single track note - returning null means that the
     * implementation could not process the search, for example,
     * SearchModeLucene cannot work if the file is not indexed. returning an
     * empty List means that the implementation could process, but found no
     * hits.
     *
     * @param search_text the input text
     * @param chrFilter the chromosome / seq to search or null for all
     * @param statusHolder the status display for output messages
     * @param option the value of an option for this mode
     * @return a list of the syms found by the search
     */
    public List<SeqSymmetry> searchTrack(String search_text, TypeContainerAnnot contSym);
}
