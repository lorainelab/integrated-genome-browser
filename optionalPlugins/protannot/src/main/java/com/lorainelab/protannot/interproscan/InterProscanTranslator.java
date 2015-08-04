/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.protannot.interproscan;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import java.math.BigInteger;
import java.util.Optional;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import com.lorainelab.protannot.interproscan.api.InterProscanService;
import com.lorainelab.protannot.model.Dnaseq;
import com.lorainelab.protannot.model.Dnaseq.Aaseq;
import com.lorainelab.protannot.model.Dnaseq.Aaseq.Simsearch;
import com.lorainelab.protannot.model.Dnaseq.Aaseq.Simsearch.Simhit;
import com.lorainelab.protannot.model.Dnaseq.Descriptor;
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

    private InterProscanService interProscanService;

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
        addAttributesToSimhit(attributes, simhit, matchNode.getNodeName());
    }

    private void parseSignatureAttributesOnMatch(Node matchNode, Simhit simhit) {
        XPath xPath = XPathFactory.newInstance().newXPath();
        try {
            Node signature = (Node) xPath.evaluate("signature", matchNode, XPathConstants.NODE);
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
            if (libraryRelease == null) {
                return;
            }
            NamedNodeMap attributes = libraryRelease.getAttributes();
            addAttributesToSimhit(attributes, simhit, null);
        } catch (XPathExpressionException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    private void parseEntryOnSignature(Node signatureNode, Simhit simhit) {
        try {
            XPath xPath = XPathFactory.newInstance().newXPath();
            Node entry = (Node) xPath.evaluate("entry", signatureNode, XPathConstants.NODE);
            if (entry == null) {
                return;
            }
            NamedNodeMap attributes = entry.getAttributes();
            addAttributesToSimhit(attributes, simhit, signatureNode.getNodeName() + "-");
        } catch (XPathExpressionException ex) {
            LOG.error(ex.getMessage(), ex);
        }

    }

    private void addAttributesToSimhit(NamedNodeMap attributes, Simhit simhit, String prefix) {
        if(prefix == null) {
            prefix = "";
        }
        if (attributes == null) {
            return;
        }
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr item = (Attr) attributes.item(i);
            switch (item.getName()) {
                case "library":
                    Dnaseq.Descriptor matchDatabase = new Dnaseq.Descriptor();
                    matchDatabase.setType(item.getName());
                    matchDatabase.setValue(item.getValue());
                    simhit.getDescriptor().add(matchDatabase);

                    Optional<String> applicationLabel = interProscanService.getApplicationLabel(item.getValue());
                    if (applicationLabel.isPresent()) {
                        Dnaseq.Descriptor evidence = new Dnaseq.Descriptor();
                        evidence.setType("application");
                        evidence.setValue(applicationLabel.get());
                        simhit.getDescriptor().add(evidence);
                    }
                    continue;
                case "ac":
                    Dnaseq.Descriptor ac = new Dnaseq.Descriptor();
                    ac.setType("InterPro accession");
                    ac.setValue(item.getValue());
                    simhit.getDescriptor().add(ac);

                    Dnaseq.Descriptor url = new Dnaseq.Descriptor();
                    url.setType("URL");
                    url.setValue("http://www.ebi.ac.uk/interpro/IEntry?ac=" + item.getValue());
                    simhit.getDescriptor().add(url);
                    continue;
                case "desc":
                    Dnaseq.Descriptor desc = new Dnaseq.Descriptor();
                    desc.setType("InterPro description");
                    desc.setValue(item.getValue());
                    simhit.getDescriptor().add(desc);
                    continue;
                case "name":
                    Dnaseq.Descriptor name = new Dnaseq.Descriptor();
                    name.setType("InterPro name");
                    name.setValue(item.getValue());
                    simhit.getDescriptor().add(name);
                    continue;
                default:
                    Dnaseq.Descriptor descriptor = new Dnaseq.Descriptor();
                    descriptor.setType(prefix + item.getName());
                    descriptor.setValue(item.getValue());
                    simhit.getDescriptor().add(descriptor);
            }

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
                            if (item.getName().equals("start")) {
                                simspan.setQueryStart(new BigInteger(item.getValue()));
                            } else if (item.getName().equals("end")) {
                                simspan.setQueryEnd(new BigInteger(item.getValue()));
                            } else {
                                Descriptor descriptor = new Descriptor();
                                descriptor.setType(matchNode.getNodeName() + "-location-" + item.getName());
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

    public InterProscanService getInterProscanService() {
        return interProscanService;
    }

    @Reference
    public void setInterProscanService(InterProscanService interProscanService) {
        this.interProscanService = interProscanService;
    }

}
