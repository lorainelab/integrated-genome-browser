/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot.interproscan.resulttype.model;

/**
 *
 * @author Tarun
 */
public class ResultTypes {
    
    private String typeDescription;
    private String typeValue;
    
    public ResultTypes(String typeDescription, String typeValue) {
        this.typeDescription = typeDescription;
        this.typeValue = typeValue;
    }

    public String getTypeDescription() {
        return typeDescription;
    }

    public void setTypeDescription(String typeDescription) {
        this.typeDescription = typeDescription;
    }

    public String getTypeValue() {
        return typeValue;
    }

    public void setTypeValue(String typeValue) {
        this.typeValue = typeValue;
    }
    
    
}
