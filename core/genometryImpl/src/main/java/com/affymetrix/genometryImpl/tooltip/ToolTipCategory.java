/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometryImpl.tooltip;

import java.util.HashMap;

/**
 *
 * @author tarun
 */
public class ToolTipCategory {
    
    private String category;
    private int weight;
    private HashMap<String, ToolTipValue> properties;
    
    public ToolTipCategory(String category, HashMap<String, ToolTipValue> properties) {
        this.category = category;
        this.properties = properties;
    }
    
    public ToolTipCategory(String category, int weight, HashMap<String, ToolTipValue> properties) {
        this.category = category;
        this.weight = weight;
        this.properties = properties;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public HashMap<String, ToolTipValue> getProperties() {
        return properties;
    }

    public void setProperties(HashMap<String, ToolTipValue> properties) {
        this.properties = properties;
    }

    public ToolTipCategory() {
    }

}