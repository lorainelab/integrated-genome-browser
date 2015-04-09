package com.lorainelab.quickload.util;

import com.google.common.collect.Lists;
import com.lorainelab.quickload.model.annots.QuickloadFile;
import com.lorainelab.quickload.model.annots.AnnotsRootElement;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.Optional;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    public List<QuickloadFile> getQuickloadFileList(Reader reader) throws IOException {
        List<QuickloadFile> annotsFileList = Lists.newArrayList();
        fromXml(reader).ifPresent(filesTag -> filesTag.getFiles().stream().forEach(annotsFileList::add));
        return annotsFileList;
    }

    private Optional<AnnotsRootElement> fromXml(Reader reader) throws IOException {
        try {
            return Optional.ofNullable((AnnotsRootElement) primaryUnmarshaller.unmarshal(reader));
        } catch (JAXBException ex) {
            logger.error("Error Loading xml preferences file", ex);
        }

        return Optional.empty();
    }

}
