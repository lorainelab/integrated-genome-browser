/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.bioviz.protannot.interproscan;

import aQute.bnd.annotation.component.Component;
import java.math.BigInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
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
@Component(provide = InterProscanTranslator.class)
public class InterProscanTranslator {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(InterProscanTranslator.class);
    

    public Aaseq translateFromResultDocumentToModel(String id, Document document) {
        Aaseq aaseq = new Aaseq();
        aaseq.setId(id);
        Simsearch simsearch = new Simsearch();
        simsearch.setMethod("InterPro");
        aaseq.getSimsearch().add(simsearch);

        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            Node matchesNode = (Node) xPath.evaluate("/protein-matches/protein/matches",
                    document.getDocumentElement(), XPathConstants.NODE);
            for (Node matchNode = matchesNode.getFirstChild();
                    matchNode != null; matchNode = matchNode.getNextSibling()) {
                if (matchNode.getNodeType() == Node.ELEMENT_NODE) {
                    Simhit simhit = new Simhit();
                    simsearch.getSimhit().add(simhit);
                    parseAttributesOnMatch(matchNode, simhit);
                    parseSignatureAttributesOnMatch(matchNode, simhit);
                    parseLocationsOnMatch(matchNode, simhit);
                }
            }
        } catch (XPathExpressionException ex) {
            LOG.error(ex.getMessage(), ex);
        }

        return aaseq;
    }

    private void parseAttributesOnMatch(Node matchNode, Simhit simhit) {
        NamedNodeMap attributes = matchNode.getAttributes();
        addAttributesToSimhit(attributes, simhit);
    }

    private void parseSignatureAttributesOnMatch(Node matchNode, Simhit simhit) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            Node signature = (Node) xPath.evaluate("signature", matchNode, XPathConstants.NODE);
            NamedNodeMap attributes = signature.getAttributes();
            addAttributesToSimhit(attributes, simhit);
            parseEntryOnSignature(signature, simhit);
            parseLibraryReleaseOnSignature(signature, simhit);
        } catch (XPathExpressionException ex) {
            LOG.error(ex.getMessage(), ex);
        }

    }

    private void parseLibraryReleaseOnSignature(Node signatureNode, Simhit simhit) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            Node libraryRelease = (Node) xPath.evaluate("signature-library-release", signatureNode, XPathConstants.NODE);
            if(libraryRelease == null) return;
            NamedNodeMap attributes = libraryRelease.getAttributes();
            addAttributesToSimhit(attributes, simhit);
        } catch (XPathExpressionException ex) {
            Logger.getLogger(InterProscanTranslator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void parseEntryOnSignature(Node signatureNode, Simhit simhit) {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            Node entry = (Node) xPath.evaluate("entry", signatureNode, XPathConstants.NODE);
            if(entry == null) return;
            NamedNodeMap attributes = entry.getAttributes();
            addAttributesToSimhit(attributes, simhit);
        } catch (XPathExpressionException ex) {
            Logger.getLogger(InterProscanTranslator.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    private void addAttributesToSimhit(NamedNodeMap attributes, Simhit simhit) {
        if (attributes == null) {
            return;
        }
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr item = (Attr) attributes.item(i);
            Dnaseq.Descriptor desc = new Dnaseq.Descriptor();
            desc.setType(item.getName());
            desc.setValue(item.getValue());
            simhit.getDescriptor().add(desc);
        }
    }

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
                                descriptor.setType(matchNode.getNodeName() + "-" + item.getName());
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
