/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometry.util;

/**
 *
 * @author Tarun
 */
public interface Weighted extends Comparable<Weighted> {

    public int getWeight();

    @Override
    public default int compareTo(Weighted that) {
        if (this.getWeight() == -1) {
            return 1;
        } else if (that.getWeight() == -1) {
            return -1;
        }
        return this.getWeight() - that.getWeight();
    }
}
