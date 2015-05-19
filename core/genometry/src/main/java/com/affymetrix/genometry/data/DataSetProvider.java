package com.affymetrix.genometry.data;

import com.affymetrix.genometry.general.DataContainer;
import com.affymetrix.genometry.general.DataSet;
import java.util.LinkedHashSet;

/**
 *
 * @author dcnorris
 */
public interface DataSetProvider extends DataProvider {

    public LinkedHashSet<DataSet> getAvailableDataSets(DataContainer dataContainer);

}
