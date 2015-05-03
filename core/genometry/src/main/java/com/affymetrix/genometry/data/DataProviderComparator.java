package com.affymetrix.genometry.data;

import java.util.Comparator;

/**
 *
 * @author dcnorris
 */
public class DataProviderComparator implements Comparator<DataProvider> {

    @Override
    public int compare(DataProvider o1, DataProvider o2) {
        return Integer.compare(o1.getLoadPriority(), o2.getLoadPriority());
    }

}
