package com.affymetrix.genometry.data.sequence;

import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.general.DataContainer;

/**
 *
 * @author dcnorris
 */
public interface ReferenceSequenceProvider extends ReferenceSequenceResource {

    /**
     * Returns the sequence for the requested location
     *
     * @return raw sequence for the requested coordinates
     */
    public String getSequence(DataContainer version, SeqSpan span);
}
