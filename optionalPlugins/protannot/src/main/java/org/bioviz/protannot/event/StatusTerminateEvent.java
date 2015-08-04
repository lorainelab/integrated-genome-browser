/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot.event;

/**
 *
 * @author Tarun
 */
public class StatusTerminateEvent {
    private String id;

    public StatusTerminateEvent(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
    
    
    
}
