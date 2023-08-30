package com.affymetrix.igb.prefs;

import org.lorainelab.igb.services.window.preferences.PreferencesPanelProvider;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;

/**
 *
 * @author dcnorris
 */
@Component(name = PreferencesPanelRegistry.COMPONENT_NAME, immediate = true)
public class PreferencesPanelRegistry {

    public static final String COMPONENT_NAME = "PreferencesPanelRegistry";

    @Reference(cardinality = ReferenceCardinality.MULTIPLE, policy = ReferencePolicy.DYNAMIC, unbind = "removePreferencesPanel")
    public void addPreferencesPanel(PreferencesPanelProvider panelProvider) {
        //TODO eventually this singleton dependency must be made a service dependency
        PreferencesPanel.getSingleton().addPreferencePanel(panelProvider);
    }

    public void removePreferencesPanel(PreferencesPanelProvider panelProvider) {
        PreferencesPanel.getSingleton().removePrefEditorComponent(panelProvider);
    }
}
