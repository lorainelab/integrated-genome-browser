/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot.interproscan;

import java.math.BigInteger;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.bioviz.protannot.model.Dnaseq;
import org.bioviz.protannot.model.Dnaseq.Aaseq;
import org.bioviz.protannot.model.Dnaseq.Aaseq.Simsearch;
import org.bioviz.protannot.model.Dnaseq.Aaseq.Simsearch.Simhit;
import org.bioviz.protannot.model.Dnaseq.Descriptor;
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
        Aaseq aaseq = new Aaseq();
        Simsearch simsearch = new Simsearch();
        simsearch.setMethod("InterPro");
        aaseq.getSimsearch().add(simsearch);
        dnaseq.getMRNAAndAaseq().add(aaseq);

        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            Node matchesNode = (Node) xPath.evaluate("/protein-matches/protein/matches",
                    document.getDocumentElement(), XPathConstants.NODE);
            for (Node matchNode = matchesNode.getFirstChild();
                    matchNode != null; matchNode = matchNode.getNextSibling()) {
                if (matchNode.getNodeType() == Node.ELEMENT_NODE) {
                    Simhit simhit = new Simhit();
                    simsearch.getSimhit().add(simhit);
                    extractAttributesAsDescriptors(matchNode, simhit);
                    parseLocationsOnMatch(matchNode, simhit);

                }
            }
        } catch (XPathExpressionException ex) {
            LOG.error(ex.getMessage(), ex);
        }

        return dnaseq;
    }

    private void extractAttributesAsDescriptors(Node matchNode, Simhit simhit)  {
        try {
            //        if(attributes == null) return;
//
//        for(int i = 0; i<attributes.getLength(); i++) {
//            Attr item = (Attr) attributes.item(i);
//            LOG.info(item.getName() + "->" + item.getValue());
//        }
            Node signature = (Node) xPath.evaluate("signature", matchNode, XPathConstants.NODE);
            NamedNodeMap attributes = signature.getAttributes();
            if (attributes == null) {
                return;
            } else {
                for (int i = 0; i < attributes.getLength(); i++) {
                    Attr item = (Attr) attributes.item(i);
                    LOG.info(item.getName() + "->" + item.getValue());
                }
            }
        } catch (XPathExpressionException ex) {
            LOG.error(ex.getMessage(), ex);
        }

    }

//    private Attr getAttributes(Node node, String xpath) {
//        try {
//            Node xpathNode = (Node) xPath.evaluate(xpath, node, XPathConstants.NODE);
//        } catch (XPathExpressionException ex) {
//            Logger.getLogger(InterProscanTranslator.class.getName()).log(Level.SEVERE, null, ex);
//        }
//
//    }

    private void parseLocationsOnMatch(Node matchNode, Simhit simhit) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            Node locationsNode = (Node) xPath.evaluate("locations", matchNode, XPathConstants.NODE);
            for (Node locationNode = locationsNode.getFirstChild();
                    locationNode != null; locationNode = locationNode.getNextSibling()) {
                if (locationNode.getNodeType() == Node.ELEMENT_NODE) {
                    NamedNodeMap attributes = locationNode.getAttributes();
                    if (attributes != null) {
                        Simhit.Simspan simspan = new Simhit.Simspan();
                        simhit.getSimspan().add(simspan);
                        for (int i = 0; i < attributes.getLength(); i++) {
                            Attr item = (Attr) attributes.item(i);
                            //LOG.info("location: "+item.getName() + "," + item.getValue());
                            if (item.getName().equals("start")) {
                                simspan.setQueryStart(new BigInteger(item.getValue()));
                            } else if (item.getName().equals("end")) {
                                simspan.setQueryEnd(new BigInteger(item.getValue()));
                            } else {
                                Descriptor descriptor = new Descriptor();
                                descriptor.setType(item.getName());
                                descriptor.setValue(item.getValue());
                                simspan.getDescriptor().add(descriptor);
                            }
                        }
                    }
                }
            }
        } catch (XPathExpressionException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }
}
