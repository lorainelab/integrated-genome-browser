package org.lorainelab.igb.quickload.utils;

import static com.affymetrix.genometry.util.UriUtils.getInputStream;
import static org.lorainelab.igb.quickload.util.QuickloadUtils.getUri;
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
    
    @Test
    public void getUriTest() {
        String absoluteOnline_fileName = "http://igbquickload.org/quickload/A_thaliana_Jun_2009/Araport11.bed.gz";
        String relative_fileName = "../down stream/E_unicornis_Jul_2043_up.bed.gz";
        String absoluteLocal_fileName = "/Users/lorainelab/Desktop/2805/relative space/E_unicornis_Jul_2043/down stream/E_unicornis_Jul_2043_down.bed.gz";
        String online_url = "http://igbquickload.org/quickload/";
        String local_url = "/Users/lorainelab/Desktop/2805/relative space/";
        String genomeVersionName = "A_thaliana_Apr_2008";
        URI uri = null;
        
        //test absolute online
        uri = getUri(absoluteOnline_fileName, online_url, genomeVersionName);
        Assert.assertEquals("http://igbquickload.org/quickload/A_thaliana_Jun_2009/Araport11.bed.gz", uri.toString());
        
        //test relative online
        uri = getUri(relative_fileName, online_url, genomeVersionName);
        Assert.assertEquals("http://igbquickload.org/quickload/A_thaliana_Apr_2008/../down%20stream/E_unicornis_Jul_2043_up.bed.gz", uri.toString());
        
        //test absolute local
        uri = getUri(absoluteLocal_fileName, local_url, genomeVersionName);
        Assert.assertEquals("file:/Users/lorainelab/Desktop/2805/relative%20space/E_unicornis_Jul_2043/down%20stream/E_unicornis_Jul_2043_down.bed.gz", uri.toString());
        
        //test relative local
        uri = getUri(relative_fileName, local_url, genomeVersionName);
        Assert.assertEquals("file:/Users/lorainelab/Desktop/2805/relative%20space/A_thaliana_Apr_2008/../down%20stream/E_unicornis_Jul_2043_up.bed.gz", uri.toString());
    }
    
}
