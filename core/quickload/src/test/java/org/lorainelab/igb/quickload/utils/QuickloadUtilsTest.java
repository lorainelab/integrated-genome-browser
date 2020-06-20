package org.lorainelab.igb.quickload.utils;

import static com.affymetrix.genometry.util.UriUtils.getInputStream;
import com.google.common.io.CharStreams;

import java.io.File;
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
        //URI uri = new URI("http://quickload.bioviz.org/quickload/contents.txt");
        String path = "src/test/resources/quickload/contents.txt";
        File file = new File(path);
        URI mockURI = file.toURI();
        Assert.assertTrue(CharStreams.toString(new InputStreamReader(getInputStream(mockURI))).contains("A_thaliana_Jun_2009"));
    }

}
