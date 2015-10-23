/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.protannot;

import aQute.bnd.annotation.component.Component;
import com.google.common.eventbus.EventBus;

/**
 *
 * @author Tarun
 */
@Component(name = ProtAnnotEventService.COMPONENT_NAME, immediate = true, provide = ProtAnnotEventService.class)
public class ProtAnnotEventService {

    public static final String COMPONENT_NAME = "ProtAnnotEventService";

    private static final EventBus bus = new EventBus();

    public ProtAnnotEventService() {
    }

    public EventBus getEventBus() {
        return bus;
    }

    public static EventBus getModuleEventBus() {
        return bus;
    }

}
