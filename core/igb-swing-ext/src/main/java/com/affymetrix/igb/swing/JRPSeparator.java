/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.swing;

import java.util.PrimitiveIterator;
import java.util.stream.IntStream;
import javax.swing.JSeparator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author tarun
 */
public class JRPSeparator extends JSeparator implements WeightedJRPWidget {
    
    private String id;
    private int weight;
    
    private static final Logger logger = LoggerFactory.getLogger(JRPSeparator.class);
    
    private static PrimitiveIterator.OfInt ids = IntStream.iterate(0, i -> i + 1).iterator();
    
    public JRPSeparator(int weight) {
        this(ids.next().toString(), weight);
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
        return true;
    }
    
}
