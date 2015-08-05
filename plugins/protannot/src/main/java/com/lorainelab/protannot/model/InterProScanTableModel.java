/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.protannot.model;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import com.lorainelab.protannot.InterProScanModelUpdateEvent;
import com.lorainelab.protannot.ProtAnnotEventService;
import com.lorainelab.protannot.interproscan.api.InterProscanService.Status;
import com.lorainelab.protannot.interproscan.api.Job;

/**
 *
 * @author Tarun
 */
public class InterProScanTableModel extends AbstractTableModel {

    private List<InterProScanTableData> results;
    private static int MODEL_ID = 1;
    private ProtAnnotEventService eventService;
    private int id;
    private static boolean allJobsCancelled = false;

    public InterProScanTableModel() {
        this.results = new ArrayList<>();
        id = MODEL_ID++;
    }

    public void setEventService(ProtAnnotEventService eventService) {
        this.eventService = eventService;
        this.eventService.getEventBus().register(this);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void addData(String proteinProductId, String jobId, Status status) {
        results.add(new InterProScanTableData(proteinProductId, jobId, status));
    }

    public void updateModel(List<Job> jobs) {
        if (allJobsCancelled) {
            return;
        }
        results.clear();
        for (Job job : jobs) {
            addData(job.getSequenceName(), job.getId(), job.getStatus());
        }
        eventService.getEventBus().post(new InterProScanModelUpdateEvent());
    }

    public void cancelAllJobs() {
        for (InterProScanTableData result : results) {
            result.status = Status.CANCELLED;
        }
        allJobsCancelled = true;
        eventService.getEventBus().post(new InterProScanModelUpdateEvent());
    }

    public List<InterProScanTableData> getResults() {
        return results;
    }

    @Override
    public int getColumnCount() {
        return 3;
    }

    @Override
    public int getRowCount() {
        return results.size();
    }

    public static final int PROTEIN_PRODUCT_ID_COLUMN = 0;
    public static final int URL_COLUMN = 1;
    public static final int STATUS_COLUMN = 2;

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex == PROTEIN_PRODUCT_ID_COLUMN) {
            return results.get(rowIndex).proteinProductId;
        } else if (columnIndex == URL_COLUMN) {
            return results.get(rowIndex).url;
        } else if (columnIndex == STATUS_COLUMN) {
            return results.get(rowIndex).status.toString();
        } else {
            return null;
        }
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex == PROTEIN_PRODUCT_ID_COLUMN) {
            return "Protein Product ID";
        } else if (columnIndex == URL_COLUMN) {
            return "Result";
        } else if (columnIndex == STATUS_COLUMN) {
            return "Status";
        } else {
            return null;
        }
    }

    public class InterProScanTableData {

        private String proteinProductId;
        private String url;
        private Status status;

        public InterProScanTableData(String proteinProductId, String jobId, Status status) {
            this.proteinProductId = proteinProductId;
            this.url = BASE_URL + jobId + "/xml";
            this.status = status;
        }
        private static final String BASE_URL = "http://www.ebi.ac.uk/Tools/services/rest/iprscan5/result/";

        public InterProScanTableData(String proteinProductId, String jobId) {
            this.proteinProductId = proteinProductId;
            this.url = BASE_URL + jobId + "/xml";;
        }

        public String getProteinProductId() {
            return proteinProductId;
        }

        public void setProteinProductId(String proteinProductId) {
            this.proteinProductId = proteinProductId;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Status getStatus() {
            return status;
        }

        public void setStatus(Status status) {
            this.status = status;
        }

    }

    public void resetModel() {
        allJobsCancelled = false;
        results.clear();
    }

}
