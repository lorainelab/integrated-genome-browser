/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.protannot.interproscan.api;

/**
 *
 * @author jeckstei
 */
public final class JobSequence {
    private String sequenceName;
    private String proteinSequence;
    private boolean noCds;
    
    public JobSequence(String sequenceName, String proteinSequence, boolean noCds) {
        setNoCds(noCds);
        setSequenceName(sequenceName);
        setProteinSequence(proteinSequence);
    }

    public String getSequenceName() {
        return sequenceName;
    }

    public void setSequenceName(String sequenceName) {
        this.sequenceName = sequenceName;
    }

    public String getProteinSequence() {
        return proteinSequence;
    }

    public void setProteinSequence(String proteinSequence) {
        if(proteinSequence.endsWith("*")) {
            proteinSequence = proteinSequence.substring(0, proteinSequence.length()-1);
        }
        this.proteinSequence = proteinSequence;
    }

    public boolean isNoCds() {
        return noCds;
    }

    public void setNoCds(boolean noCds) {
        this.noCds = noCds;
    }

    
}
