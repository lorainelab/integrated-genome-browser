/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot.model;

import aQute.bnd.annotation.component.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author Tarun
 */
@Component(name = InterProScanTableModel.COMPONENT_NAME, immediate = true, provide = InterProScanTableModel.class)
public class InterProScanTableModel extends AbstractTableModel {

    public static final String COMPONENT_NAME = "InterProScanTableModel";
    List<InterProScanTableData> results;

    List<InterProScanTableModelListener> listeners;

    public InterProScanTableModel() {
        this.results = new ArrayList<>();
        this.listeners = new ArrayList<>();
        this.addData("Something 1", "jobid");
        this.addData("Something 1", "jobid");
        this.addData("Something 1", "jobid");
        this.addData("Something 1", "jobid");
    }

    public void addListener(InterProScanTableModelListener listener) {
        listeners.add(listener);
    }

    public void removeListener(InterProScanTableModelListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners() {
        for (InterProScanTableModelListener listener : listeners) {
            listener.tableDataChanged();
        }
    }

    public void addData(String geneModelCoordinate, String jobId) {
        results.add(new InterProScanTableData(geneModelCoordinate, jobId, jobId));
    }

    public List<InterProScanTableData> getResults() {
        return results;
    }

    @Override
    public int getColumnCount() {
        return 2;
    }

    @Override
    public int getRowCount() {
        return results.size();
    }

    private static final int URL_COLUMN = 1;
    private static final int GENE_MODEL_COORDINATES_COLUMN = 0;

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == GENE_MODEL_COORDINATES_COLUMN) {
            return results.get(rowIndex).geneModelCoordinate;
        } else if (columnIndex == URL_COLUMN) {
            return results.get(rowIndex).url;
        } else {
            return null;
        }
    }

    public interface InterProScanTableModelListener {

        public void tableDataChanged();
    }

    public class InterProScanTableData {

        private String geneModelCoordinate;
        private String url;
        private String resultsInJob;

        public InterProScanTableData(String geneModelCoordinate, String jobId, String resultsInJob) {
            this.geneModelCoordinate = geneModelCoordinate;
            this.url = BASE_URL + jobId + "/xml";
            this.resultsInJob = resultsInJob;
        }
        private static final String BASE_URL = "http://www.ebi.ac.uk/Tools/services/rest/iprscan5/result/";

        public InterProScanTableData(String geneModelCoordinate, String jobId) {
            this.geneModelCoordinate = geneModelCoordinate;
            this.url = BASE_URL + jobId + "/xml";;
        }

        public String getGeneModelCoordinate() {
            return geneModelCoordinate;
        }

        public void setGeneModelCoordinate(String geneModelCoordinate) {
            this.geneModelCoordinate = geneModelCoordinate;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getResultsInJob() {
            return resultsInJob;
        }

        public void setResultsInJob(String resultsInJob) {
            this.resultsInJob = resultsInJob;
        }
    }

}
