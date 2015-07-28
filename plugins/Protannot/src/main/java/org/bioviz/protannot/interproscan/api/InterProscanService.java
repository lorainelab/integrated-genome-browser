/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot.interproscan.api;

import java.util.List;
import java.util.Optional;
import org.bioviz.protannot.interproscan.appl.model.ParameterType;
import org.w3c.dom.Document;

/**
 *
 * @author jeckstei
 */
public interface InterProscanService {

    public Status status(String jobId);

    public List<Job> run(JobRequest jobRequest);

    public ParameterType getApplications();
    
    public Optional<String> getApplicationLabel(String key);

    public Optional<Document> result(String jobId);

    public static enum Status {

        RUNNING, FINISHED, ERROR, FAILURE, NOT_FOUND, CANCELLED;
    }
}
