/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot.interproscan.api;

import java.util.List;
import java.util.Optional;
import org.bioviz.protannot.interproscan.resulttype.model.ResultTypes;

/**
 *
 * @author jeckstei
 */
public interface InterProscanService {
    public Status status(String jobId);
    public Optional<String> run(JobRequest jobRequest);
    public List<ResultTypes> resultTypes(String jobId);
    
    public static enum Status {
        RUNNING,FINISHED,ERROR,FAILURE,NOT_FOUND;
    }
}
