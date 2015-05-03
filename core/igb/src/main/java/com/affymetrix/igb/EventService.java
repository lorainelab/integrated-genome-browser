package com.affymetrix.igb;

import aQute.bnd.annotation.component.Component;
import com.google.common.eventbus.EventBus;

/**
 *
 * @author dcnorris
 */
@Component(name = EventService.COMPONENT_NAME, immediate = true, provide = EventService.class)
public class EventService {

    public static final String COMPONENT_NAME = "EventService";
    //making this static as temporary measure during migration to components
    private static final EventBus bus = new EventBus();

    public EventService() {

    }

    public EventBus getEventBus() {
        return bus;
    }

    public static EventBus getModuleEventBus() {
        return bus;
    }

}
