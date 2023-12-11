package org.lorainelab.igb.quickload.utils;

import org.lorainelab.igb.quickload.util.AnnotsParser;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
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
            assertTrue(!file.getProps().isEmpty());
        });
    }

}
