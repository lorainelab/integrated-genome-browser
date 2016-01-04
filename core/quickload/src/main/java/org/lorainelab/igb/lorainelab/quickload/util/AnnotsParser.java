package org.lorainelab.igb.quickload.util;

import com.google.common.collect.Lists;
import org.lorainelab.igb.quickload.model.annots.AnnotsRootElement;
import org.lorainelab.igb.quickload.model.annots.QuickloadFile;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;

/**
 *
 * @author dcnorris
 */
public class AnnotsParser {

    private static final Logger logger = LoggerFactory.getLogger(AnnotsParser.class);
    private JAXBContext primaryContext;
    private Unmarshaller primaryUnmarshaller;

    public AnnotsParser() {
        try {
            primaryContext = JAXBContext.newInstance(AnnotsRootElement.class);
            primaryUnmarshaller = primaryContext.createUnmarshaller();
        } catch (JAXBException ex) {
            logger.error("Could not initialize JAXBContext for annots.xml parser", ex);
        }
    }

    public List<QuickloadFile> getQuickloadFileList(InputStream inputStream) throws IOException {
        List<QuickloadFile> annotsFileList = Lists.newArrayList();
        fromXml(inputStream).ifPresent(filesTag -> filesTag.getFiles().stream().forEach(annotsFileList::add));
        return annotsFileList;
    }

    private Optional<AnnotsRootElement> fromXml(InputStream inputStream) throws IOException {
        try {
            SAXSource saxSource = createSaxSource(inputStream);
            return Optional.ofNullable((AnnotsRootElement) primaryUnmarshaller.unmarshal(saxSource));
        } catch (JAXBException | SAXException | ParserConfigurationException ex) {
            logger.error("Error Loading xml preferences file", ex);
        }

        return Optional.empty();
    }

    private static SAXSource createSaxSource(final InputStream inputSteam) throws SAXException, ParserConfigurationException {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        spf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        spf.setFeature("http://xml.org/sax/features/validation", false);
        XMLReader xr = (XMLReader) spf.newSAXParser().getXMLReader();
        SAXSource source = new SAXSource(xr, new InputSource(inputSteam));
        return source;
    }

}
