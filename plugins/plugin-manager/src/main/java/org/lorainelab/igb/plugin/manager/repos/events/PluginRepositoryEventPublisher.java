package org.lorainelab.igb.plugin.manager.repos.events;

import org.osgi.service.component.annotations.Component;
import com.google.common.eventbus.EventBus;

/**
 *
 * @author dcnorris
 */
@Component(name = PluginRepositoryEventPublisher.COMPONENT_NAME, immediate = true, service = PluginRepositoryEventPublisher.class)
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
