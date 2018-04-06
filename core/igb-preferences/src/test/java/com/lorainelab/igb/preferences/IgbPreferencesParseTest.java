package com.lorainelab.igb.preferences;

import com.google.common.base.Strings;
import org.lorainelab.igb.preferences.IgbPreferencesParser;
import org.lorainelab.igb.preferences.model.DataProviderConfig;
import org.lorainelab.igb.preferences.model.IgbPreferences;
import org.lorainelab.igb.preferences.model.JsonWrapper;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.bind.JAXBException;
import jdk.nashorn.internal.objects.NativeArray;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
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
    private IgbPreferences prefs;

    @Before
    public void init() {
        igbPreferencesParser = new IgbPreferencesParser();
        Reader reader = new InputStreamReader(IgbPreferencesParseTest.class.getClassLoader().getResourceAsStream("igbDefaultPrefs.json"));
        prefs = igbPreferencesParser.fromJson(reader).get();
    }

    @Test
    public void readJsonIgbPrefs() {
        assertTrue(prefs.getDataProviders().size()>0);
    }
    
    /* checks if all the data providers in igbDefaultPrefs.json has Id*/
    @Test
    public void checkForDataProviderId(/*IgbPreferences prefs*/) {
        
        for (DataProviderConfig item : prefs.getDataProviders()) {
            assertTrue(!Strings.isNullOrEmpty( item.getId().trim() ));
        }
    }
    
    /* checks if all the data providers in igbDefaultPrefs.json has provider name*/
    @Test
    public void checkForDataProviderName(/*IgbPreferences prefs*/) {
        
        for (DataProviderConfig item : prefs.getDataProviders()) {
            assertTrue(!Strings.isNullOrEmpty( item.getName().trim() ));
        }
    }
    
    /* checks if all the data providers in igbDefaultPrefs.json has load priority */
    @Test
    public void checkForDataProviderLoadPriority(/*IgbPreferences prefs*/) {
        
        for (DataProviderConfig item : prefs.getDataProviders()) {
            assertTrue((Object)item.getLoadPriority() instanceof Integer);
        }
    }

    @Test
    public void writePreferencesToJsonFromModel() {
        JsonWrapper jsonWrapper = new JsonWrapper();
        IgbPreferences prefs = new IgbPreferences();
        DataProviderConfig server = new DataProviderConfig();
        server.setName("IGB Quickload");
        server.setDefault("false");
        List<DataProviderConfig> list = new ArrayList<>();
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
