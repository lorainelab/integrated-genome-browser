package com.lorainelab.das;

import com.affymetrix.genometry.general.DataContainer;
import com.affymetrix.genometry.util.LocalUrlCacher;
import com.affymetrix.genometry.util.QueryBuilder;
import com.affymetrix.genometry.util.SynonymLookup;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @author hiralv
 */
public class DasResiduesHandler extends DefaultHandler {

    private static final Logger logger = LoggerFactory.getLogger(DasResiduesHandler.class);
    private static final Pattern white_space = Pattern.compile("\\s+");
    private static final Matcher matcher = white_space.matcher("");

    private StringBuffer tempVal;
    private String residues = null;

    public String getDasResidues(DataContainer version, String seqid, int min, int max) {
        Set<String> segments = null;//((DasSource) version.versionSourceObj).getEntryPoints();
        String segment = SynonymLookup.getDefaultLookup().findMatchingSynonym(segments, seqid);
        URI request;
        try {
            request = URI.create(version.getDataProvider().getUrl());
            URL url = new URL(request.toURL(), version.getName() + "/dna?");
            QueryBuilder builder = new QueryBuilder(url.toExternalForm());
            builder.add("segment", segment + ":" + (min + 1) + "," + max);
            request = builder.build();
            try (InputStream result_stream = LocalUrlCacher.getInputStream(request.toString())) {
                SAXParserFactory factory = SAXParserFactory.newInstance();
                SAXParser saxParser = factory.newSAXParser();
                saxParser.parse(new BufferedInputStream(result_stream), this);
            }
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }
        return residues;
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equalsIgnoreCase("DNA")) {
            int length = Integer.parseInt(attributes.getValue("length"));
            tempVal = new StringBuffer(length);
        } else {
            tempVal = new StringBuffer(10);
        }

        if (Thread.currentThread().isInterrupted()) {
            tempVal = null;
            residues = null;
            throw new SAXException(new InterruptedException("Thread interruped. Cancelling loading."));
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        tempVal.append(new String(ch, start, length));

        if (Thread.currentThread().isInterrupted()) {
            tempVal = null;
            residues = null;
            throw new SAXException(new InterruptedException("Thread interruped. Cancelling loading."));
        }
    }

    @Override
    public void endElement(String uri, String localName,
            String qName) throws SAXException {

        if (qName.equalsIgnoreCase("DASDNA")) {
            //Do Nothing
        } else if (qName.equalsIgnoreCase("SEQUENCE")) {
            //Do Nothing
        } else if (qName.equalsIgnoreCase("DNA")) {
            residues = matcher.reset(tempVal).replaceAll("");
        }

        if (Thread.currentThread().isInterrupted()) {
            tempVal = null;
            residues = null;
            throw new SAXException(new InterruptedException("Thread interruped. Cancelling loading."));
        }
    }
}
