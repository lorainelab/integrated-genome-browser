package com.affymetrix.genometry.data;

import com.affymetrix.genometry.SeqSpan;
import com.affymetrix.genometry.general.DataContainer;
import com.affymetrix.genometry.general.DataSet;
import com.affymetrix.genometry.symmetry.impl.SeqSymmetry;
import java.util.Set;

/**
 *
 * @author dcnorris
 */
public interface DataModelProvider extends DataProvider {

    public Set<DataSet> getDataSources(DataContainer dataContainer);

    public Set<? extends SeqSymmetry> getData(DataSet dataSource, DataContainer dataContainer, SeqSpan seqSpan);
}
