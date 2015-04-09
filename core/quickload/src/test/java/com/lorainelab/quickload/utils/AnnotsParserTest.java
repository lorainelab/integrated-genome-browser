package com.lorainelab.quickload.utils;

import com.lorainelab.quickload.util.AnnotsParser;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author dcnorris
 */
public class AnnotsParserTest {

    @Test
    public void filesTagAsRoot() throws IOException {
        Reader reader = new InputStreamReader(AnnotsParserTest.class.getClassLoader().getResourceAsStream("annots-1.xml"));
        AnnotsParser parser = new AnnotsParser();
        Assert.assertEquals(3, parser.getQuickloadFileList(reader).size());
    }

}
