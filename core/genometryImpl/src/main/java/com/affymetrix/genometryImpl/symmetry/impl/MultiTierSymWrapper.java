/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometryImpl.symmetry.impl;

/**
 *
 * @author tarun
 */
public interface MultiTierSymWrapper extends SeqSymmetry{
    
    public static final int CHILD_COUNT = 2;
    
    public SeqSymmetry getPairConnector();
    public SeqSymmetry getFirstChild();
    public SeqSymmetry getSecondChild();
    
}
