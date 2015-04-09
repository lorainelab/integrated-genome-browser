package com.lorainelab.quickload.utils;

import com.google.common.io.CharStreams;
import static com.lorainelab.quickload.util.QuickloadUtils.getInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author dcnorris
 */
public class QuickloadUtilsTest {

    @Test
    public void contentsTxtParserTest() throws MalformedURLException, IOException {
        Assert.assertTrue(CharStreams.toString(new InputStreamReader(getInputStream(new URL("http://igbquickload.org/contents.txt")))).contains("A_thaliana_Jun_2009"));
    }

}
