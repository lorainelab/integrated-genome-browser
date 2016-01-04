/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.protannot.event;

import org.lorainelab.igb.protannot.view.StatusBar;

/**
 *
 * @author Tarun
 */
public class StatusSetEvent {
    
    private String id;
    private String statusMessage;
    private StatusBar.ICONS messageIcon;
    private boolean isProgressBarRequired;

    public StatusSetEvent(String statusMessage, StatusBar.ICONS messageIcon, boolean isProgressBarRequired, String id) {
        this.statusMessage = statusMessage;
        this.messageIcon = messageIcon;
        this.isProgressBarRequired = isProgressBarRequired;
        this.id = id;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public StatusBar.ICONS getMessageIcon() {
        return messageIcon;
    }

    public boolean isProgressBarRequired() {
        return isProgressBarRequired;
    }

    public String getId() {
        return id;
    }
    
    
}
