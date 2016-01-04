package org.lorainelab.igb.quickload.utils;

import org.lorainelab.igb.quickload.util.AnnotsParser;
import java.io.IOException;
import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author dcnorris
 */
public class AnnotsParserTest {

    @Test
    public void filesTagAsRoot() throws IOException {
        AnnotsParser parser = new AnnotsParser();
//        Assert.assertEquals(3, parser.getQuickloadFileList(reader).size());
        parser.getQuickloadFileList(AnnotsParserTest.class.getClassLoader().getResourceAsStream("annots-1.xml")).stream().forEach(file -> {
            System.out.println(file.getName());
            System.out.println(file.getTitle());
            System.out.println(file.getUrl());
            Assert.assertTrue("", !file.getProps().isEmpty());
        });
    }

}
