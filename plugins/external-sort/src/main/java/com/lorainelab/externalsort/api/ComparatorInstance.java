/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.externalsort.api;

/**
 *
 * @author jeckstei
 */
public class ComparatorInstance {
    private String line;
    private ComparatorMetadata comparatorMetadata;

    public String getLine() {
        return line;
    }

    public void setLine(String line) {
        this.line = line;
    }

    public ComparatorMetadata getComparatorMetadata() {
        return comparatorMetadata;
    }

    public void setComparatorMetadata(ComparatorMetadata comparatorMetadata) {
        this.comparatorMetadata = comparatorMetadata;
    }

}
