package com.affymetrix.genometry.data.sequence;

import com.affymetrix.genometry.general.GenomeVersion;

/**
 *
 * @author dcnorris
 */
public interface ReferenceSequenceDataSetProvider extends ReferenceSequenceResource {

    /**
     * @return the url of the sequence file to be queried (e.g. a 2bit of fasta file url)
     */
    public String getSequenceFileUrl(GenomeVersion genomeVersion);
}
