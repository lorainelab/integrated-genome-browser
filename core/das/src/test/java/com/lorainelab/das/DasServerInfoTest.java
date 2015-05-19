package com.lorainelab.das;

import java.net.MalformedURLException;
import java.net.URL;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dcnorris
 */
public class DasServerInfoTest {

    private static final Logger logger = LoggerFactory.getLogger(DasServerInfoTest.class);

    @Test
    public void checkServerInfo() throws MalformedURLException {
        DasServerInfo dasServerInfo;
        final String dasUrl = "http://genome.cse.ucsc.edu/cgi-bin/das/dsn";
        URL url = new URL(dasUrl);
        dasServerInfo = new DasServerInfo(url);
        dasServerInfo.getDataSources().entrySet().forEach(entry -> logger.info(entry.toString()));
    }
}
