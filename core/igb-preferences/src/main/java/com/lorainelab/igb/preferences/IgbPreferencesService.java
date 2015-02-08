package com.lorainelab.igb.preferences;

import com.lorainelab.igb.preferences.model.IgbPreferences;
import com.lorainelab.igb.preferences.model.JsonWrapper;
import java.io.Reader;
import java.net.URL;
import java.util.Optional;

/**
 *
 * @author dcnorris
 */
public interface IgbPreferencesService {

    Optional<IgbPreferences> fromDefaultPreferences();

    Optional<IgbPreferences> fromJson(URL url);

    Optional<IgbPreferences> fromJson(String input);

    Optional<IgbPreferences> fromJson(Reader reader);

    Optional<IgbPreferences> fromXml(Reader reader);

    String toJson(JsonWrapper config);

}
