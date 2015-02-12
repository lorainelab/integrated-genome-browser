package com.lorainelab.igb.plugins.repos.events;

import aQute.bnd.annotation.component.Component;
import com.google.common.eventbus.EventBus;

/**
 *
 * @author dcnorris
 */
@Component(name = PluginRepositoryEventPublisher.COMPONENT_NAME, immediate = true, provide = PluginRepositoryEventPublisher.class)
public class PluginRepositoryEventPublisher {

    public static final String COMPONENT_NAME = "PluginRepositoryEventPublisher";
    private final EventBus bus;

    public PluginRepositoryEventPublisher() {
        bus = new EventBus();
    }

    public EventBus getPluginRepositoryEventBus() {
        return bus;
    }

}
