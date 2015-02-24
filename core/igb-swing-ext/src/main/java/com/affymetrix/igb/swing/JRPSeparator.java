/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.swing;

import javax.swing.JSeparator;

/**
 *
 * @author tarun
 */
public class JRPSeparator extends JSeparator implements WeightedJRPWidget {
    
    private String id;
    private int weight;
    
    public JRPSeparator(String id) {
        this(id, -1);
    }
    
    public JRPSeparator(int weight) {
        this("", weight);
    }
    
    public JRPSeparator(String id, int weight) {
        this.id = id;
        this.weight = weight;
    }
    
    @Override
    public int getWeight() {
        return weight;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean consecutiveOK() {
        return false;
    }
    
}
