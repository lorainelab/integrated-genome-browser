/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.lorainelab.protannot.interproscan;

import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Reference;
import com.lorainelab.protannot.interproscan.api.InterProscanService;
import com.lorainelab.protannot.model.Dnaseq;
import com.lorainelab.protannot.model.Dnaseq.Aaseq;
import com.lorainelab.protannot.model.Dnaseq.Aaseq.Simsearch;
import com.lorainelab.protannot.model.Dnaseq.Aaseq.Simsearch.Simhit;
import com.lorainelab.protannot.model.Dnaseq.Descriptor;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
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
            Optional<Descriptor> name = simhit.getDescriptor().stream().filter(d -> d.getType().equals("InterPro name")).findFirst();
            Optional<Descriptor> ac = simhit.getDescriptor().stream().filter(d -> d.getType().equals("InterPro accession")).findFirst();
            Optional<Descriptor> url = simhit.getDescriptor().stream().filter(d -> d.getType().equals("URL")).findFirst();
            Optional<Descriptor> desc = simhit.getDescriptor().stream().filter(d -> d.getType().equals("InterPro description")).findFirst();
            NamedNodeMap attributes = signature.getAttributes();
            if (!name.isPresent() && attributes != null
                    && attributes.getNamedItem("name") != null) {
                String signatureName = ((Attr) attributes.getNamedItem("name")).getValue();
                Dnaseq.Descriptor descriptor = new Dnaseq.Descriptor();
                descriptor.setType("InterPro name");
                descriptor.setValue(signatureName);
                simhit.getDescriptor().add(descriptor);
            }
            if (!ac.isPresent() && attributes != null
                    && attributes.getNamedItem("ac") != null) {
                String signatureAc = ((Attr) attributes.getNamedItem("ac")).getValue();
                Dnaseq.Descriptor descriptor = new Dnaseq.Descriptor();
                descriptor.setType("InterPro accession");
                descriptor.setValue(signatureAc);
                simhit.getDescriptor().add(descriptor);

                if (!url.isPresent()) {
                    try {
                        Dnaseq.Descriptor urlDescriptor = new Dnaseq.Descriptor();
                        urlDescriptor.setType("URL");
                        urlDescriptor.setValue(generateUrlFromAc(signatureAc).toString());
                        simhit.getDescriptor().add(urlDescriptor);
                    } catch (MalformedURLException ex) {
                        LOG.error(ex.getMessage(), ex);
                    }
                }
            }
            if (!desc.isPresent() && attributes != null
                    && attributes.getNamedItem("desc") != null) {
                String signatureDesc = ((Attr) attributes.getNamedItem("desc")).getValue();
                Dnaseq.Descriptor descriptor = new Dnaseq.Descriptor();
                descriptor.setType("InterPro description");
                descriptor.setValue(signatureDesc);
                simhit.getDescriptor().add(descriptor);
            }
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

    private URL generateUrlFromAc(String ac) throws MalformedURLException {
        if (ac.toLowerCase().startsWith("ipr")) {
            return new URL("http://www.ebi.ac.uk/interpro/entry/" + ac);
        } else {
            return new URL("http://www.google.com/search?q=" + ac);
        }
    }

    private void addAttributesToSimhit(NamedNodeMap attributes, Simhit simhit, String prefix) {
        if (prefix == null) {
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

                    try {
                        Dnaseq.Descriptor url = new Dnaseq.Descriptor();
                        url.setType("URL");
                        url.setValue(generateUrlFromAc(item.getValue()).toString());
                        simhit.getDescriptor().add(url);
                    } catch (MalformedURLException ex) {
                        LOG.error(ex.getMessage(), ex);
                    }
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
            orderSimspans(simhit.getSimspan());
        } catch (XPathExpressionException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    private void orderSimspans(List<Simhit.Simspan> simspans) {
        Collections.sort(simspans, new SimhitComparer());
    }

    public class SimhitComparer implements Comparator<Simhit.Simspan> {

        @Override
        public int compare(Simhit.Simspan x, Simhit.Simspan y) {
            return x.getQueryStart().compareTo(y.getQueryStart());
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
