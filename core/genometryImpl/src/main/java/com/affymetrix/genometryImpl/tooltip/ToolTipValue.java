/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometryImpl.tooltip;

/**
 *
 * @author tarun
 */
public class ToolTipValue {
    
    private String value;
    private int weight;
    public ToolTipValue(){
        
    }
    
    public ToolTipValue(String value, int weight){
        this.value = value;
        this.weight = weight;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }
    
    
}
