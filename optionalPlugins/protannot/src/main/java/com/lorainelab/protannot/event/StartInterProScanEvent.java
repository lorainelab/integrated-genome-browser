/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.protannot.event;

/**
 *
 * @author jeckstei
 */
public class StartInterProScanEvent {
    private final String id;

    public StartInterProScanEvent(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
    
}
