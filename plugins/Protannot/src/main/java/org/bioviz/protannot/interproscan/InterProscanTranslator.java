/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot.interproscan;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.bioviz.protannot.model.Dnaseq;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

/**
 *
 * @author jeckstei
 */
public class InterProscanTranslator {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(InterProscanTranslator.class);

    public Dnaseq translateFromResultDocumentToModel(Document document) {
        Dnaseq dnaseq = new Dnaseq();
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            Node matchNode = (Node) xPath.evaluate("/protein-matches/protein/matches",
                    document.getDocumentElement(), XPathConstants.NODE);
            for (Node childNode = matchNode.getFirstChild();
                    childNode != null; childNode = childNode.getNextSibling()) {
                if(childNode.getNodeType() == Node.ELEMENT_NODE) {
                    NamedNodeMap attributes = childNode.getAttributes();
                    if(attributes != null) {
                        for(int i = 0;i < attributes.getLength();i++) {
                            Attr item = (Attr)attributes.item(i);
                            LOG.info(item.getName());
                        }
                    }
                }
            }
        } catch (XPathExpressionException ex) {
            LOG.error(ex.getMessage(), ex);
        }

        return dnaseq;
    }
}
