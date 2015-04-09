package com.affymetrix.genometry.data;

import com.affymetrix.genometry.general.GenomeVersion;
import java.util.Set;

/**
 *
 * @author dcnorris
 */
public interface DataSetProvider extends DataProvider {

    public Set<String> getAvailableDataSetUrls(GenomeVersion genomeVersion);

}
