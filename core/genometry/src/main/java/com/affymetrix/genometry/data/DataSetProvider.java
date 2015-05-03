package com.affymetrix.genometry.data;

import com.affymetrix.genometry.general.DataContainer;
import com.affymetrix.genometry.general.DataSet;
import java.util.Set;

/**
 *
 * @author dcnorris
 */
public interface DataSetProvider extends DataProvider {

    public Set<DataSet> getAvailableDataSets(DataContainer dataContainer);

}
