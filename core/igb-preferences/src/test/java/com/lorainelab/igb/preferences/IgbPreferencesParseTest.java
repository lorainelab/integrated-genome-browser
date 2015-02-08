package com.lorainelab.igb.preferences;

import com.lorainelab.igb.preferences.model.DataProvider;
import com.lorainelab.igb.preferences.model.IgbPreferences;
import com.lorainelab.igb.preferences.model.JsonWrapper;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.JAXBException;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class IgbPreferencesParseTest {

    private static final Logger logger = LoggerFactory.getLogger(IgbPreferencesParseTest.class);
    private IgbPreferencesParser igbPreferencesParser;

    @Before
    public void init() {
        igbPreferencesParser = new IgbPreferencesParser();
    }

    @Test
    public void readJsonIgbPrefs() {
        Reader reader = new InputStreamReader(IgbPreferencesParseTest.class.getClassLoader().getResourceAsStream("igbDefaultPrefs.json"));
        IgbPreferences prefs = igbPreferencesParser.fromJson(reader).get();
        assertEquals("IGB Quickload", prefs.getDataProviders().get(0).getName());
    }

    @Test
    public void writePreferencesToJsonFromModel() {
        JsonWrapper jsonWrapper = new JsonWrapper();
        IgbPreferences prefs = new IgbPreferences();
        DataProvider server = new DataProvider();
        server.setName("IGB Quickload");
        server.setDefault("false");
        List<DataProvider> list = new ArrayList<>();
        list.add(server);
        prefs.setDataProviders(list);
        jsonWrapper.setPrefs(prefs);
        logger.info(igbPreferencesParser.toJson(jsonWrapper));
    }

    @Test
    public void readXmlIgbPrefs() throws JAXBException {
        Reader reader = new InputStreamReader(IgbPreferencesParseTest.class.getClassLoader().getResourceAsStream("igbDefaultPrefs.xml"));
        IgbPreferences prefs = igbPreferencesParser.fromXml(reader).get();
        assertEquals("IGB Quickload", prefs.getDataProviders().get(0).getName());
    }

}
