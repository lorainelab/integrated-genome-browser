package org.lorainelab.igb.das.utils;

import com.affymetrix.genometry.SeqSpan;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class DasResiduesHandler extends DefaultHandler {

    private static final Logger logger = LoggerFactory.getLogger(DasResiduesHandler.class);
    private static final Pattern whiteSpace = Pattern.compile("\\s+");
    private static final Matcher matcher = whiteSpace.matcher("");

    private StringBuffer buffer;
    private String residues;
    boolean bfname = false;

    public String getDasResidues(InputStream stream, SeqSpan span) throws ParserConfigurationException, SAXException, IOException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();
        saxParser.parse(new BufferedInputStream(stream), this);
        return residues;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equalsIgnoreCase("DNA")) {
            int length = Integer.parseInt(attributes.getValue("length"));
            buffer = new StringBuffer(length);
        } else {
            buffer = new StringBuffer(10);
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        buffer.append(new String(ch, start, length));

    }

    @Override
    public void endElement(String uri, String localName,
            String qName) throws SAXException {
        if (qName.equalsIgnoreCase("DNA")) {
            residues = matcher.reset(buffer).replaceAll("");
        }

    }
}
