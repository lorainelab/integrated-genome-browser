/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot.interproscan.api;

import java.util.Optional;

/**
 *
 * @author jeckstei
 */
public interface InterProscanService {
    public Status status(String jobId);
    public Optional<String> run(JobRequest jobRequest);
    
    public static enum Status {
        RUNNING,FINISHED,ERROR,FAILURE,NOT_FOUND;
    }
}
