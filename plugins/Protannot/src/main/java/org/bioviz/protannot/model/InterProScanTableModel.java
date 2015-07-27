/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot.model;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;
import org.bioviz.protannot.InterProScanModelUpdateEvent;
import org.bioviz.protannot.ProtAnnotEventService;
import org.bioviz.protannot.interproscan.api.InterProscanService.Status;
import org.bioviz.protannot.interproscan.api.Job;

/**
 *
 * @author Tarun
 */
@Component(name = InterProScanTableModel.COMPONENT_NAME, immediate = true, provide = InterProScanTableModel.class)
public class InterProScanTableModel extends AbstractTableModel {

    public static final String COMPONENT_NAME = "InterProScanTableModel";
    List<InterProScanTableData> results;

    ProtAnnotEventService eventService;

    public InterProScanTableModel() {
        this.results = new ArrayList<>();
    }

    @Reference
    public void setEventService(ProtAnnotEventService eventService) {
        this.eventService = eventService;
    }

    @Activate
    public void activate() {
        eventService.getEventBus().register(this);
    }

    public void addData(String proteinProductId, String jobId) {
        results.add(new InterProScanTableData(proteinProductId, jobId));
    }

    public void addData(String proteinProductId, String jobId, Status status) {
        results.add(new InterProScanTableData(proteinProductId, jobId, status));
    }

    public void updateModel(List<Job> jobs) {
        results.clear();
        for (Job job : jobs) {
            addData(job.getSequenceName(), job.getId(), job.getStatus());
        }
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

    private static final int PROTEIN_PRODUCT_ID_COLUMN = 0;
    private static final int URL_COLUMN = 1;
    private static final int STATUS_COLUMN = 2;

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

}
