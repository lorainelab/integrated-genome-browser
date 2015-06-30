/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot.interproscan.api;

import java.util.Optional;
import java.util.Set;

/**
 *
 * @author jeckstei
 */
public class JobRequest {

    private String email;
    private Optional<String> title;
    private Optional<Set<SignatureMethods>> signatureMethods;
    private Optional<Boolean> goterms;
    private Optional<Boolean> pathways;
    private Optional<String> sequence;

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

    public Optional<Set<SignatureMethods>> getSignatureMethods() {
        return signatureMethods;
    }

    public void setSignatureMethods(Optional<Set<SignatureMethods>> signatureMethods) {
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

    public Optional<String> getSequence() {
        return sequence;
    }

    public void setSequence(Optional<String> sequence) {
        this.sequence = sequence;
    }

   
    
    

    public enum SignatureMethods {

        BlastProDom("ProDom"), FPrintScan("PRINTS"),
        HMMPIR("PIRSF"), HMMPfam("PfamA"), HMMSmart("SMART"),
        HMMTigr("TIGRFAM"), ProfileScan("PrositeProfiles"), HAMAP("HAMAP"),
        Coils("Coils"), Phobius("Phobius"), Gene3D("Gene3d"),
        HMMPanther("Panther"), TMHMM("TMHMM"), SignalPHMM("SignalP"),
        SuperFamily("SuperFamily"), PatternScan("PrositePatterns");

        private final String value;

        private SignatureMethods(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return this.value;
        }

    }
}
