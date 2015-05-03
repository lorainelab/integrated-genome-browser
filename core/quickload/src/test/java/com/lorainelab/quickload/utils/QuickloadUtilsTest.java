package com.lorainelab.quickload.utils;

import com.google.common.io.CharStreams;
import static com.lorainelab.quickload.util.QuickloadUtils.getInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author dcnorris
 */
public class QuickloadUtilsTest {

    @Test
    public void contentsTxtParserTest() throws IOException, URISyntaxException {
        Assert.assertTrue(CharStreams.toString(new InputStreamReader(getInputStream(new URI("http://igbquickload.org/contents.txt")))).contains("A_thaliana_Jun_2009"));
    }

}
