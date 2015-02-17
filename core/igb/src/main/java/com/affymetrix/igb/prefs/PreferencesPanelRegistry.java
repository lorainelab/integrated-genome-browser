package com.affymetrix.igb.prefs;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.lorainelab.igb.service.api.PreferencesPanelProvider;

/**
 *
 * @author dcnorris
 */
@Component(name = PreferencesPanelRegistry.COMPONENT_NAME, immediate = true)
public class PreferencesPanelRegistry {

    public static final String COMPONENT_NAME = "PreferencesPanelRegistry";

    @Reference(multiple = true, optional = true, dynamic = true, unbind = "removePreferencesPanel")
    public void addPreferencesPanel(PreferencesPanelProvider panelProvider) {
        //TODO eventually this singleton dependency must be made a service dependency
        PreferencesPanel.getSingleton().addPrefEditorComponent(panelProvider);
    }

    public void removePreferencesPanel(PreferencesPanelProvider panelProvider) {
        PreferencesPanel.getSingleton().removePrefEditorComponent(panelProvider);
    }
}
