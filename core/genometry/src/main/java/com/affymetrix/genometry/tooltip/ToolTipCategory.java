/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometry.tooltip;

import java.util.Map;

/**
 *
 * @author tarun
 */
public class ToolTipCategory {
    
    private String category;
    private int weight;
    private Map<String, String> properties;
    
    public ToolTipCategory(String category, Map<String, String> properties) {
        this.category = category;
        this.properties = properties;
    }
    
    public ToolTipCategory(String category, int weight, Map<String, String> properties) {
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

    public Map<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public ToolTipCategory() {
    }

}