/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.protannot.interproscan.api;

import org.lorainelab.igb.protannot.interproscan.api.InterProscanService.Status;

/**
 *
 * @author jeckstei
 */
public final class Job {

    private String sequenceName;
    private String id;
    private Status status;

    public Job(String sequenceName, String id) {
        this.sequenceName = sequenceName;
        this.id = id;
        this.status = Status.RUNNING; // default status will be running
    }

    public String getSequenceName() {
        return sequenceName;
    }

    public void setSequenceName(String sequenceName) {
        this.sequenceName = sequenceName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

}
