/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot.interproscan.api;

import java.util.Set;

/**
 *
 * @author jeckstei
 */
public class JobRequest {

    private String email;
    private String title;
    private Set<SignatureMethods> signatureMethods;
    private boolean goterms;
    private boolean pathways;
    private String sequence;

    public boolean isGoterms() {
        return goterms;
    }

    public void setGoterms(boolean goterms) {
        this.goterms = goterms;
    }

    public boolean isPathways() {
        return pathways;
    }

    public void setPathways(boolean pathways) {
        this.pathways = pathways;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }
    
    

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Set<SignatureMethods> getSignatureMethods() {
        return signatureMethods;
    }

    public void setSignatureMethods(Set<SignatureMethods> signatureMethods) {
        this.signatureMethods = signatureMethods;
    }
    
    

    private enum SignatureMethods {

        BlastProDom("ProDom"), FPrintScan("PRINTS"),
        HMMPIR("PIRSF"), HMMPfam("PfamA"), HMMSmart("SMART"),
        HMMTigr("TIGRFAM"), ProfileScan("PrositeProfiles"), HAMAP("HAMAP"),
        Coils("Coils"), Phobius("Phobius"), Gene3D("Gene3D"),
        HMMPanther("Panther"), TMHMM("TMHMM"), SignalPHMM("SignalP"),
        SuperFamily("SuperFamily"), PatternScan("PrositePatterns");

        private final String value;

        private SignatureMethods(String value) {
            this.value = value;
        }

    }
}
