/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.protannot.interproscan.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 *
 * @author jeckstei
 */
public class JobRequest {

    private String email;
    private Optional<String> title;
    private Optional<Set<String>> signatureMethods;
    private Optional<Boolean> goterms;
    private Optional<Boolean> pathways;
    private List<JobSequence> jobSequences;

    public JobRequest() {
        jobSequences = new ArrayList<>();
    
    }
    
    

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Optional<String> getTitle() {
        return title;
    }

    public void setTitle(Optional<String> title) {
        this.title = title;
    }

    public Optional<Set<String>> getSignatureMethods() {
        return signatureMethods;
    }

    public void setSignatureMethods(Optional<Set<String>> signatureMethods) {
        this.signatureMethods = signatureMethods;
    }

    public Optional<Boolean> getGoterms() {
        return goterms;
    }

    public void setGoterms(Optional<Boolean> goterms) {
        this.goterms = goterms;
    }

    public Optional<Boolean> getPathways() {
        return pathways;
    }

    public void setPathways(Optional<Boolean> pathways) {
        this.pathways = pathways;
    }

    public List<JobSequence> getJobSequences() {
        return jobSequences;
    }

    public void setJobSequences(List<JobSequence> jobSequences) {
        this.jobSequences = jobSequences;
    }

}
