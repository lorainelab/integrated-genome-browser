/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot.interproscan;

import java.util.logging.Level;
import java.util.logging.Logger;
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
    XPath xPath = XPathFactory.newInstance().newXPath();

    public Dnaseq translateFromResultDocumentToModel(Document document) {
        Dnaseq dnaseq = new Dnaseq();
        try {
            Node matchNode = (Node) xPath.evaluate("/protein-matches/protein/matches",
                    document.getDocumentElement(), XPathConstants.NODE);
            for (Node childNode = matchNode.getFirstChild();
                    childNode != null; childNode = childNode.getNextSibling()) {
                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                    //extractAttributesAsDescriptors(childNode, dnaseq.getMRNAAndAaseq().get);
                }
            }
        } catch (XPathExpressionException ex) {
            LOG.error(ex.getMessage(), ex);
        }

        return dnaseq;
    }

    private void extractAttributesAsDescriptors(Node childNode, Dnaseq.Aaseq.Simsearch.Simhit simhit) throws XPathExpressionException {
//        if(attributes == null) return;
//        
//        for(int i = 0; i<attributes.getLength(); i++) {
//            Attr item = (Attr) attributes.item(i);
//            LOG.info(item.getName() + "->" + item.getValue());
//        }
        Node signature = (Node) xPath.evaluate("signature", childNode, XPathConstants.NODE);
        NamedNodeMap attributes = signature.getAttributes();
        if (attributes == null) {
            return;
        } else {
            for(int i = 0; i<attributes.getLength();i++) {
                Attr item = (Attr) attributes.item(i);
                LOG.info(item.getName() + "->" + item.getValue());
            }
        }

    }
    
    private Attr getAttributes(Node node, String xpath) {
        try {
            Node xpathNode = (Node) xPath.evaluate(xpath, node, XPathConstants.NODE);
        } catch (XPathExpressionException ex) {
            Logger.getLogger(InterProscanTranslator.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
}
