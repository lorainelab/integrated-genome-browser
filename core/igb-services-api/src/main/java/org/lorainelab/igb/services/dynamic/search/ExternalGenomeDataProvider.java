package org.lorainelab.igb.services.dynamic.search;

import java.util.List;

public interface ExternalGenomeDataProvider {
    List<ExternalGenomeData> getPageData(int pageNumber, int pageSize);
    int getTotalGenomes();
    void search(String query);
    void performLoadGenome(ExternalGenomeData genomeData);
    void setSorting(String columnName, Boolean ascending);
    List<String> getColumnNames();
}
