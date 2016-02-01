/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.lorainelab.igb.menu.api;

import aQute.bnd.annotation.component.Component;
import com.google.common.eventbus.EventBus;

/**
 *
 * @author dcnorris
 */
@Component(immediate = true, provide = MenuItemEventService.class)
public class MenuItemEventService {

    private EventBus eventBus;

    public MenuItemEventService() {
        eventBus = new EventBus();
    }

    public EventBus getEventBus() {
        return eventBus;
    }

}
