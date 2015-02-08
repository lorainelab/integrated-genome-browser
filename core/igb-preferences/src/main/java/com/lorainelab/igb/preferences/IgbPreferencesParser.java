package com.lorainelab.igb.preferences;

import aQute.bnd.annotation.component.Component;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lorainelab.igb.preferences.model.IgbPreferences;
import com.lorainelab.igb.preferences.model.JsonWrapper;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.Optional;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
@Component(name = IgbPreferencesParser.COMPONENT_NAME, immediate = true)
public class IgbPreferencesParser implements IgbPreferencesService {

    private static final Logger logger = LoggerFactory.getLogger(IgbPreferencesParser.class);
    public static final String COMPONENT_NAME = "IgbPreferencesParser";
    private static final Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    private JAXBContext jaxbContext;
    private Unmarshaller unmarshaller;

    public IgbPreferencesParser() {
        try {
            jaxbContext = JAXBContext.newInstance(IgbPreferences.class);
            unmarshaller = jaxbContext.createUnmarshaller();
        } catch (JAXBException ex) {
            logger.error("Could not initialize JAXBContext for igb preferences", ex);
        }
    }

    @Override
    public Optional<IgbPreferences> fromJson(URL url) {
        try {
            return fromJson(Resources.asCharSource(url, Charsets.UTF_8).read());
        } catch (IOException ex) {
            logger.error("Error Loading preferences file from url {}", url, ex);
        }
        return Optional.empty();
    }

    @Override
    public Optional<IgbPreferences> fromJson(String input) {
        return Optional.ofNullable(gson.fromJson(input, JsonWrapper.class).getPrefs());
    }

    @Override
    public Optional<IgbPreferences> fromJson(Reader reader) {
        return Optional.ofNullable(gson.fromJson(reader, JsonWrapper.class).getPrefs());
    }

    @Override
    public String toJson(JsonWrapper config) {
        return gson.toJson(config);
    }

    @Override
    public Optional<IgbPreferences> fromXml(Reader reader) {

        try {
            return Optional.ofNullable((IgbPreferences) unmarshaller.unmarshal(reader));
        } catch (JAXBException ex) {
            logger.error("Error Loading xml preferences file", ex);
        }
        return Optional.empty();
    }

    @Override
    public Optional<IgbPreferences> fromDefaultPreferences() {
        Reader reader = new InputStreamReader(IgbPreferencesParser.class.getClassLoader().getResourceAsStream("igbDefaultPrefs.json"));
        return fromJson(reader);
    }

}
